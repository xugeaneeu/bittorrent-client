import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PeerConfigParser {

  public static List<Peer> getPeers(String path) throws IOException {
    List<Peer> peers = new ArrayList<>();

    try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
      String line;
      int lineNo = 0;
      while ((line = reader.readLine()) != null) {
        lineNo++;
        line = line.trim();
        if (line.isEmpty() || line.startsWith("#")) {
          continue;
        }
        String[] parts = line.split(":", 2);
        if (parts.length != 2) {
          throw new IOException("Bad peer config at line " + lineNo + ": '" + line + "'");
        }

        String host = parts[0];
        int port;
        try {
          port = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
          throw new IOException("Bad port number at line " + lineNo + ": '" + parts[1] + "'");
        }
        peers.add(new Peer(host, port));
      }
    }
    return peers;
  }

}
