package cc.shiyi.service;

import cc.shiyi.dto.MusicDTO;
import cc.shiyi.dto.MusicPageQueryDTO;
import cc.shiyi.entity.Music;
import cc.shiyi.result.PageResult;
import cc.shiyi.vo.MusicVO;

import java.util.List;

public interface MusicService {
    /**
     * 添加音乐
     * @param musicDTO
     */
    void addMusic(MusicDTO musicDTO);

    /**
     * 分页查询音乐列表
     * @param musicPageQueryDTO
     * @return
     */
    PageResult pageQuery(MusicPageQueryDTO musicPageQueryDTO);

    /**
     * 更新音乐
     * @param musicDTO
     */
    void updateMusic(MusicDTO musicDTO);

    /**
     * 批量删除音乐
     * @param ids
     */
    void batchDelete(List<Long> ids);

    /**
     * 根据ID查询音乐
     * @param id
     * @return
     */
    Music getById(Long id);

    /**
     * 获取所有可见的音乐
     * @return
     */
    List<MusicVO> getAllVisibleMusic();
}
