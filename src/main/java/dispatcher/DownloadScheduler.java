package dispatcher;

import bencode.torrent.TorrentMeta;
import network.NetworkReactor;
import network.PeerChannel;
import protocol.messages.HaveMessage;
import protocol.messages.RequestMessage;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class DownloadScheduler {
  private final int pieceCount;
  private final NetworkReactor reactor;
  private final PeerManager peerManager;

  private final Set<Integer> inFlight = ConcurrentHashMap.newKeySet();

  public DownloadScheduler(TorrentMeta meta,
                           NetworkReactor reactor,
                           PeerManager peerManager) {
    this.pieceCount = meta.getPieces().size();
    this.reactor = reactor;
    this.peerManager = peerManager;
  }

  public void peerReady(PeerChannel peer) {
    boolean[] localBitmap = peerManager.getLocalBitmap();
    boolean[] remoteBitmap = peerManager.getRemoteBitmaps().get(peer);

    for (int i = 0; i < pieceCount; i++) {
      if (!localBitmap[i] && remoteBitmap[i] && inFlight.add(i)) {
        int begin = 0;
        int length = peerManager.getFileManager().pieceSize(i);
        reactor.send(peer, new RequestMessage(i, begin, length));
        return;
      }
    }
  }

  public void pieceReceived(int idx, PeerChannel fromPeer) {
    inFlight.remove(idx);

    HaveMessage msg = new HaveMessage(idx);
    for (PeerChannel peer : peerManager.getAllPeerChannels()) {
      if (peer != fromPeer) {
        reactor.send(peer, msg);
      }
    }

    peerReady(fromPeer);

    boolean done = true;
    for (boolean b : peerManager.getLocalBitmap()) {
      if (!b) { done = false; break; }
    }
    if (done) {
      System.out.println("Download complete.");
    }
  }
}
