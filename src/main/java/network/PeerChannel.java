package network;

import dispatcher.ProtocolListener;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import protocol.MessageCodec;
import protocol.messages.HandshakeMessage;
import protocol.messages.Message;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Queue;

@Slf4j
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
    log.info("Created PeerChannel (initiator={}) for {}", initiator, channel.getRemoteAddress());
  }

  public void send(Message message) {
    ByteBuffer buffer = MessageCodec.encode(message);
    synchronized (outbound) {
      outbound.add(buffer);
    }
    try {
      log.debug("Enqueued {} for {}", message.getType(), channel.getRemoteAddress());
    } catch (IOException _) { }
  }

  public boolean hasPendingWrites() {
    synchronized (outbound) {
      return !outbound.isEmpty();
    }
  }

  public void handleWrite() throws IOException {
    log.trace("handleWrite for {}", channel.getRemoteAddress());
    synchronized (outbound) {
      while (!outbound.isEmpty()) {
        ByteBuffer buffer = outbound.peek();
        channel.write(buffer);
        if (buffer.hasRemaining()) {
          log.trace("Partial write, remaining={} bytes", buffer.remaining());
          return;
        }
        outbound.poll();
        log.debug("Sent buffer to {}", channel.getRemoteAddress());
      }
    }
  }

  public void handleRead() throws IOException {
    log.trace("handleRead(state={}) for {}", state, channel.getRemoteAddress());
    if (state == State.HANDSHAKE) {
      readHandshake();
    } else {
      readMessage();
    }
  }

  private void readHandshake() throws IOException {
    log.trace("readHandshake phase1 (len) for {}", channel.getRemoteAddress());
    if (hsLengthBuffer.hasRemaining()) {
      int n = channel.read(hsLengthBuffer);
      if (n <= 0) {return;}
      if (!hsLengthBuffer.hasRemaining()) {
        int pstrlen = Byte.toUnsignedInt(hsLengthBuffer.get(0));
        int total = pstrlen + 8 + 20 + 20;
        hsBuffer = ByteBuffer.allocate(total);
        log.debug("Handshake length {} (pstrlen={})", total, pstrlen);
      } else {return;}
    }

    log.trace("readHandshake phase2 (body) for {}", channel.getRemoteAddress());
    if (hsBuffer.hasRemaining()) {
      int n = channel.read(hsBuffer);
      if (n <= 0) {return;}
    }

    if (!hsBuffer.hasRemaining()) {
      hsBuffer.flip();
      HandshakeMessage hsm = MessageCodec.decodeHandshake(hsLengthBuffer, hsBuffer);
      log.info("Received HANDSHAKE from {}", channel.getRemoteAddress());
      listener.onMessage(this, hsm);
      state = State.MESSAGING;
      lengthBuffer.clear();
    }
  }

  private void readMessage() throws IOException {
    log.trace("readMessage: reading length for {}", channel.getRemoteAddress());
    if (lengthBuffer.hasRemaining()) {
      int n = channel.read(lengthBuffer);
      if (n <= 0 || lengthBuffer.hasRemaining()) {return;}
      lengthBuffer.flip();

      int msgLength = lengthBuffer.getInt();
      log.debug("Message length={} for {}", msgLength, channel.getRemoteAddress());
      if (msgLength == 0) {
        listener.onMessage(this, MessageCodec.decodeMessage(ByteBuffer.allocate(0)));
        lengthBuffer.clear();
        return;
      }
      dataBuffer = ByteBuffer.allocate(msgLength);
    }

    log.trace("readMessage: reading payload for {}", channel.getRemoteAddress());
    if (dataBuffer.hasRemaining()) {
      int r = channel.read(dataBuffer);
      if (r <= 0) return;
    }

    if (!dataBuffer.hasRemaining()) {
      dataBuffer.flip();
      Message msg = MessageCodec.decodeMessage(dataBuffer);
      log.info("Received {} from {}", msg.getType(), channel.getRemoteAddress());
      listener.onMessage(this, msg);
      lengthBuffer.clear();
      dataBuffer = null;
    }
  }

}
