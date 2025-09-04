package protocol.messages;

import lombok.Getter;
import lombok.ToString;

import java.nio.ByteBuffer;

@Getter
@ToString
public class RequestMessage implements Message {
  private final int index;
  private final int begin;
  private final int length;

  public RequestMessage(final int index, final int begin, final int length) {
    this.index = index;
    this.begin = begin;
    this.length = length;
  }

  @Override
  public MessageType getType() {
    return MessageType.REQUEST;
  }

  @Override
  public ByteBuffer toBytes() {
    int payload = 1 + 12;
    ByteBuffer buffer = ByteBuffer.allocate(4 + 1 + 4*3);
    buffer.putInt(payload)
            .put((byte) getType().getId())
            .putInt(index)
            .putInt(begin)
            .putInt(length)
            .flip();
    return buffer;
  }
}
