package com.brice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.brice.entity.ShoppingCart;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ShoppingCartMapper extends BaseMapper<ShoppingCart> {
}
