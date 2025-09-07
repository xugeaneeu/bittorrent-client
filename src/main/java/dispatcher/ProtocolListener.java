package dispatcher;

import network.PeerChannel;
import protocol.messages.Message;

public interface ProtocolListener {
  void onChannelConnected(PeerChannel peerChannel);
  void onMessage(PeerChannel peer, Message message);
}
