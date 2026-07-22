package cc.shiyi.controller.blog;

import cc.shiyi.result.Result;
import cc.shiyi.service.MusicService;
import cc.shiyi.vo.MusicVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 博客端音乐接口
 */
@Slf4j
@RestController("blogMusicController")
@RequestMapping("/blog/music")
public class MusicController {

    @Autowired
    private MusicService musicService;

    /**
     * 获取所有可见的音乐
     * @return
     */
    @GetMapping
    public Result<List<MusicVO>> getAllVisibleMusic() {
        List<MusicVO> musicVOList = musicService.getAllVisibleMusic();
        return Result.success(musicVOList);
    }
}
