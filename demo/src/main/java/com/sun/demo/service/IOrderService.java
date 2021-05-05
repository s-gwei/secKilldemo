package com.sun.demo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sun.demo.pojo.Order;
import com.sun.demo.pojo.User;
import com.sun.demo.vo.GoodsVo;

public interface IOrderService extends IService<Order> {

    Order seckill(User user, GoodsVo goods);
}
