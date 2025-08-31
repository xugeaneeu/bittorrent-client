import java.net.InetSocketAddress;
import java.util.Objects;

public class Peer {
  private final String host;
  private final int port;

  public Peer(String host, int port) {
    this.host = Objects.requireNonNull(host);
    this.port = port;
  }

  public InetSocketAddress getSocketAddress() {
    return new InetSocketAddress(host, port);
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  @Override
  public String toString() {
    return host + ":" + port;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Peer peer)) return false;
    return port == peer.port && host.equals(peer.host);
  }
}
