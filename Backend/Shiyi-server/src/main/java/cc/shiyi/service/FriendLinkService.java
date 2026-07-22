package cc.shiyi.service;

import cc.shiyi.dto.FriendLinkDTO;
import cc.shiyi.entity.FriendLinks;
import cc.shiyi.vo.FriendLinkVO;

import java.util.List;

public interface FriendLinkService {
    /**
     * 管理端获取所有友情链接
     * @return
     */
    List<FriendLinks> getAllFriendLink();

    /**
     * 管理端添加友情链接
     * @param friendLinkDTO
     */
    void addFriendLink(FriendLinkDTO friendLinkDTO);

    /**
     * 批量删除友情链接
     * @param ids
     */
    void batchDelete(List<Long> ids);

    /**
     * 管理端修改友情链接
     * @param friendLinkDTO
     */
    void updateFriendLink(FriendLinkDTO friendLinkDTO);

    /**
     * 博客端获取可见友情链接
     * @return
     */
    List<FriendLinkVO> getVisibleFriendLink();
}
