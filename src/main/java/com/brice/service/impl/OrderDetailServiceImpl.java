package com.brice.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.brice.entity.OrderDetail;
import com.brice.mapper.OrderDetailMapper;
import com.brice.service.OrderDetailService;
import org.springframework.stereotype.Service;

@Service
public class OrderDetailServiceImpl extends ServiceImpl<OrderDetailMapper, OrderDetail> implements OrderDetailService {
}
