package network;

import dispatcher.PeerManager;
import lombok.SneakyThrows;

import java.net.InetSocketAddress;

public class NetworkReactor implements Runnable {
  @SneakyThrows
  @Override
  public void run() {
    Thread.sleep(100000);
  }

  public void setListener(PeerManager peerManager) {

  }

  public void registerServer(int listenPort) {

  }

  public void registerClient(InetSocketAddress peer) {

  }
}
