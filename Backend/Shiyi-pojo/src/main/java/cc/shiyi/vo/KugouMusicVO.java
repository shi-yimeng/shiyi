package cc.shiyi.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class KugouMusicVO {

    private String title;

    private String artist;

    private String album;

    private Integer duration;

    private String coverImage;

    private String musicUrl;

    private String lyricUrl;

    private String hash;

    private String albumId;
}
