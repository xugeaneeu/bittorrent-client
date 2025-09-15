package network;

import dispatcher.ProtocolListener;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import protocol.messages.Message;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

@Slf4j
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
    log.info("Reactor thread started");
    try {
      while (running) {
        log.debug("Waiting for I/O events...");
        demultiplexer.select();
        Set<SelectionKey> keys = demultiplexer.selectedKeys();
        Iterator<SelectionKey> iterator = keys.iterator();
        while (iterator.hasNext()) {
          SelectionKey key = iterator.next();
          iterator.remove();
          if (!key.isValid()) {
            log.debug("Skipping invalid key");
            continue;
          }

          try {
            if (key.isAcceptable()) {
              log.debug("Key is acceptable: {}", key);
              doAccept(key);
            }
            if (key.isConnectable()) {
              log.debug("Key is connectable: {}", key);
              doConnect(key);
            }
            if (key.isReadable()) {
              log.debug("Key is readable: {}", key);
              PeerChannel peer = (PeerChannel) key.attachment();
              peer.handleRead();
            }
            if (key.isWritable()) {
              log.debug("Key is writable: {}", key);
              PeerChannel peer = (PeerChannel) key.attachment();
              peer.handleWrite();
              if (!peer.hasPendingWrites()) {
                key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
                log.debug("Cleared OP_WRITE for {}", peer);
              }
            }
          } catch (IOException e) {
            log.warn("I/O error on key {}, cancelling: {}", key, e.toString());
            cancelKey(key);
          }
        }
      }
    } catch (IOException e) {
      log.error("Selector failed: {}", e.toString(), e);
      throw new RuntimeException(e);
    } finally {
      log.info("Reactor shutting down, cleaning up");
      cleanup();
    }
  }

  public void send(PeerChannel peer, Message msg) {
    log.debug("Queueing message {} to {}", msg.getType(), peer);
    peer.send(msg);
    SelectionKey key = peer.getChannel().keyFor(demultiplexer);
    if (key != null && key.isValid()) {
      key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
      demultiplexer.wakeup();
      log.debug("Set OP_WRITE and wakeup selector for {}", peer);
    }
  }

  public void registerServer(int listenPort) throws IOException {
    serverChannel = ServerSocketChannel.open();
    serverChannel.bind(new InetSocketAddress(listenPort));
    serverChannel.configureBlocking(false);
    serverChannel.register(demultiplexer, SelectionKey.OP_ACCEPT);
    log.info("Server registered on port {}", listenPort);
  }

  public void registerClient(InetSocketAddress addr) throws IOException {
    SocketChannel sc = SocketChannel.open();
    PeerChannel peer = new PeerChannel(sc, listener, true);
    log.info("Initiating connection to {}", addr);
    sc.connect(addr);
    sc.register(demultiplexer, SelectionKey.OP_CONNECT, peer);
  }

  public void shutdown() {
    log.info("Shutdown requested");
    running = false;
    demultiplexer.wakeup();
  }

  private void doAccept(SelectionKey key) throws IOException {
    ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
    SocketChannel connection = ssc.accept();
    if (connection == null) {
      log.debug("Accept returned null (non-blocking)");
      return;
    }
    log.info("Accepted connection from {}", connection.getRemoteAddress());
    PeerChannel peer = new PeerChannel(connection, listener, false);
    connection.register(demultiplexer, SelectionKey.OP_READ, peer);
  }

  private void doConnect(SelectionKey key) throws IOException {
    SocketChannel connection = (SocketChannel) key.channel();
    if (!connection.finishConnect()) {
      log.warn("finishConnect failed, cancelling {}", connection);
      cancelKey(key);
      return;
    }
    log.info("Connection established to {}", connection.getRemoteAddress());
    PeerChannel peer = (PeerChannel) key.attachment();
    key.interestOps(SelectionKey.OP_READ);
    listener.onChannelConnected(peer);
  }

  private void cancelKey(SelectionKey key) {
    log.debug("Cancelling key {}", key);
    key.cancel();
    try {
      key.channel().close();
      log.debug("Closed channel for key {}", key);
    } catch (IOException _) {}
  }

  private void cleanup() {
    log.info("Cleaning up NetworkReactor resources");
    try {
      if (serverChannel != null) {
        serverChannel.close();
        log.debug("Closed serverChannel");
      }
    } catch (IOException _) {}
    try {
      demultiplexer.close();
      log.debug("Closed serverChannel");
    } catch (IOException _) {}
  }
}
