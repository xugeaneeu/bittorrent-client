import bencode.torrent.TorrentMeta;
import bencode.torrent.TorrentParser;
import config.CmdParser;
import config.PeerConfigParser;
import dispatcher.DownloadScheduler;
import dispatcher.PeerManager;
import lombok.extern.slf4j.Slf4j;
import network.NetworkReactor;
import storage.FileManager;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class BitTorrentClient {
  public static void main(String[] args) {
    CmdParser paths = CmdParser.parse(args);

    try {
      TorrentMeta meta = TorrentParser.parseTorrent(paths.getTorrentFilePath().toString());
      log.info("Parsed torrent metadata: {}", meta);

      FileManager fm = new FileManager(meta);
      NetworkReactor reactor = new NetworkReactor();

      ExecutorService executor = Executors.newCachedThreadPool();
      PeerManager peerManager = new PeerManager(reactor, meta, fm, executor);
      DownloadScheduler scheduler = new DownloadScheduler(meta, reactor, peerManager);

      peerManager.setScheduler(scheduler);

      reactor.setListener(peerManager);
      reactor.registerServer(paths.getListenPort());
      log.info("Listening on port {}", paths.getListenPort());

      List<InetSocketAddress> peers = PeerConfigParser.getPeers(paths.getPeersConfigPath());
      for (InetSocketAddress peer : peers) {
        reactor.registerClient(peer);
        log.info("Connecting to peer {}", peer);
      }

      Thread reactorThread = new Thread(reactor, "reactor-thread");
      reactorThread.start();
      log.info("Reactor thread started");

    } catch (Exception e) {
      log.error("Fatal error in main(): {}", e.toString(), e);
      System.exit(1);
    }
  }
}
