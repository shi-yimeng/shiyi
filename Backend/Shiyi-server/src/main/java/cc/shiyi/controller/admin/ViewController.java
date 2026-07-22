package cc.shiyi.controller.admin;

import cc.shiyi.dto.ViewPageQueryDTO;
import cc.shiyi.result.PageResult;
import cc.shiyi.result.Result;
import cc.shiyi.service.ViewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端浏览记录接口
 */
@Slf4j
@RestController("adminViewController")
@RequestMapping("/admin/view")
public class ViewController {

    @Autowired
    private ViewService viewService;

    /**
     * 获取浏览记录列表
     * @param viewPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    public Result<PageResult> getViewList(ViewPageQueryDTO viewPageQueryDTO) {
        log.info("获取浏览记录列表,{}", viewPageQueryDTO);
        PageResult pageResult = viewService.pageQuery(viewPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 批量删除浏览记录
     * @param ids
     * @return
     */
    @DeleteMapping
    public Result batchDelete(@RequestParam List<Long> ids) {
        log.info("批量删除浏览记录,{}", ids);
        viewService.batchDelete(ids);
        return Result.success();
    }
}
