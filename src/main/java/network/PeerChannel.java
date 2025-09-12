package network;

import dispatcher.ProtocolListener;
import lombok.Getter;
import lombok.ToString;
import protocol.MessageCodec;
import protocol.messages.HandshakeMessage;
import protocol.messages.Message;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Queue;

@Getter
@ToString
public class PeerChannel {
  public enum State {HANDSHAKE, MESSAGING}

  private final SocketChannel channel;
  private final ProtocolListener listener;
  private final boolean initiator;
  private State state = State.HANDSHAKE;

  private final Queue<ByteBuffer> outbound = new ArrayDeque<>();

  private final ByteBuffer hsLengthBuffer = ByteBuffer.allocate(1);
  private ByteBuffer hsBuffer;

  private final ByteBuffer lengthBuffer = ByteBuffer.allocate(4);
  private ByteBuffer dataBuffer;

  public PeerChannel(SocketChannel channel, ProtocolListener listener, boolean initiator)
          throws IOException {
    this.channel = channel;
    this.listener = listener;
    this.initiator = initiator;
    channel.configureBlocking(false);
  }

  public void send(Message message) {
    ByteBuffer buffer = MessageCodec.encode(message);
    synchronized (outbound) {
      outbound.add(buffer);
    }
  }

  public boolean hasPendingWrites() {
    synchronized (outbound) {
      return !outbound.isEmpty();
    }
  }

  public void handleWrite() throws IOException {
    synchronized (outbound) {
      while (!outbound.isEmpty()) {
        ByteBuffer buffer = outbound.peek();
        channel.write(buffer);
        if (buffer.hasRemaining()) {return;}
        outbound.poll();
      }
    }
  }

  public void handleRead() throws IOException {
    if (state == State.HANDSHAKE) {
      readHandshake();
    } else {
      readMessage();
    }
  }

  private void readHandshake() throws IOException {
    if (hsLengthBuffer.hasRemaining()) {
      int n = channel.read(hsLengthBuffer);
      if (n <= 0) {return;}
      if (!hsLengthBuffer.hasRemaining()) {
        int pstrlen = Byte.toUnsignedInt(hsLengthBuffer.get(0));
        int total = 1 + pstrlen + 8 + 20 + 20;
        hsBuffer = ByteBuffer.allocate(total);
      } else {return;}
    }

    if (hsBuffer.hasRemaining()) {
      int n = channel.read(hsBuffer);
      if (n <= 0) {return;}
    }

    if (!hsBuffer.hasRemaining()) {
      hsBuffer.flip();
      HandshakeMessage hsm = MessageCodec.decodeHandshake(hsLengthBuffer, hsBuffer);
      listener.onMessage(this, hsm);
      state = State.MESSAGING;
      lengthBuffer.clear();
    }
  }

  private void readMessage() throws IOException {
    if (lengthBuffer.hasRemaining()) {
      int n = channel.read(lengthBuffer);
      if (n <= 0 || lengthBuffer.hasRemaining()) {return;}
      lengthBuffer.flip();

      int msgLength = lengthBuffer.getInt();
      if (msgLength == 0) {
        listener.onMessage(this, MessageCodec.decodeMessage(ByteBuffer.allocate(0)));
        lengthBuffer.clear();
        return;
      }
      dataBuffer = ByteBuffer.allocate(msgLength);
    }

    if (dataBuffer.hasRemaining()) {
      int r = channel.read(dataBuffer);
      if (r <= 0) return;
    }

    if (!dataBuffer.hasRemaining()) {
      dataBuffer.flip();
      Message msg = MessageCodec.decodeMessage(dataBuffer);
      listener.onMessage(this, msg);
      lengthBuffer.clear();
      dataBuffer = null;
    }
  }

}
