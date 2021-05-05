package com.sun.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sun.demo.mapper.IOrderMapper;
import com.sun.demo.mapper.SeckillGoodsMapper;
import com.sun.demo.pojo.Order;
import com.sun.demo.pojo.SeckillGoods;
import com.sun.demo.pojo.SeckillOrder;
import com.sun.demo.pojo.User;
import com.sun.demo.service.IOrderService;
import com.sun.demo.service.ISeckillGoodsService;
import com.sun.demo.service.ISeckillOrderService;
import com.sun.demo.vo.GoodsVo;
import com.sun.demo.vo.RespBeanEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
public class IOrderServiceImpl extends ServiceImpl<IOrderMapper, Order> implements IOrderService {

    @Autowired
    ISeckillGoodsService seckillGoodsService;

    @Autowired
    IOrderMapper orderMapper;

    @Autowired
    ISeckillOrderService seckillOrderService;

    @Autowired
    private RedisTemplate redisTemplate;
    /**
     * 秒杀
     * @param user
     * @param goods
     * @return
     */
    @Transactional
    @Override
    public Order seckill(User user, GoodsVo goods) {

        //秒杀商品减库存
        SeckillGoods seckillGoods =  seckillGoodsService.getOne(new QueryWrapper<SeckillGoods>().eq("goods_id",goods.getId()));
        seckillGoods.setStockCount(seckillGoods.getStockCount()-1);
        boolean result = seckillGoodsService.update(new UpdateWrapper<SeckillGoods>().setSql("stock_count="+ "stock_count - 1")
                .eq("goods_id",goods.getId()).gt("stock_count",0));
        if(!result){
            return null;
        }
        //生成订单
        Order order = new Order();
        order.setUserId(user.getId());
        order.setGoodsId(goods.getId());
        order.setDeliveryAddrId(0L);
        order.setGoodsName(goods.getGoodsName());
        order.setGoodsCount(1);
        order.setGoodsPrice(seckillGoods.getSeckillPrice());
        order.setOrderChannel(1);
        order.setStatus(0);
        order.setCreateDate(new Date());
        orderMapper.insert(order);
        //生成秒杀订单
        SeckillOrder seckillOrder = new SeckillOrder();
        seckillOrder.setUserId(user.getId());
        seckillOrder.setOrderId(order.getId());
        seckillOrder.setGoodsId(goods.getId());
        seckillOrderService.save(seckillOrder);
        redisTemplate.opsForValue().set("order:"+user.getId()+":"+goods.getId(),seckillOrder);
        return order;


    }
}
