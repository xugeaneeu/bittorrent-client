package protocol;

import lombok.Getter;

public enum MessageType {
  KEEP_ALIVE(-1),
  CHOKE(0),
  UNCHOKE(1),
  INTERESTED(2),
  NOTINTERESTED(3),
  HAVE(4),
  BITFIELD(5),
  REQUEST(6),
  PIECE(7),
  CANCEL(8),
  HANDSHAKE(9);

  @Getter
  private final int id;
  MessageType(int id) {this.id = id;}

  public static MessageType fromId(int id) {
    for (MessageType mt : values()) {
      if (mt.id == id) {
        return mt;
      }
    }
    throw new IllegalArgumentException("Invalid message id: " + id);
  }
}
