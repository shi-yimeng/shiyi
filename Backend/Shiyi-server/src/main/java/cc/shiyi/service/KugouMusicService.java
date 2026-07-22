package cc.shiyi.service;

import cc.shiyi.vo.KugouMusicVO;

import java.util.List;

public interface KugouMusicService {

    List<KugouMusicVO> search(String keyword, int page, int pageSize);

    KugouMusicVO getPlayUrl(String hash, String albumId);
}
