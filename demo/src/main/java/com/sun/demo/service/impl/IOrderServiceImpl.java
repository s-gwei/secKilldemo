package com.sun.demo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@Slf4j
public class IOrderServiceImpl extends ServiceImpl<IOrderMapper, Order> implements IOrderService {

    @Autowired
    ISeckillGoodsService seckillGoodsService;

    @Autowired
    IOrderMapper orderMapper;

    @Autowired
    ISeckillOrderService seckillOrderService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private IGoodsService goodsService;
    /**
     * 秒杀
     * @param user
     * @param goods
     * @return
     */
    @Transactional
    @Override
    //
    public synchronized Order seckill(User user, GoodsVo goods) {

        //秒杀商品减库存
        SeckillGoods seckillGoods =  seckillGoodsService.getOne(new QueryWrapper<SeckillGoods>()
                .eq("goods_id",goods.getId()));
        seckillGoods.setStockCount(seckillGoods.getStockCount()-1);

        boolean result = seckillGoodsService.update(new UpdateWrapper<SeckillGoods>()
                .setSql("stock_count="+ "stock_count - 1")
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
        //保存抢到商品的用户
        redisTemplate.opsForValue().set("order:"+user.getId()+":"+goods.getId(),seckillOrder);
        return order;
    }



    public void createOrderByMq(Long goodsId, Long userId) {
        //校验库存（不要学我在trycatch中做逻辑处理，这样是不优雅的。这里这样处理是为了兼容之前的秒杀系统文章）
        //1，库存商品是否足够
        SeckillGoods seckillGoods = seckillGoodsService.getOne(new QueryWrapper<SeckillGoods>()
                .eq("goods_id", goodsId));
        if (seckillGoods.getStockCount() - seckillGoods.getCount() < 1) {
            log.info("库存不足");
            return;
        }
        //2,判断用户是否已购买
        if (!userIsBuy(userId, goodsId)) {
            log.info("该用户已购买，请勿重复购买");
            return;
        }
        //3,如果版本号一致，则可以修改秒杀库存
        boolean result = seckillGoodsService.update(new UpdateWrapper<SeckillGoods>()
                .setSql("version=" + (seckillGoods.getVersion() + 1))
                .setSql("count=" + (seckillGoods.getCount() + 1))
                .eq("goods_id", seckillGoods.getGoodsId())
                .eq("version", seckillGoods.getVersion()));
        if (!result) {
            log.info("秒杀失败");
            return;
        }
        //4,保存抢到商品的用户
        redisTemplate.opsForValue().set("order:" + userId + ":" + goodsId, goodsId);
        //5,生成订单
        log.info("写入订单至数据库");
        GoodsVo goods = goodsService.findGoodsVoByGoodsId(goodsId);
        Order order = new Order();
        order.setUserId(userId);
        order.setGoodsId(goods.getId());
        order.setDeliveryAddrId(0L);
        order.setGoodsName(goods.getGoodsName());
        order.setGoodsCount(1);
        order.setGoodsPrice(goods.getSeckillPrice());
        order.setOrderChannel(1);
        order.setStatus(0);
        order.setCreateDate(new Date());
        orderMapper.insert(order);
        //生成秒杀订单
        SeckillOrder seckillOrder = new SeckillOrder();
        seckillOrder.setUserId(userId);
        seckillOrder.setOrderId(order.getId());
        seckillOrder.setGoodsId(goods.getId());
        seckillOrderService.save(seckillOrder);
        log.info("下单完成");

    }

    private boolean userIsBuy(Long userId, Long goodsId) {
        Integer goodsIds = (Integer) redisTemplate.opsForValue().get("order:" +userId + ":" + goodsId);
        if (goodsIds != null) {
            return false;
        }
        return true;
    }


}
