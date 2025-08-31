import bencode.torrent.TorrentMeta;
import bencode.torrent.TorrentParser;

import java.io.IOException;
import java.util.List;

public class BitTorrentClient {
  public static void main(String[] args) {
    if (args.length != 3) {
      System.err.println("Usage: java -jar bittorrent-client.jar <torrent-file> <peers-conf> <port>");
      System.exit(1);
      return;
    }

    String torrentFilePath = args[0];
    String peersConfPath = args[1];
    int clientPort;
    try {
      clientPort = Integer.parseInt(args[2]);
    } catch (NumberFormatException e) {
      System.err.println("Error: <port> must be an integer>");
      System.exit(1);
      return;
    }

    TorrentMeta meta;
    try {
      meta = TorrentParser.parseTorrent(torrentFilePath);
    } catch (IOException e) {
      System.err.println("Failed to parse torrent file: " + e.getMessage());
      System.exit(1);
      return;
    }

    List<Peer> peers;
    try {
      peers = PeerConfigParser.getPeers(args[1]);
    } catch (IOException e) {
      System.err.println("Failed to parse peers: " + e.getMessage());
      System.exit(1);
      return;
    }


  }
}
