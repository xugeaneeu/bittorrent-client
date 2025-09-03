package storage;

public class BitmapUtils {
  public static byte[] serialize(boolean[] bitmap) {
    int byteLen = (bitmap.length + 7) / 8;
    byte[] out = new byte[byteLen];
    for (int i = 0; i < byteLen; i++) {
      if (bitmap[i]) {
        int b = i/8;
        int bit = 7 - (i%8);
        out[b] = (byte) (out[b] | (1 << bit));
      }
    }
    return out;
  }

  public static boolean[] deserialize(byte[] data, int pieceCount) {
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
    return bitmap;
  }
}
