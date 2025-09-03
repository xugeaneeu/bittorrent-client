package config;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class PeerConfigParser {
  public static List<InetSocketAddress> getPeers(Path peerConfigFile) throws IOException {
    List<InetSocketAddress> peers = new ArrayList<>();
    try (BufferedReader br = Files.newBufferedReader(peerConfigFile, StandardCharsets.UTF_8)) {
      String line;
      while ((line = br.readLine()) != null) {
        line = line.trim();
        if (line.isEmpty() || line.startsWith("#")) {continue;}
        String[] parts = line.split(":");
        if (parts.length != 2) {
          System.err.println("Invalid peer entry: " + line);
          continue;
        }
        String host = parts[0];
        int port;
        try {
          port = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
          System.err.println("Invalid port: " + parts[1]);
          continue;
        }
        peers.add(new InetSocketAddress(host, port));
      }
    }
    return peers;
  }
}
