package protocol.messages;

import protocol.MessageType;

import java.nio.ByteBuffer;

public interface Message {
  MessageType getType();
  ByteBuffer toBytes();
}
