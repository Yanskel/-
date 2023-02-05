package com.brice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.brice.dto.DishDto;
import com.brice.entity.Dish;

public interface DishService extends IService<Dish> {
    void saveWithFlavor(DishDto dishDto);

    DishDto getByIdWithFlavor(Long id);

    void updateWithFlavor(DishDto dishDto);
}
