package cc.shiyi.service.impl;

import cc.shiyi.dto.ViewPageQueryDTO;
import cc.shiyi.entity.Views;
import cc.shiyi.mapper.ViewMapper;
import cc.shiyi.result.PageResult;
import cc.shiyi.service.ViewService;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ViewServiceImpl implements ViewService {

    @Autowired
    private ViewMapper viewMapper;

    /**
     * 分页查询浏览记录
     * @param viewPageQueryDTO
     * @return
     */
    public PageResult pageQuery(ViewPageQueryDTO viewPageQueryDTO) {
        PageHelper.startPage(viewPageQueryDTO.getPage(), viewPageQueryDTO.getPageSize());
        Page<Views> page = viewMapper.pageQuery(viewPageQueryDTO);
        long total = page.getTotal();
        List<Views> records = page.getResult();
        return new PageResult(total, records);
    }

    /**
     * 批量删除浏览记录
     * @param ids
     */
    public void batchDelete(List<Long> ids) {
        viewMapper.batchDelete(ids);
    }
}
