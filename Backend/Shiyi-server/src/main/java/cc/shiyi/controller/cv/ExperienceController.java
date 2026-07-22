package cc.shiyi.controller.cv;

import cc.shiyi.entity.Experiences;
import cc.shiyi.result.Result;
import cc.shiyi.service.ExperienceService;
import cc.shiyi.vo.ExperienceVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 简历端经历接口
 */
@RestController("cvExperienceController")
@RequestMapping("/cv/experience")
public class ExperienceController {

    @Autowired
    private ExperienceService experienceService;

    /**
     * 获取全部经历信息
     */
    @GetMapping
    public Result<List<ExperienceVO>> getAllExperience() {
        List<ExperienceVO> experienceList = experienceService.getAllExperience();
        return Result.success(experienceList);
    }
}
