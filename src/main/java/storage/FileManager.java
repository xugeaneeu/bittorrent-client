package storage;

import bencode.torrent.TorrentMeta;

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

public class FileManager implements AutoCloseable {
  private final Path filePath;
  private final int pieceLength;
  private final int pieceCount;
  private final long fileLength;
  private final boolean[] localBitmap;
  private final FileChannel channel;

  public FileManager(TorrentMeta meta) throws IOException, NoSuchAlgorithmException, DecoderException {
    this.pieceLength = meta.getPieceLength().intValue();
    this.pieceCount  = meta.getPieces().size();
    this.fileLength  = meta.getFileLength();
    this.filePath    = Path.of(meta.getName());

    prepareFile();

    this.channel = FileChannel.open(
            filePath,
            StandardOpenOption.CREATE,
            StandardOpenOption.READ,
            StandardOpenOption.WRITE
    );
    channel.truncate(fileLength);

    this.localBitmap = buildLocalBitmap(meta);
  }

  public boolean[] getLocalBitmap() {
    return Arrays.copyOf(localBitmap, localBitmap.length);
  }

  public byte[] readPiece(int index) throws IOException {
    long offset = (long) index * pieceLength;
    int length = pieceSize(index);
    ByteBuffer buffer = ByteBuffer.allocate(length);

    int read = 0;
    while (read < length) {
      int n = channel.read(buffer, offset + read);
      if (n < 0) {
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
    ByteBuffer buffer = ByteBuffer.wrap(data);

    while (buffer.hasRemaining()) {
      channel.write(buffer, offset + (data.length - buffer.remaining()));
    }
  }

  private boolean[] buildLocalBitmap(TorrentMeta meta)
          throws IOException, NoSuchAlgorithmException, DecoderException {
    boolean[] bitmap = new boolean[pieceCount];
    for (int i = 0; i < pieceCount; i++) {
      byte[] data = readPiece(i);
      bitmap[i] = checkHash(i, data, meta);
    }
    return bitmap;
  }

  private int pieceSize(int index) {
    long offset = (long) pieceLength * index;
    long remaining = fileLength - offset;
    return (int) Math.min(remaining, pieceLength);
  }

  private void prepareFile() throws IOException {
    if (!Files.exists(filePath)) {
      Files.createFile(filePath);
    }
  }

  private boolean checkHash(int index, byte[] data, TorrentMeta meta)
          throws NoSuchAlgorithmException, DecoderException {
    MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
    byte[] digest = sha1.digest(data);

    String expectedHex = meta.getPieces().get(index);
    byte[] expected = Hex.decodeHex(expectedHex.toCharArray());
    return Arrays.equals(digest, expected);
  }

  @Override
  public void close() throws IOException {
    channel.close();
  }
}
