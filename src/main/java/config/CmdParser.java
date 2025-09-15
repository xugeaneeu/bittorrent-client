package config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Getter
public class CmdParser {
  private final Path torrentFilePath;
  private final Path peersConfigPath;
  private final int listenPort;

  CmdParser(Path torrentFilePath, Path peersConfigPath, int listenPort) {
    this.torrentFilePath = torrentFilePath;
    this.peersConfigPath = peersConfigPath;
    this.listenPort = listenPort;
  }

  public static CmdParser parse(String[] args) {
    if (args.length != 3) {
      log.error("Usage: java -jar bittorrent-client.jar <torrent-file> <peers-conf> <listen-port>");
      System.exit(1);
    }

    Path torrentFilePath = Paths.get(args[0]);
    Path peersConfPath = Paths.get(args[1]);
    int listenPort;
    try {
      listenPort = Integer.parseInt(args[2]);
    } catch (NumberFormatException e) {
      log.error("Error: <listen-port> must be an integer, got='{}'", args[2]);
      System.exit(1);
      return null;
    }

    log.info("Parsed arguments: torrent='{}', peers='{}', port={}",
            torrentFilePath, peersConfPath, listenPort);

    return new CmdParser(torrentFilePath, peersConfPath, listenPort);
  }
}
