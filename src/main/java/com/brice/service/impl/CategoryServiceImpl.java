package com.brice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.brice.common.CustomException;
import com.brice.entity.Category;
import com.brice.entity.Dish;
import com.brice.entity.Setmeal;
import com.brice.mapper.CategoryMapper;
import com.brice.service.CategoryService;
import com.brice.service.DishService;
import com.brice.service.SetmealService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {
    @Autowired
    private DishService dishService;

    @Autowired
    private SetmealService setmealService;

    /**
     * 根据id删除分类，删除之前判断是否有关联
     *
     * @param id
     */
    @Override
    public void remove(Long id) {
        LambdaQueryWrapper<Dish> dishLambdaQueryWrapper = new LambdaQueryWrapper<>();
        //根据id查询是否有关联菜品
        //select * from dish where category_id = ?
        dishLambdaQueryWrapper.eq(Dish::getCategoryId, id);
        long dCount = dishService.count(dishLambdaQueryWrapper);

        if (dCount > 0){
            //已关联菜品，抛出异常
            throw new CustomException("当前分类下有关联的菜品，删除失败");
        }

        LambdaQueryWrapper<Setmeal> setmealLambdaQueryWrapper = new LambdaQueryWrapper<>();
        //根据id查询是否有关联套餐
        setmealLambdaQueryWrapper.eq(Setmeal::getCategoryId,id);
        long sCount = setmealService.count(setmealLambdaQueryWrapper);

        if (sCount > 0){
            //已关联套餐，抛出异常
            throw new CustomException("当前分类下有关联的套餐，删除失败");
        }

        //正常删除
        super.removeById(id);
    }
}
