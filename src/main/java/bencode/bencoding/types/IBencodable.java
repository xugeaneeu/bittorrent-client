package bencode.bencoding.types;

public interface IBencodable {
  /**
   * @return byte representation of the bencoded object.
   */
  byte[] bencode();

  /**
   * @return string representation of bencoded object.
   */
  String bencodedString();
}
