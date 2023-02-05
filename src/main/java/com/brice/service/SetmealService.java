package com.brice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.brice.dto.SetmealDto;
import com.brice.entity.Setmeal;

import java.util.List;

public interface SetmealService extends IService<Setmeal> {
    /**
     * 新增套餐，同时保存套餐与菜品关联数据
     *
     * @param setmealDto
     */
    void saveWithDish(SetmealDto setmealDto);

    /**
     * 删除套餐及其菜品关联信息
     *
     * @param ids
     */
    void removeWithDish(List<Long> ids);
}
