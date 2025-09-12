package network;

import dispatcher.ProtocolListener;
import lombok.Setter;
import protocol.messages.Message;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class NetworkReactor implements Runnable {
  private final Selector demultiplexer;
  private ServerSocketChannel serverChannel;
  @Setter
  private ProtocolListener listener;
  private volatile boolean running = true;

  public NetworkReactor() throws IOException {
    this.demultiplexer = Selector.open();
  }

  @Override
  public void run() {
    try {
      while (running) {
        demultiplexer.select();
        Set<SelectionKey> keys = demultiplexer.selectedKeys();
        Iterator<SelectionKey> iterator = keys.iterator();
        while (iterator.hasNext()) {
          SelectionKey key = iterator.next();
          iterator.remove();
          if (!key.isValid()) {continue;}
          if (key.isAcceptable()) {
            doAccept(key);
          }
          if (key.isConnectable()) {
            doConnect(key);
          }
          if (key.isReadable()) {
            PeerChannel peer = (PeerChannel) key.attachment();
            peer.handleRead();
//            if (peer.hasPendingWrites()) {
//              key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
//            }
          }
          if (key.isWritable()) {
            PeerChannel peer = (PeerChannel) key.attachment();
            peer.handleWrite();
            if (!peer.hasPendingWrites()) {
              key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
            }
          }
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      cleanup();
    }
  }

  public void send(PeerChannel peer, Message msg) {
    peer.send(msg);
    SelectionKey key = peer.getChannel().keyFor(demultiplexer);
    if (key != null && key.isValid()) {
      key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
      demultiplexer.wakeup();
    }
  }

  public void registerServer(int listenPort) throws IOException {
    serverChannel = ServerSocketChannel.open();
    serverChannel.bind(new InetSocketAddress(listenPort));
    serverChannel.configureBlocking(false);
    serverChannel.register(demultiplexer, SelectionKey.OP_ACCEPT);
  }

  public void registerClient(InetSocketAddress addr) throws IOException {
    SocketChannel sc = SocketChannel.open();
    PeerChannel peer = new PeerChannel(sc, listener, true);
    sc.connect(addr);
    sc.register(demultiplexer, SelectionKey.OP_CONNECT, peer);
  }

  public void shutdown() {
    running = false;
    demultiplexer.wakeup();
  }

  private void doAccept(SelectionKey key) throws IOException {
    ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
    SocketChannel connection = ssc.accept();
    if (connection == null) {return;}
    PeerChannel peer = new PeerChannel(connection, listener, false);
    connection.register(demultiplexer, SelectionKey.OP_READ, peer);
  }

  private void doConnect(SelectionKey key) throws IOException {
    SocketChannel connection = (SocketChannel) key.channel();
    if (!connection.finishConnect()) {
      cancelKey(key);
      return;
    }
    PeerChannel peer = (PeerChannel) key.attachment();
    key.interestOps(SelectionKey.OP_READ);
    listener.onChannelConnected(peer);
  }

  private void cancelKey(SelectionKey key) {
    key.cancel();
    try {
      key.channel().close();
    } catch (IOException _) {}
  }

  private void cleanup() {
    try { if (serverChannel != null) serverChannel.close(); } catch (IOException _) {}
    try { demultiplexer.close(); } catch (IOException _) {}
  }
}
