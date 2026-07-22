package cc.shiyi.controller.admin;

import cc.shiyi.annotation.OperationLog;
import cc.shiyi.dto.MessagePageQueryDTO;
import cc.shiyi.dto.MessageReplyDTO;
import cc.shiyi.enumeration.OperationType;
import cc.shiyi.result.PageResult;
import cc.shiyi.result.Result;
import cc.shiyi.service.MessageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端留言接口
 */
@Slf4j
@RestController("adminMessageController")
@RequestMapping("/admin/message")
public class MessageController {

    @Autowired
    private MessageService messageService;

    /**
     * 分页条件查询留言
     * @param messagePageQueryDTO
     * @return
     */
    @GetMapping("/page")
    public Result<PageResult> pageQuery(MessagePageQueryDTO messagePageQueryDTO) {
        log.info("分页条件查询留言: {}", messagePageQueryDTO);
        PageResult pageResult = messageService.pageQuery(messagePageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 批量审核通过留言
     * @param ids
     * @return
     */
    @PutMapping("/approve")
    @OperationLog(value = OperationType.UPDATE, target = "message", targetId = "#ids")
    public Result<String> batchApprove(@RequestParam List<Long> ids) {
        log.info("批量审核通过留言: {}", ids);
        messageService.batchApprove(ids);
        return Result.success();
    }

    /**
     * 批量删除留言
     * @param ids
     * @return
     */
    @DeleteMapping
    @OperationLog(value = OperationType.DELETE, target = "message", targetId = "#ids")
    public Result<String> batchDelete(@RequestParam List<Long> ids) {
        log.info("批量删除留言: {}", ids);
        messageService.batchDelete(ids);
        return Result.success();
    }

    /**
     * 管理员回复留言
     * @param messageReplyDTO
     * @return
     */
    @PostMapping("/reply")
    @OperationLog(value = OperationType.INSERT, target = "message", targetId = "#messageReplyDTO.parentId")
    public Result<String> adminReply(@Valid @RequestBody MessageReplyDTO messageReplyDTO,
                                     HttpServletRequest request) {
        log.info("管理员回复留言: {}", messageReplyDTO);
        messageService.adminReply(messageReplyDTO, request);
        return Result.success();
    }
}
