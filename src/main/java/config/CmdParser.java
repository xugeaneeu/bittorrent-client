package config;

import lombok.Getter;
import lombok.ToString;

import java.nio.file.Path;
import java.nio.file.Paths;

@Getter
@ToString
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
      System.err.println("Usage: java -jar bittorrent-client.jar <torrent-file> <peers-conf> <listen-port>");
      System.exit(1);
    }

    Path torrentFilePath = Paths.get(args[0]);
    Path peersConfPath = Paths.get(args[1]);
    int listenPort;
    try {
      listenPort = Integer.parseInt(args[2]);
    } catch (NumberFormatException e) {
      System.err.println("Error: <listen-port> must be an integer>");
      System.exit(1);
      return null;
    }

    return new CmdParser(torrentFilePath, peersConfPath, listenPort);
  }
}
