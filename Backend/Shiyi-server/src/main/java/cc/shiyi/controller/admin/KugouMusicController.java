package cc.shiyi.controller.admin;

import cc.shiyi.result.Result;
import cc.shiyi.service.KugouMusicService;
import cc.shiyi.vo.KugouMusicVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/admin/music/kugou")
public class KugouMusicController {

    @Autowired
    private KugouMusicService kugouMusicService;

    @GetMapping("/search")
    public Result<List<KugouMusicVO>> search(@RequestParam String keyword,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int pageSize) {
        log.info("[Kugou] search: keyword={}, page={}, pageSize={}", keyword, page, pageSize);
        List<KugouMusicVO> result = kugouMusicService.search(keyword, page, pageSize);
        return Result.success(result);
    }

    @GetMapping("/url")
    public Result<KugouMusicVO> getPlayUrl(@RequestParam String hash,
                                            @RequestParam(required = false) String albumId) {
        log.info("[Kugou] getPlayUrl: hash={}, albumId={}", hash, albumId);
        KugouMusicVO result = kugouMusicService.getPlayUrl(hash, albumId);
        if (result == null) {
            return Result.error("获取播放链接失败");
        }
        return Result.success(result);
    }
}
