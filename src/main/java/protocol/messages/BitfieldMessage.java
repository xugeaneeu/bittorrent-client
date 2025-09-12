package protocol.messages;

import lombok.Getter;
import lombok.ToString;

import java.nio.ByteBuffer;

@Getter
@ToString
public class BitfieldMessage implements Message {
  private final byte[] raw;

  public BitfieldMessage(final byte[] raw) {
    this.raw = raw;
  }

  @Override
  public MessageType getType() {
    return MessageType.BITFIELD;
  }

  @Override
  public ByteBuffer toBytes() {
    int payload = 1 + raw.length;
    ByteBuffer buffer = ByteBuffer.allocate(4 + payload);
    buffer.putInt(payload)
            .put((byte) getType().getId())
            .put(raw)
            .flip();
    return buffer;
  }
}
