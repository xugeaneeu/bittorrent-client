package protocol;

import lombok.extern.slf4j.Slf4j;
import protocol.messages.HandshakeMessage;
import protocol.messages.*;
import protocol.messages.MessageType;

import java.nio.ByteBuffer;

@Slf4j
public class MessageCodec {

  public static ByteBuffer encode(Message msg) {
    ByteBuffer buf = msg.toBytes();
    log.debug("encode: type={} length={}", msg.getType(), buf.limit());
    return buf;
  }

  public static HandshakeMessage decodeHandshake(ByteBuffer pstrlenBuf, ByteBuffer dataBuf) {
    log.trace("decodeHandshake: reading pstrlen");
    pstrlenBuf.rewind();
    int pstrlen = Byte.toUnsignedInt(pstrlenBuf.get());
    dataBuf.rewind();
    log.trace("decodeHandshake: pstrlen={}", pstrlen);
    byte[] protocol = new byte[pstrlen];
    dataBuf.get(protocol);

    dataBuf.position(dataBuf.position() + 8);

    byte[] infoHash = new byte[20];
    dataBuf.get(infoHash);
    byte[] peerId = new byte[20];
    dataBuf.get(peerId);

    HandshakeMessage hs = new HandshakeMessage(infoHash, peerId);
    log.debug("decodeHandshake: {}", hs);
    return hs;
  }

  public static Message decodeMessage(ByteBuffer dataBuf) {
    dataBuf.rewind();
    byte id = dataBuf.get();
    MessageType type = MessageType.fromId(Byte.toUnsignedInt(id));
    log.trace("decodeMessage: type={}", type);

    switch (type) {
      case KEEP_ALIVE:    return new KeepAliveMessage();
      case CHOKE:         return new ChokeMessage();
      case UNCHOKE:       return new UnchokeMessage();
      case INTERESTED:    return new InterestedMessage();
      case NOTINTERESTED: return new NotInterestedMessage();
      case HAVE: {
        int idx = dataBuf.getInt();
        return new HaveMessage(idx);
      }
      case BITFIELD: {
        byte[] bits = new byte[dataBuf.remaining()];
        dataBuf.get(bits);
        return new BitfieldMessage(bits);
      }
      case REQUEST: {
        int idx   = dataBuf.getInt();
        int off   = dataBuf.getInt();
        int len   = dataBuf.getInt();
        return new RequestMessage(idx, off, len);
      }
      case PIECE: {
        int idx   = dataBuf.getInt();
        int off   = dataBuf.getInt();
        byte[] blk = new byte[dataBuf.remaining()];
        dataBuf.get(blk);
        return new PieceMessage(idx, off, blk);
      }
      case CANCEL: {
        int idx   = dataBuf.getInt();
        int off   = dataBuf.getInt();
        int len   = dataBuf.getInt();
        return new CancelMessage(idx, off, len);
      }
      default:
        throw new IllegalArgumentException("Unsupported message type: " + type);
    }
  }
}
