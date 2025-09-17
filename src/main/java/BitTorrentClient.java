import bencode.torrent.TorrentMeta;
import bencode.torrent.TorrentParser;
import config.CmdParser;
import config.PeerConfigParser;
import dispatcher.DownloadScheduler;
import dispatcher.PeerManager;
import network.NetworkReactor;
import storage.FileManager;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.*;

public class BitTorrentClient {
  public static void main(String[] args) {
    CmdParser paths = CmdParser.parse(args);

    try {
      TorrentMeta meta = TorrentParser.parseTorrent(paths.getTorrentFilePath().toString());

      FileManager fm = new FileManager(meta);
      NetworkReactor reactor = new NetworkReactor();

      int cores = Runtime.getRuntime().availableProcessors();
      BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(100);

      ExecutorService executor = new ThreadPoolExecutor(
              cores,
              cores,
              60, TimeUnit.SECONDS,
              queue,
              new ThreadPoolExecutor.CallerRunsPolicy()
      );

      PeerManager peerManager = new PeerManager(reactor, meta, fm, executor);
      DownloadScheduler scheduler = new DownloadScheduler(meta, reactor, peerManager);

      peerManager.setScheduler(scheduler);

      reactor.setListener(peerManager);
      reactor.registerServer(paths.getListenPort());

      List<InetSocketAddress> peers = PeerConfigParser.getPeers(paths.getPeersConfigPath());
      for (InetSocketAddress peer : peers) {
        reactor.registerClient(peer);
      }

      Thread reactorThread = new Thread(reactor, "reactor-thread");
      reactorThread.start();

    } catch (Exception e) {
      System.err.println("Fatal error: " + e.getMessage());
      System.exit(1);
    }
  }
}
