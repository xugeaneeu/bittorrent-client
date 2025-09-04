package protocol.messages;

import lombok.Getter;
import lombok.ToString;
import protocol.MessageType;

import java.nio.ByteBuffer;

@Getter
@ToString
public class CancelMessage implements Message {
  private final int index;
  private final int begin;
  private final int length;

  public CancelMessage(final int index, final int begin, final int length) {
    this.index = index;
    this.begin = begin;
    this.length = length;
  }

  @Override
  public MessageType getType() {
    return MessageType.CANCEL;
  }

  @Override
  public ByteBuffer toBytes() {
    int payloadLength = 1 + 12;
    ByteBuffer buffer = ByteBuffer.allocate(4 + payloadLength);
    buffer.putInt(payloadLength);
    buffer.put((byte)getType().getId());
    buffer.putInt(index);
    buffer.putInt(begin);
    buffer.putInt(length);
    buffer.flip();
    return buffer;
  }
}
