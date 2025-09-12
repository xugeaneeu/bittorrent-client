package dispatcher;

import bencode.torrent.TorrentMeta;
import lombok.Setter;
import network.NetworkReactor;
import network.PeerChannel;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import protocol.messages.*;
import storage.BitmapUtils;
import storage.FileManager;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

public class PeerManager implements ProtocolListener {
  private final NetworkReactor reactor;
  private final byte[] infoHash;
  private final byte[] myPeerId;
  private final boolean[] localBitmap;
  private final FileManager fileManager;
  @Setter
  private DownloadScheduler scheduler;
  private final ExecutorService pool;

  private final ConcurrentHashMap<PeerChannel, boolean[]> remoteBitmaps = new ConcurrentHashMap<>();

  public PeerManager(NetworkReactor reactor,
                     TorrentMeta meta,
                     FileManager fileManager,
                     ExecutorService pool) throws DecoderException {
    this.reactor = reactor;
    this.infoHash = Hex.decodeHex(meta.getInfo_hash().toCharArray());
    this.myPeerId = genPeerId();
    this.localBitmap = fileManager.getLocalBitmap();
    this.fileManager = fileManager;
    this.pool = pool;
  }

  @Override
  public void onChannelConnected(PeerChannel peer) {
    pool.submit(() -> {
      reactor.send(peer, new HandshakeMessage(infoHash, myPeerId));
    });
  }

  @Override
  public void onMessage(PeerChannel peer, Message msg) {
    pool.submit(() -> {
      switch (msg.getType()) {
        case HANDSHAKE -> handleHandshake(peer, (HandshakeMessage) msg);
        case BITFIELD -> handleBitfield(peer, (BitfieldMessage) msg);
        case HAVE -> handleHave(peer, (HaveMessage) msg);
        case REQUEST -> handleRequest(peer, (RequestMessage) msg);
        case PIECE -> handlePiece(peer, (PieceMessage) msg);
        default -> { }
      }
    });
  }

  private void handleHandshake(PeerChannel peer, HandshakeMessage msg) {
    if (!Arrays.equals(msg.getInfoHash(), infoHash)) {
      try { peer.getChannel().close(); } catch (IOException _) {}
      return;
    }
    if (!peer.isInitiator()) {
      reactor.send(peer, new HandshakeMessage(infoHash, myPeerId));
    }
    byte[] bf = BitmapUtils.serialize(localBitmap);
    reactor.send(peer, new BitfieldMessage(bf));
  }

  private void handleBitfield(PeerChannel peer, BitfieldMessage msg) {
    boolean[] remote = BitmapUtils.deserialize(msg.getRaw(), localBitmap.length);
    remoteBitmaps.put(peer, remote);
    scheduler.peerReady(peer);
  }

  private void handleHave(PeerChannel peer, HaveMessage msg) {
    boolean[] remote = remoteBitmaps.get(peer);
    if (remote != null) {
      remote[msg.getPieceIndex()] = true;
      scheduler.peerReady(peer);
    }
  }

  private void handleRequest(PeerChannel peer, RequestMessage msg) {
    try {
      byte[] data = fileManager.readPiece(msg.getIndex());
      reactor.send(peer, new PieceMessage(msg.getIndex(), msg.getBegin(), data));
    } catch (IOException _) { }
  }

  private void handlePiece(PeerChannel peer, PieceMessage msg) {
    int idx = msg.getIndex();
    byte[] blk = msg.getBlock();
    try {
      if (!fileManager.checkHash(idx, blk)) {return; }
      fileManager.writePiece(idx, blk);
      localBitmap[idx] = true;
      HaveMessage hm = new HaveMessage(idx);
      remoteBitmaps.keySet().forEach(other -> {
        if (other != peer) reactor.send(other, hm);
      });
      scheduler.pieceReceived(idx, peer);
    } catch (IOException | NoSuchAlgorithmException _) { }
  }

  private static byte[] genPeerId() {
    String prefix = "-JM0001-";
    String rand   = UUID.randomUUID().toString().replace("-", "")
                        .substring(0, 12);
    byte[] id = new byte[20];
    System.arraycopy(prefix.getBytes(), 0, id, 0, prefix.length());
    System.arraycopy(rand.getBytes(),   0, id, prefix.length(), 12);
    return id;
  }
}
