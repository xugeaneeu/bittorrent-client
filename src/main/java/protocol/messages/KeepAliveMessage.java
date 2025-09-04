package protocol.messages;

import protocol.MessageType;

import java.nio.ByteBuffer;

public class KeepAliveMessage implements Message {
  @Override
  public MessageType getType() {
    return MessageType.KEEP_ALIVE;
  }

  @Override
  public ByteBuffer toBytes() {
    return ByteBuffer.allocate(4).putInt(0).flip();
  }
}
