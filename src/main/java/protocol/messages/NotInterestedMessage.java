package protocol.messages;

import protocol.MessageType;

import java.nio.ByteBuffer;

public class NotInterestedMessage implements Message {
  @Override
  public MessageType getType() {
    return MessageType.NOTINTERESTED;
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
