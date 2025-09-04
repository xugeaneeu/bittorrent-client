package protocol.messages;

import lombok.Getter;
import lombok.ToString;

import java.nio.ByteBuffer;

@Getter
@ToString
public class PieceMessage implements Message {
  private final int index;
  private final int begin;
  private final byte[] block;

  public PieceMessage(final int index, final int begin, final byte[] block) {
    this.index = index;
    this.begin = begin;
    this.block = block;
  }

  @Override
  public MessageType getType() {
    return MessageType.PIECE;
  }

  @Override
  public ByteBuffer toBytes() {
    int payloadLength = 1 + 4 + 4 + block.length;
    ByteBuffer buffer = ByteBuffer.allocate(4 + payloadLength);
    buffer.putInt(payloadLength)
            .put((byte) getType().getId())
            .putInt(index)
            .putInt(begin)
            .put(block)
            .flip();
    return buffer;
  }
}
