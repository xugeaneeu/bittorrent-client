package protocol.messages;

import lombok.Getter;
import lombok.ToString;

import java.nio.ByteBuffer;

@Getter
@ToString
public class HaveMessage implements Message {
  private final int pieceIndex;

  public HaveMessage(final int pieceIndex) {
    this.pieceIndex = pieceIndex;
  }

  @Override
  public MessageType getType() {
    return MessageType.HAVE;
  }

  @Override
  public ByteBuffer toBytes() {
    int payload = 1 + 4;
    ByteBuffer buffer = ByteBuffer.allocate(4 + 1 + 4);
    buffer.putInt(payload)
            .put((byte) getType().getId())
            .putInt(pieceIndex)
            .flip();
    return buffer;
  }
}
