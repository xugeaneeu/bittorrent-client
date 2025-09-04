package protocol.messages;

import lombok.Getter;
import lombok.ToString;
import storage.BitmapUtils;

import java.nio.ByteBuffer;

@Getter
@ToString
public class BitfieldMessage implements Message {
  private final boolean[] bitmap;

  public BitfieldMessage(final boolean[] bitmap) {
    this.bitmap = bitmap;
  }

  @Override
  public MessageType getType() {
    return MessageType.BITFIELD;
  }

  @Override
  public ByteBuffer toBytes() {
    byte[] bits = BitmapUtils.serialize(bitmap);
    int payload = 1 + bits.length;
    ByteBuffer buffer = ByteBuffer.allocate(4 + payload);
    buffer.putInt(payload)
            .put((byte) getType().getId())
            .put(bits)
            .flip();
    return buffer;
  }
}
