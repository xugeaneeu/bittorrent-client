import bencode.torrent.TorrentMeta;
import bencode.torrent.TorrentParser;
import config.CmdParser;
import config.PeerConfigParser;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

public class BitTorrentClient {
  public static void main(String[] args) {
    CmdParser paths = CmdParser.parse(args);

    TorrentMeta meta;
    try {
      meta = TorrentParser.parseTorrent(paths.getTorrentFilePath().toString());
    } catch (IOException e) {
      System.err.println("Failed to parse torrent file: " + e.getMessage());
      System.exit(1);
      return;
    }

    List<InetSocketAddress> peers;
    try {
      peers = PeerConfigParser.getPeers(paths.getPeersConfigPath());
    } catch (IOException e) {
      System.err.println("Failed to parse peers: " + e.getMessage());
      System.exit(1);
    }

//    System.out.println(paths);
//    System.out.println(meta.toString());
//    System.out.println(peers);
  }
}
