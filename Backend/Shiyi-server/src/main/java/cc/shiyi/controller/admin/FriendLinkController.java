package cc.shiyi.controller.admin;

import cc.shiyi.annotation.OperationLog;
import cc.shiyi.dto.FriendLinkDTO;
import cc.shiyi.entity.FriendLinks;
import cc.shiyi.enumeration.OperationType;
import cc.shiyi.result.Result;
import cc.shiyi.service.FriendLinkService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端友链接口
 */
@RestController("adminFriendLinkController")
@RequestMapping("/admin/friendLink")
@Slf4j
public class FriendLinkController {

    @Autowired
    private FriendLinkService friendLinkService;

    /**
     * 获取所有友情链接信息
     */
    @GetMapping
    public Result<List<FriendLinks>> getAllFriendLink() {
        List<FriendLinks> friendLinkList = friendLinkService.getAllFriendLink();
        return Result.success(friendLinkList);
    }

    /**
     * 添加友情链接信息
     */
    @PostMapping
    @OperationLog(value = OperationType.INSERT, target = "friendLink")
    public Result addFriendLink(@Valid @RequestBody FriendLinkDTO friendLinkDTO) {
        log.info("添加友情链接信息:{}", friendLinkDTO);
        friendLinkService.addFriendLink(friendLinkDTO);
        return Result.success();
    }

    /**
     * 批量删除友情链接信息
     */
    @DeleteMapping
    @OperationLog(value = OperationType.DELETE, target = "friendLink", targetId = "#ids")
    public Result deleteFriendLink(@RequestParam List<Long> ids) {
        log.info("批量删除友情链接信息:{}", ids);
        friendLinkService.batchDelete(ids);
        return Result.success();
    }

    /**
     * 修改友情链接信息
     */
    @PutMapping
    @OperationLog(value = OperationType.UPDATE, target = "friendLink", targetId = "#friendLinkDTO.id")
    public Result updateFriendLink(@Valid @RequestBody FriendLinkDTO friendLinkDTO) {
        log.info("修改友情链接信息:{}", friendLinkDTO);
        friendLinkService.updateFriendLink(friendLinkDTO);
        return Result.success();
    }
}
