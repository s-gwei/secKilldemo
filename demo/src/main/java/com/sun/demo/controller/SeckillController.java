package com.sun.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.sun.demo.mapper.IOrderMapper;
import com.sun.demo.pojo.Order;
import com.sun.demo.pojo.SeckillGoods;
import com.sun.demo.pojo.SeckillOrder;
import com.sun.demo.pojo.User;
import com.sun.demo.service.IGoodsService;
import com.sun.demo.service.IOrderService;
import com.sun.demo.service.ISeckillGoodsService;
import com.sun.demo.service.ISeckillOrderService;
import com.sun.demo.vo.GoodsVo;
import com.sun.demo.vo.RespBeanEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Date;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 秒杀
 */
@Controller
@RequestMapping("seckill")
public class SeckillController {

    @Autowired
    private IGoodsService goodsService;

    @Autowired
    private ISeckillOrderService seckillOrderService;

    @Autowired
    private IOrderService orderService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    ISeckillGoodsService seckillGoodsService;

    @Autowired
    IOrderMapper orderMapper;


    /**
     * 去秒杀
     * windows qps:785
     */

    @RequestMapping("doSeckill")
    public String doSeckill(Model model, User user, Long goodsId) {
        //如果用户不存在，跳到登录页面

        if (user == null) {
            return "login";
        }
        model.addAttribute("user", user);
        //库存商品是否足够
        GoodsVo goods = goodsService.findGoodsVoByGoodsId(goodsId);
        if (goods.getStockCount() < 1) {
            model.addAttribute("errmsg", RespBeanEnum.EMPTY_STOCK.getMessage());
            return "secKillFail";
        }
        //判断用户是否已购买
//        SeckillOrder seckillOrder = seckillOrderService.getOne(new QueryWrapper<SeckillOrder>().eq("user_id",user.getId()).eq(
//                "goods_id",goodsId));
        SeckillOrder seckillOrder = (SeckillOrder) redisTemplate.opsForValue().get("order:" + user.getId() + ":" + goodsId);
        if (seckillOrder != null) {
            model.addAttribute("errmsg", RespBeanEnum.REPEATE_ERROR.getMessage());
            return "secKillFail";
        }
        //开始抢购
        Order order = orderService.seckill(user, goods);
        model.addAttribute("order", order);
        model.addAttribute("goods", goods);
        return "orderDetail";
    }

    public Order seckill(User user, GoodsVo goods) {

        //秒杀商品减库存
        SeckillGoods seckillGoods = seckillGoodsService.getOne(new QueryWrapper<SeckillGoods>().eq("goods_id", goods.getId()));
        seckillGoods.setStockCount(seckillGoods.getStockCount() - 1);
        boolean result = seckillGoodsService.update(new UpdateWrapper<SeckillGoods>().setSql("stock_count=" + "stock_count - 1")
                .eq("goods_id", goods.getId()).gt("stock_count", 0));
        if (!result) {
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
        redisTemplate.opsForValue().set("order:" + user.getId() + ":" + goods.getId(), seckillOrder);
        return order;


    }
}
