package dispatcher;

import network.PeerChannel;
import protocol.messages.HandshakeMessage;
import protocol.messages.Message;

public interface ProtocolListener {
  void onHandshake(PeerChannel peer, HandshakeMessage message);
  void onMessage(PeerChannel peer, Message message);
  void onError(PeerChannel peerChannel, Exception e);
  void onClose(PeerChannel peer);
}
