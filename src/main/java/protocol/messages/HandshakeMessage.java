package protocol.messages;

import lombok.Getter;
import lombok.ToString;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@Getter
@ToString
public class HandshakeMessage implements Message {
  private static final String PROTOCOL_NAME = "BitTorrent protocol";
  private static final int RESERVED_TO_EXTENSIONS = 8;
  private final byte[] infoHash;
  private final byte[] peerId;

  public HandshakeMessage(final byte[] infoHash, final byte[] peerId) {
    if (infoHash.length != 20 || peerId.length != 20) {
      throw new IllegalArgumentException("infoHash or peerId is not valid: must be 20 bytes");
    }

    this.infoHash = infoHash;
    this.peerId = peerId;
  }

  @Override
  public MessageType getType() {
    return MessageType.HANDSHAKE;
  }

  @Override
  public ByteBuffer toBytes() {
    byte[] pstrBytes = PROTOCOL_NAME.getBytes(StandardCharsets.UTF_8);
    int totalLength = 1 + pstrBytes.length + RESERVED_TO_EXTENSIONS + infoHash.length + peerId.length;
    ByteBuffer buffer = ByteBuffer.allocate(totalLength);
    buffer.put((byte) pstrBytes.length)
            .put(pstrBytes)
            .put(new byte[RESERVED_TO_EXTENSIONS])
            .put(infoHash)
            .put(peerId)
            .flip();
    return buffer;
  }
}
