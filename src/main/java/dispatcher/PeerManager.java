package dispatcher;

import bencode.torrent.TorrentMeta;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
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
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

@Slf4j
public class PeerManager implements ProtocolListener {
  private final NetworkReactor reactor;
  private final byte[] infoHash;
  private final byte[] myPeerId;
  @Getter private final boolean[] localBitmap;
  @Getter private final FileManager fileManager;
  @Setter private DownloadScheduler scheduler;
  private final ExecutorService pool;

  @Getter
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
    log.info("PeerManager initialized: infoHash={} peerId={}",
            Hex.encodeHexString(infoHash),
            new String(myPeerId));
  }

  @Override
  public void onChannelConnected(PeerChannel peer) {
    try {
      log.info("Channel connected: {}", peer.getChannel().getRemoteAddress());
    } catch (IOException _) { }
    pool.submit(() -> {
      try {
        log.debug("Sending HANDSHAKE to {}", peer.getChannel().getRemoteAddress());
      } catch (IOException _) { }
      reactor.send(peer, new HandshakeMessage(infoHash, myPeerId));
    });
  }

  @Override
  public void onMessage(PeerChannel peer, Message msg) {
    try {
      log.debug("onMessage {} from {}", msg.getType(), peer.getChannel().getRemoteAddress());
    } catch (IOException _) { }
    pool.submit(() -> {
      switch (msg.getType()) {
        case HANDSHAKE -> handleHandshake(peer, (HandshakeMessage) msg);
        case BITFIELD -> handleBitfield(peer, (BitfieldMessage) msg);
        case HAVE -> handleHave(peer, (HaveMessage) msg);
        case REQUEST -> handleRequest(peer, (RequestMessage) msg);
        case PIECE -> handlePiece(peer, (PieceMessage) msg);
        default -> log.debug("Ignoring message type {}", msg.getType());
      }
    });
  }

  public Set<PeerChannel> getAllPeerChannels() {
    return Collections.unmodifiableSet(remoteBitmaps.keySet());
  }

  private void handleHandshake(PeerChannel peer, HandshakeMessage msg) {
    try {
      log.debug("handleHandshake from {}", peer.getChannel().getRemoteAddress());
      if (!Arrays.equals(msg.getInfoHash(), infoHash)) {
        log.warn("Invalid infoHash from {}, closing", peer.getChannel().getRemoteAddress());
        try { peer.getChannel().close(); } catch (IOException _) {}
        return;
      }
      if (!peer.isInitiator()) {
        log.info("Replying HANDSHAKE to {}", peer.getChannel().getRemoteAddress());
        reactor.send(peer, new HandshakeMessage(infoHash, myPeerId));
      }
      byte[] bf = BitmapUtils.serialize(localBitmap);
      log.info("Sending BITFIELD to {}", peer.getChannel().getRemoteAddress());
      reactor.send(peer, new BitfieldMessage(bf));
    } catch (IOException _) { }
  }

  private void handleBitfield(PeerChannel peer, BitfieldMessage msg) {
    try {
      log.info("handleBitfield from {}", peer.getChannel().getRemoteAddress());
    } catch (IOException _) { }
    boolean[] remote = BitmapUtils.deserialize(msg.getRaw(), localBitmap.length);
    remoteBitmaps.put(peer, remote);
    scheduler.peerReady(peer);
  }

  private void handleHave(PeerChannel peer, HaveMessage msg) {
    try {
      log.debug("handleHave index={} from {}", msg.getPieceIndex(), peer.getChannel().getRemoteAddress());
    } catch (IOException _) { }
    boolean[] remote = remoteBitmaps.get(peer);
    if (remote != null) {
      remote[msg.getPieceIndex()] = true;
      scheduler.peerReady(peer);
    }
  }

  private void handleRequest(PeerChannel peer, RequestMessage msg) {
    try {
      log.debug("handleRequest index={} from {}", msg.getIndex(), peer.getChannel().getRemoteAddress());
      byte[] data = fileManager.readPiece(msg.getIndex());
      log.info("Replying PIECE index={} to {}", msg.getIndex(), peer.getChannel().getRemoteAddress());
      reactor.send(peer, new PieceMessage(msg.getIndex(), msg.getBegin(), data));
    } catch (IOException _) { }
  }

  private void handlePiece(PeerChannel peer, PieceMessage msg) {
    int idx = msg.getIndex();
    try {
      log.debug("handlePiece index={} from {}", idx, peer.getChannel().getRemoteAddress());
    } catch (IOException _) { }
    byte[] blk = msg.getBlock();
    try {
      if (!fileManager.checkHash(idx, blk)) {
        log.warn("Piece {} failed hash check", idx);
        return;
      }
      fileManager.writePiece(idx, blk);
      localBitmap[idx] = true;
      log.info("Stored piece {}", idx);

      HaveMessage hm = new HaveMessage(idx);
      remoteBitmaps.keySet().forEach(other -> {
        if (other != peer) {
          try {
            log.debug("Broadcast HAVE {} to {}", idx, other.getChannel().getRemoteAddress());
          } catch (IOException _) { }
          reactor.send(other, hm);
        }
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
