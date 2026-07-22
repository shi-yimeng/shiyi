package cc.shiyi.controller.admin;

import cc.shiyi.annotation.OperationLog;
import cc.shiyi.dto.ExperienceDTO;
import cc.shiyi.entity.Experiences;
import cc.shiyi.enumeration.OperationType;
import cc.shiyi.result.Result;
import cc.shiyi.service.ExperienceService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 *  管理端经历接口
 */
@RestController("adminExperienceController")
@RequestMapping("/admin/experience")
@Slf4j
public class ExperienceController {

    @Autowired
    private ExperienceService experienceService;

    /**
     * 根据分类获取经历信息
     */
    @GetMapping
    public Result<List<Experiences>> getExperience(@RequestParam(required = false) Integer type) {
        log.info("根据分类获取经历信息,{}", type);
        List<Experiences> experienceList = experienceService.getExperience(type);
        return Result.success(experienceList);
    }

    /**
     * 添加经历信息
     */
    @PostMapping
    @OperationLog(value = OperationType.INSERT, target = "experience")
    public Result addExperience(@Valid @RequestBody ExperienceDTO experienceDTO) {
        log.info("添加经历信息,{}", experienceDTO);
        experienceService.addExperience(experienceDTO);
        return Result.success();
    }

    /**
     * 修改经历信息
     */
    @PutMapping
    @OperationLog(value = OperationType.UPDATE, target = "experience", targetId = "#experienceDTO.id")
    public Result updateExperience(@Valid @RequestBody ExperienceDTO experienceDTO) {
        log.info("修改经历信息,{}", experienceDTO);
        experienceService.updateExperience(experienceDTO);
        return Result.success();
    }

    /**
     * 批量删除经历信息
     */
    @DeleteMapping
    @OperationLog(value = OperationType.DELETE, target = "experience", targetId = "#ids")
    public Result deleteExperience(@RequestParam List<Long> ids) {
        log.info("批量删除经历信息,{}", ids);
        experienceService.batchDelete(ids);
        return Result.success();
    }

}
