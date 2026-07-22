package cc.shiyi.service;

import cc.shiyi.dto.PersonalInfoDTO;
import cc.shiyi.entity.PersonalInfo;
import cc.shiyi.vo.PersonalInfoVO;

public interface PersonalInfoService {
    /**
     * 获取个人信息
     * @return
     */
    PersonalInfo getAllPersonalInfo();

    /**
     * 更新个人信息
     * @param personalInfoDTO
     */
    void updatePersonalInfo(PersonalInfoDTO personalInfoDTO);

    /**
     * 其他端获取个人信息
     * @return
     */
    PersonalInfoVO getPersonalInfo();
}
