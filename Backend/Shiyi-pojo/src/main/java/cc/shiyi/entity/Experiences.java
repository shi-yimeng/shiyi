package cc.shiyi.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 经历
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Experiences implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    // 类型，0-教育经历，1-实习及工作经历,2-项目经历
    private Integer type;

    // 标题,公司名/学校名/项目名
    private String title;

    // 副标题,职位/专业/项目角色
    private String subtitle;

    // logo
    private String logoUrl;

    // 内容
    private String content;

    // 开始时间
    private LocalDate startDate;

    // 结束时间
    private LocalDate endDate;

    // 是否可见
    private Integer isVisible;

    // 创建时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    // 更新时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
