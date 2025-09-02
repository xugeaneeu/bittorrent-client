package bencode.torrent;

import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Setter
@Getter
public class TorrentMeta {
  private String announce;
  private String name;
  private Long pieceLength;
  private byte[] piecesBlob;
  private List<String> pieces;
  private boolean singleFileTorrent;
  private Long fileLength;
  private List<TorrentFile> fileList;
  private String comment;
  private String createdBy;
  private Date creationDate;
  private List<String> announceList;
  private String info_hash;

  @Override
  public String toString() {
    return "TorrentMeta{" +
            "announce='" + announce + '\'' +
            ", name='" + name + '\'' +
            ", pieceLength=" + pieceLength +
            ", piecesBlob.length=" + (piecesBlob == null ? 0 : piecesBlob.length) +
            ", piecesCount=" + (pieces == null ? 0 : pieces.size()) +
            ", singleFileTorrent=" + singleFileTorrent +
            ", fileLength=" + fileLength +
            ", fileList=" + (fileList == null ? "null" : fileList) +
            ", comment='" + comment + '\'' +
            ", createdBy='" + createdBy + '\'' +
            ", creationDate=" + creationDate +
            ", announceList=" + (announceList == null ? "null" : announceList) +
            ", info_hash='" + info_hash + '\'' +
            '}';
  }
}