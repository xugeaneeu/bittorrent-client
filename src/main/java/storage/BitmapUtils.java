package storage;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BitmapUtils {
  public static byte[] serialize(boolean[] bitmap) {
    log.debug("BitmapUtils.serialize: bitmap.length={}", bitmap.length);
    int byteLen = (bitmap.length + 7) / 8;
    byte[] out = new byte[byteLen];
    for (int i = 0; i < bitmap.length; i++) {
      if (bitmap[i]) {
        int b = i/8;
        int bit = 7 - (i%8);
        out[b] = (byte) (out[b] | (1 << bit));
      }
    }
    log.trace("BitmapUtils.serialize → {} bytes", out.length);
    return out;
  }

  public static boolean[] deserialize(byte[] data, int pieceCount) {
    log.debug("BitmapUtils.deserialize: data.length={}, pieceCount={}", data.length, pieceCount);
    boolean[] bitmap = new boolean[pieceCount];
    for (int i = 0; i < pieceCount; i++) {
      int b = i/8;
      int bit = 7 - (i%8);
      if (b < data.length) {
        bitmap[i] = ((data[b] >> bit) & 1) == 1;
      } else {
        bitmap[i] = false;
      }
    }
    log.trace("BitmapUtils.deserialize → boolean[{}]", bitmap.length);
    return bitmap;
  }
}
