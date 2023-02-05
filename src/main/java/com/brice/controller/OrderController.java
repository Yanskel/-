package com.brice.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.brice.common.BaseContext;
import com.brice.common.R;
import com.brice.dto.OrdersDto;
import com.brice.entity.OrderDetail;
import com.brice.entity.Orders;
import com.brice.service.OrderDetailService;
import com.brice.service.OrderService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/order")
public class OrderController {
    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderDetailService orderDetailService;

    /**
     * 下单
     *
     * @param orders 订单数据
     * @return 状态
     */
    @PostMapping("/submit")
    public R<String> submit(@RequestBody Orders orders) {
        orderService.submit(orders);
        return R.success("下单成功");
    }

    /**
     * 订单查询
     *
     * @param page      当前页码
     * @param pageSize  每页数据量
     * @param number    订单号
     * @param beginTime 起始时间
     * @param endTime   结束时间
     * @return 分页查询的数据
     */
    @GetMapping("/page")
    public R<Page<Orders>> page(int page, int pageSize, String number, String beginTime, String endTime) {
        //构造分页构造器
        Page<Orders> pageInfo = new Page<>(page, pageSize);
        //构造条件构造器
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        //根据number进行模糊查询
        wrapper.like(StringUtils.isNotEmpty(number), Orders::getNumber, number);
        //时间限制查询
        if (beginTime != null && endTime != null) {
            wrapper.gt(Orders::getOrderTime, beginTime);
            wrapper.lt(Orders::getOrderTime, endTime);
        }
        //根据时间排序
        wrapper.orderByDesc(Orders::getOrderTime);
        //分页查询
        orderService.page(pageInfo, wrapper);

//        //对象拷贝(分页数据)
//        BeanUtils.copyProperties(pageInfo, ordersDtoPage, "records");
//
//        List<Orders> records = pageInfo.getRecords();
//
//        List<OrdersDto> list = records.stream().map(order -> {
//            OrdersDto ordersDto = new OrdersDto();
//            //对象拷贝(内容)
//            BeanUtils.copyProperties(order, ordersDto);
//            ordersDto.setUserName(order.getConsignee());
//            return ordersDto;
//        }).collect(Collectors.toList());
//
//        ordersDtoPage.setRecords(list);
        return R.success(pageInfo);
    }

    /**
     * 客户端订单查询
     *
     * @param page     当前页码
     * @param pageSize 每页数据量
     * @return 分页查询的数据
     */
    @GetMapping("/userPage")
    public R<Page<OrdersDto>> userPage(int page, int pageSize) {
        //构造分页构造器
        Page<Orders> pageInfo = new Page<>(page, pageSize);

        Page<OrdersDto> ordersDtoPage = new Page<>();
        //条件构造器
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        //查询当前用户的订单
        queryWrapper.eq(Orders::getUserId, BaseContext.getCurrentId());
        //根据时间排序
        queryWrapper.orderByDesc(Orders::getOrderTime);
        //分页查询
        orderService.page(pageInfo, queryWrapper);

        //对象拷贝(分页数据)
        BeanUtils.copyProperties(pageInfo, ordersDtoPage, "records");
        List<Orders> records = pageInfo.getRecords();
        List<OrdersDto> list = records.stream().map(order -> {
            OrdersDto ordersDto = new OrdersDto();
            //对象拷贝(内容)
            BeanUtils.copyProperties(order, ordersDto);
            Long orderId = order.getId();

            LambdaQueryWrapper<OrderDetail> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(OrderDetail::getOrderId, orderId);
            List<OrderDetail> orderDetailList = orderDetailService.list(wrapper);

            ordersDto.setOrderDetails(orderDetailList);

            return ordersDto;
        }).collect(Collectors.toList());

        ordersDtoPage.setRecords(list);
        return R.success(ordersDtoPage);
    }

    /**
     * 派送/完成订单
     *
     * @param orders 订单号
     * @return 成功与否
     */
    @PutMapping
    public R<String> distribute(@RequestBody Orders orders) {
        Integer status = orders.getStatus();
        //构建条件构造器
        LambdaUpdateWrapper<Orders> wrapper = new LambdaUpdateWrapper<>();
        wrapper.set(Orders::getStatus, status);
        wrapper.eq(Orders::getId, orders.getId());
        orderService.update(wrapper);
        return status == 3 ? R.success("订单已派送") : R.success("订单已完成");
    }

    /**
     * 再来一单
     *
     * @param orders 选中的订单号
     * @return 下单成功
     */
    @Transactional
    @PostMapping("/again")
    public R<String> again(@RequestBody Orders orders) {
        long orderId = orders.getId();
        //根据订单id查询订单
        Orders order = orderService.getById(orderId);
        //重新设置订单id
        long id = IdWorker.getId();
        order.setId(id);
        //重新设置订单号
        String number = String.valueOf(id);
        order.setNumber(number);
        //设置下单时间
        order.setOrderTime(LocalDateTime.now());
        order.setCheckoutTime(LocalDateTime.now());
        //设置配送状态
        order.setStatus(2);
        //插入一条一样的新数据
        orderService.save(order);

        //根据订单id查询订单明细表
        LambdaQueryWrapper<OrderDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderDetail::getOrderId, orderId);
        List<OrderDetail> list = orderDetailService.list(wrapper);
        list = list.stream().peek(orderDetail -> {
            //修改订单明细表id
            orderDetail.setId(IdWorker.getId());

            orderDetail.setOrderId(id);
        }).collect(Collectors.toList());

        //批量插入订单明细表
        orderDetailService.saveBatch(list);
        return R.success("下单成功");
    }
}
