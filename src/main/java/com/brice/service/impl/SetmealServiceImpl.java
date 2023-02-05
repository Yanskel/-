package com.brice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.brice.common.CustomException;
import com.brice.dto.SetmealDto;
import com.brice.entity.Setmeal;
import com.brice.entity.SetmealDish;
import com.brice.mapper.SetmealMapper;
import com.brice.service.SetmealDishService;
import com.brice.service.SetmealService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {
    @Autowired
    private SetmealDishService setmealDishService;

    /**
     * 新增套餐，同时保存套餐与菜品关联数据
     *
     * @param setmealDto
     */
    @Override
    @Transactional
    public void saveWithDish(SetmealDto setmealDto) {
        //保存套餐基本信息
        this.save(setmealDto);

        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();
        setmealDishes = setmealDishes.stream().peek(item -> item.setSetmealId(setmealDto.getId())).collect(Collectors.toList());
        //保存套餐关联菜品信息，setmeal_dish表
        setmealDishService.saveBatch(setmealDishes);
    }

    /**
     * 删除套餐及其菜品关联信息
     *
     * @param ids
     */
    @Override
    @Transactional
    public void removeWithDish(List<Long> ids) {
        //查询套餐状态，确定是否可以删除
        LambdaQueryWrapper<Setmeal> setmealLambdaQueryWrapper = new LambdaQueryWrapper<>();
        setmealLambdaQueryWrapper.in(Setmeal::getId, ids);
        setmealLambdaQueryWrapper.eq(Setmeal::getStatus, 1);
        //统计在售的菜品
        long count = this.count(setmealLambdaQueryWrapper);
        //不能删除，抛出异常
        if (count > 0) {
            throw new CustomException("套餐正在售卖中，不能删除");
        }
        //可以删除，删除套餐表
        this.removeByIds(ids);
        //删除套餐关联表,根据setmeal_id删除
        LambdaQueryWrapper<SetmealDish> dishLambdaQueryWrapper = new LambdaQueryWrapper<>();
        dishLambdaQueryWrapper.in(SetmealDish::getSetmealId,ids);
        setmealDishService.remove(dishLambdaQueryWrapper);
    }
}
