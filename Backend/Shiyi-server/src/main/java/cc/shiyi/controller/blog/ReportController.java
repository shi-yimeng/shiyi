package cc.shiyi.controller.blog;

import cc.shiyi.result.Result;
import cc.shiyi.service.ReportService;
import cc.shiyi.vo.BlogReportVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 博客端统计相关接口
 */
@Slf4j
@RestController("blogReportController")
@RequestMapping("/blog/report")
public class ReportController {

    @Autowired
    private ReportService reportService;

    /**
     * 获取博客统计数据
     */
    @GetMapping
    public Result<BlogReportVO> getBlogReport() {
        log.info("博客端获取统计数据");
        BlogReportVO blogReportVO = reportService.getBlogReport();
        return Result.success(blogReportVO);
    }
}
