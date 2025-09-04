package protocol.messages;

import java.nio.ByteBuffer;

public interface Message {
  MessageType getType();
  ByteBuffer toBytes();
}
