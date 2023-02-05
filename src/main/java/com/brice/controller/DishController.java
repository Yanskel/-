package com.brice.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.brice.common.CustomException;
import com.brice.common.R;
import com.brice.dto.DishDto;
import com.brice.entity.Category;
import com.brice.entity.Dish;
import com.brice.entity.DishFlavor;
import com.brice.service.CategoryService;
import com.brice.service.DishFlavorService;
import com.brice.service.DishService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/dish")
public class DishController {
    @Autowired
    private DishService dishService;
    @Autowired
    private DishFlavorService dishFlavorService;
    @Autowired
    private CategoryService categoryService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 新增菜品
     */
    @PostMapping
    public R<String> save(@RequestBody DishDto dishDto) {
        dishService.saveWithFlavor(dishDto);
        //清理redis缓存
        String key = "dish_" + dishDto.getCategoryId() + "_1";
        redisTemplate.delete(key);

        return R.success("添加成功");
    }

    /**
     * 分页菜品查询
     */
    @GetMapping("/page")
    public R<Page<DishDto>> page(int page, int pageSize, String name) {
        //分页构造器
        Page<Dish> pageInfo = new Page<>(page, pageSize);
        //条件构造器
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.like(name != null, Dish::getName, name);
        queryWrapper.orderByDesc(Dish::getUpdateTime);

        dishService.page(pageInfo, queryWrapper);
        //================================================================
        //对象拷贝,排除records(数据内容)
        Page<DishDto> dishDtoPage = new Page<>();
        BeanUtils.copyProperties(pageInfo, dishDtoPage, "records");

        List<Dish> records = pageInfo.getRecords();
        List<DishDto> list = records.stream().map(item -> {
            DishDto dishDto = new DishDto();
            BeanUtils.copyProperties(item, dishDto);

            Long categoryId = item.getCategoryId();
            //根据id查询分类对象
            Category category = categoryService.getById(categoryId);

            if (category != null) {
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);
            }
            return dishDto;
        }).collect(Collectors.toList());

        dishDtoPage.setRecords(list);

        return R.success(dishDtoPage);
    }

    /**
     * 根据id返回修改数据
     */
    @GetMapping("/{id}")
    public R<DishDto> getById(@PathVariable Long id) {
        DishDto dishDto = dishService.getByIdWithFlavor(id);
        return R.success(dishDto);
    }

    /**
     * 更新菜品
     */
    @PutMapping
    public R<String> update(@RequestBody DishDto dishDto) {
        dishService.updateWithFlavor(dishDto);
        //清理redis缓存
        String key = "dish_" + dishDto.getCategoryId() + "_1";
        redisTemplate.delete(key);

        return R.success("修改成功");
    }

    /**
     * 根据条件查询对应菜品
     *
     * @param dish 菜品信息
     * @return 处理过后的菜品信息
     */
    @GetMapping("/list")
    public R<List<DishDto>> list(Dish dish) {
        List<DishDto> dtoList;
        //构造redis key
        String key = "dish_" + dish.getCategoryId() + "_" + dish.getStatus();
        //根据key查询
        dtoList = (List<DishDto>) redisTemplate.opsForValue().get(key);

        //如果存在，直接返回
        if (dtoList != null) {
            return R.success(dtoList);
        }
        //如果不存在，查询数据库，
        //条件构造器
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        //查询状态为启售状态的
        queryWrapper.eq(Dish::getStatus, 1);
        queryWrapper.eq(dish.getCategoryId() != null, Dish::getCategoryId, dish.getCategoryId());
        //排序
        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);
        List<Dish> list = dishService.list(queryWrapper);

        dtoList = list.stream().map(item -> {
            DishDto dishDto = new DishDto();

            BeanUtils.copyProperties(item, dishDto);

            Long categoryId = item.getCategoryId();
            //根据id查询分类对象
            Category category = categoryService.getById(categoryId);

            if (category != null) {
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);
            }
            //菜品id
            Long dishId = item.getId();
            LambdaQueryWrapper<DishFlavor> dtoLambdaQueryWrapper = new LambdaQueryWrapper<>();
            dtoLambdaQueryWrapper.eq(DishFlavor::getDishId, dishId);
            //SQL:select * from dish_flavor where dish_id =?
            List<DishFlavor> dishFlavorList = dishFlavorService.list(dtoLambdaQueryWrapper);

            dishDto.setFlavors(dishFlavorList);
            return dishDto;
        }).collect(Collectors.toList());

        //将数据缓存到redis
        redisTemplate.opsForValue().set(key, dtoList, 60, TimeUnit.MINUTES);

        return R.success(dtoList);
    }

    /**
     * 根据id删除菜品
     *
     * @param ids 菜品id
     * @return 删除状态
     */
    @DeleteMapping
    public R<String> removeByIds(@RequestParam List<Long> ids) {
        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Dish::getId, ids);
        wrapper.eq(Dish::getStatus, 1);
        long count = dishService.count(wrapper);
        if (count > 0) {
            throw new CustomException("有菜品正在售卖中，不能删除");
        }
        //批量删除
        dishService.removeByIds(ids);
        return R.success("删除成功");
    }

    /**
     * 修改菜品启售状态
     *
     * @param flag 启售/停售
     * @param ids  菜品id
     * @return 修改成功或失败
     */
    @PostMapping("/status/{flag}")
    public R<String> changeStatus(@PathVariable Integer flag, Long[] ids) {
        List<Dish> dishList = new ArrayList<>();
        for (Long id : ids) {
            Dish dish = dishService.getById(id);
            //清除redis缓存
            String key = "dish_" + dish.getCategoryId() + "_1";
            redisTemplate.delete(key);
            if (flag == 0) {
                //停售操作
                dish.setStatus(0);
            } else {
                //起售操作
                dish.setStatus(1);
            }
            dishList.add(dish);
        }
        dishService.updateBatchById(dishList);
        return R.success("修改成功");
    }
}
