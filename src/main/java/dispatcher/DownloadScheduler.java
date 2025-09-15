package dispatcher;

import bencode.torrent.TorrentMeta;
import lombok.extern.slf4j.Slf4j;
import network.NetworkReactor;
import network.PeerChannel;
import protocol.messages.HaveMessage;
import protocol.messages.RequestMessage;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
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
    log.info("DownloadScheduler initialized: pieceCount={}", pieceCount);
  }

  public void peerReady(PeerChannel peer) {
    try {
      log.info("peerReady() called for {}", peer.getChannel().getRemoteAddress());
      boolean[] localBitmap = peerManager.getLocalBitmap();
      boolean[] remoteBitmap = peerManager.getRemoteBitmaps().get(peer);

      for (int i = 0; i < pieceCount; i++) {
        if (!localBitmap[i] && remoteBitmap[i] && inFlight.add(i)) {
          int begin = 0;
          int length = peerManager.getFileManager().pieceSize(i);
          log.info("Requesting piece {} (size={}) from {}", i, length, peer.getChannel().getRemoteAddress());
          reactor.send(peer, new RequestMessage(i, begin, length));
          return;
        }
      }
      log.info("No piece to request from {}", peer.getChannel().getRemoteAddress());
    } catch (IOException _) { }
  }

  public void pieceReceived(int idx, PeerChannel fromPeer) {
    try {
      log.info("pieceReceived() index={} from {}", idx, fromPeer.getChannel().getRemoteAddress());
      inFlight.remove(idx);

      HaveMessage msg = new HaveMessage(idx);
      for (PeerChannel peer : peerManager.getAllPeerChannels()) {
        if (peer != fromPeer) {
          log.debug("Broadcasting HAVE {} to {}", idx, peer.getChannel().getRemoteAddress());
          reactor.send(peer, msg);
        }
      }
    } catch (IOException _) { }

    peerReady(fromPeer);

    boolean done = true;
    for (boolean b : peerManager.getLocalBitmap()) {
      if (!b) { done = false; break; }
    }
    if (done) {
      System.out.println("Download complete.");
      log.info("Download complete.");
    }
  }
}
