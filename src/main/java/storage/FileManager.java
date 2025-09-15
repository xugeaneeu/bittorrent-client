package storage;

import bencode.torrent.TorrentMeta;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class FileManager implements AutoCloseable {
  private final Path filePath;
  private final int pieceLength;
  private final byte[][] pieceHashes;
  private final long fileLength;
  private final boolean[] localBitmap;
  private final FileChannel channel;

  public FileManager(TorrentMeta meta) throws IOException, NoSuchAlgorithmException, DecoderException {
    this.pieceLength = meta.getPieceLength().intValue();
    this.fileLength  = meta.getFileLength();
    this.filePath    = Path.of(meta.getName());
    this.pieceHashes = decodeAllPieceHashes(meta.getPieces());

    log.info("Preparing file {} (length={} bytes)", filePath, fileLength);
    prepareFile();

    this.channel = FileChannel.open(
            filePath,
            StandardOpenOption.READ,
            StandardOpenOption.WRITE
    );
    log.info("Opened FileChannel on {}, actual size={}", filePath, channel.size());
    channel.truncate(fileLength);
    log.debug("Truncated file to {} bytes", fileLength);

    log.info("Building local bitmap for {} pieces", pieceHashes.length);
    this.localBitmap = buildLocalBitmap();
  }

  private byte[][] decodeAllPieceHashes(List<String> pieces) throws DecoderException {
    int count = pieces.size();
    byte[][] hashes = new byte[count][];
    for (int i = 0; i < count; i++) {
      hashes[i] = Hex.decodeHex(pieces.get(i).toCharArray());
    }
    return hashes;
  }

  public boolean[] getLocalBitmap() {
    return Arrays.copyOf(localBitmap, localBitmap.length);
  }

  public byte[] readPiece(int index) throws IOException {
    long offset = (long) index * pieceLength;
    int length = pieceSize(index);
    log.debug("Reading piece {}: offset={}, length={}", index, offset, length);
    ByteBuffer buffer = ByteBuffer.allocate(length);

    int read = 0;
    while (read < length) {
      int n = channel.read(buffer, offset + read);
      if (n < 0) {
        log.error("Unexpected EOF while reading piece {}", index);
        throw new EOFException("Unexpected EOF while reading piece " + index);
      }
      read += n;
    }
    buffer.flip();
    byte[] data = new byte[length];
    buffer.get(data);
    return data;
  }

  public void writePiece(int index, byte[] data) throws IOException {
    long offset = (long) index * pieceLength;
    log.debug("Writing piece {}: offset={}, bytes={}", index, offset, data.length);
    ByteBuffer buffer = ByteBuffer.wrap(data);

    while (buffer.hasRemaining()) {
      channel.write(buffer, offset + (data.length - buffer.remaining()));
    }
  }

  public int pieceSize(int index) {
    long offset = (long) pieceLength * index;
    long remaining = fileLength - offset;
    return (int) Math.min(remaining, pieceLength);
  }

  private boolean[] buildLocalBitmap() throws IOException, NoSuchAlgorithmException {
    boolean[] bitmap = new boolean[pieceHashes.length];
    for (int i = 0; i < pieceHashes.length; i++) {
      byte[] data = readPiece(i);
      bitmap[i] = checkHash(i, data);
    }
    return bitmap;
  }

  private void prepareFile() throws IOException {
    if (!Files.exists(filePath)) {
      log.info("File {} does not exist, creating and setting length", filePath);
      try (RandomAccessFile raf = new RandomAccessFile(filePath.toFile(), "rw")) {
        raf.setLength(fileLength);
      }
    }
  }

  public boolean checkHash(int index, byte[] data) throws NoSuchAlgorithmException {
    MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
    byte[] digest = sha1.digest(data);
    return Arrays.equals(digest, pieceHashes[index]);
  }

  @Override
  public void close() throws IOException {
    log.info("Closing FileChannel for {}", filePath);
    channel.close();
  }
}
