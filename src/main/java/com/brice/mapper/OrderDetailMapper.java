package com.brice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.brice.entity.OrderDetail;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderDetailMapper extends BaseMapper<OrderDetail> {
}
