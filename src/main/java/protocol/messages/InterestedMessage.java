package protocol.messages;

import java.nio.ByteBuffer;

public class InterestedMessage implements Message {
  @Override
  public MessageType getType() {
    return MessageType.INTERESTED;
  }

  @Override
  public ByteBuffer toBytes() {
    ByteBuffer b = ByteBuffer.allocate(4+1);
    b.putInt(1);
    b.put((byte)getType().getId());
    b.flip();
    return b;
  }
}
