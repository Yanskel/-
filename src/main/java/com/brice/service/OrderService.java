package com.brice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.brice.entity.Orders;

public interface OrderService extends IService<Orders> {
    /**
     * 去支付
     *
     * @param orders
     */
    public void submit(Orders orders);
}
