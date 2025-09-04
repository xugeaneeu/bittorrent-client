package protocol.messages;

import lombok.Getter;
import lombok.ToString;
import protocol.MessageType;
import storage.BitmapUtils;

import java.nio.ByteBuffer;

@Getter
@ToString
public class BitfieldMessage implements Message {
  private final byte[] bitmap;

  public BitfieldMessage(final byte[] bitmap) {
    this.bitmap = bitmap;
  }

  @Override
  public MessageType getType() {
    return MessageType.BITFIELD;
  }

  @Override
  public ByteBuffer toBytes() {
    int payload = 1 + bitmap.length;
    ByteBuffer buffer = ByteBuffer.allocate(payload + 4);
    buffer.putInt(payload)
            .put((byte) getType().getId())
            .put(bitmap)
            .flip();
    return buffer;
  }

  public boolean[] getBitmap(int expectedPieces) {
    return BitmapUtils.deserialize(bitmap, expectedPieces);
  }
}
