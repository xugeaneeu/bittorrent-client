package config;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class PeerConfigParser {
  public static List<InetSocketAddress> getPeers(Path peerConfigFile) throws IOException {
    List<InetSocketAddress> peers = new ArrayList<>();

    log.info("Loading peers from {}", peerConfigFile);
    try (BufferedReader br = Files.newBufferedReader(peerConfigFile, StandardCharsets.UTF_8)) {
      String line;
      while ((line = br.readLine()) != null) {
        line = line.trim();
        if (line.isEmpty() || line.startsWith("#")) {continue;}
        String[] parts = line.split(":");
        if (parts.length != 2) {
          log.error("Invalid peer entry: {}", line);
          continue;
        }
        String host = parts[0];
        int port;
        try {
          port = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
          log.error("Invalid port: {}", parts[1]);
          continue;
        }
        peers.add(new InetSocketAddress(host, port));
        log.info("Added peer {}:{}", host, port);
      }
    }
    return peers;
  }
}
