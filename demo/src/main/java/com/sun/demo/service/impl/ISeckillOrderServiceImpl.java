package com.sun.demo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sun.demo.mapper.SeckillGoodsMapper;
import com.sun.demo.mapper.SeckillOrderMapper;
import com.sun.demo.pojo.SeckillGoods;
import com.sun.demo.pojo.SeckillOrder;
import com.sun.demo.pojo.User;
import com.sun.demo.service.ISeckillGoodsService;
import com.sun.demo.service.ISeckillOrderService;
import org.springframework.stereotype.Service;

@Service
public class ISeckillOrderServiceImpl extends ServiceImpl<SeckillOrderMapper, SeckillOrder> implements ISeckillOrderService {


    @Override
    public Long getResult(User user, Long goodsId) {
        return null;
    }
}

