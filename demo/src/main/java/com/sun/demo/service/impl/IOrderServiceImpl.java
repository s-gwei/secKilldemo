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
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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

    @Autowired
    private Redisson redisson;

    /**
     * 秒杀
     *
     * @param user
     * @param goods
     * @return
     */
    @Transactional
    @Override
    //
    public synchronized Order seckill(User user, GoodsVo goods) {

        //秒杀商品减库存
        SeckillGoods seckillGoods = seckillGoodsService.getOne(new QueryWrapper<SeckillGoods>()
                .eq("goods_id", goods.getId()));
        seckillGoods.setStockCount(seckillGoods.getStockCount() - 1);

        boolean result = seckillGoodsService.update(new UpdateWrapper<SeckillGoods>()
                .setSql("stock_count=" + "stock_count - 1")
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
        //保存抢到商品的用户
        redisTemplate.opsForValue().set("order:" + user.getId() + ":" + goods.getId(), seckillOrder);
        return order;
    }


    public void createOrderByMq(Long goodsId, Long userId) {
        //校验库存（不要学我在trycatch中做逻辑处理，这样是不优雅的。这里这样处理是为了兼容之前的秒杀系统文章）
        //1，库存商品是否足够，通过数据双写一致性，
        //先查询缓存
        Integer count = (Integer) redisTemplate.opsForValue().get(goodsId + "count");

        //count==null,说明缓存失效，或者被删除了,从数据库种获取，并写入缓存，
        SeckillGoods seckillGoods = null;
        if (count == null) {
            seckillGoods = seckillGoodsService.getOne(new QueryWrapper<SeckillGoods>()
                    .eq("goods_id", goodsId));
            if (seckillGoods.getStockCount() - seckillGoods.getCount() < 1) {
                log.info("库存不足");
                return;
            }
            //设置一分钟缓存过期
            redisTemplate.opsForValue().set(goodsId + "count", seckillGoods.getStockCount() - seckillGoods.getCount(), 1, TimeUnit.MINUTES);

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
        //并删除缓存
        redisTemplate.delete(goodsId + "count");
        if (!result) {
            log.info("秒杀失败");
            return;
        }
        //4,保存抢到商品的用户
        redisTemplate.opsForValue().set("order:" + userId + ":" + goodsId, goodsId);

        //5,生成订单
        log.info("写入订单至数据库");
        updateGoods(goodsId, userId);
        log.info("下单完成");

    }

    private void updateGoods(Long goodsId, Long userId) {
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
    }

    private boolean userIsBuy(Long userId, Long goodsId) {
        Integer goodsIds = (Integer) redisTemplate.opsForValue().get("order:" + userId + ":" + goodsId);
        if (goodsIds != null) {
            return false;
        }
        return true;
    }

    //分布式锁
    public void createOrderByMq1(Long goodsId, Long userId) {
        //1，tryfinally可以捕捉程序异常，但是程序挂掉之后，锁还是无法被获取。
        //尝试获取分布式锁,执行完毕删掉分布式锁
        try {
            boolean flag = redisTemplate.opsForValue().setIfAbsent(goodsId, userId);
            if (!flag) {
                log.info("抢购失败");
                return;
            }
            //todo
        } finally {
            //删除分布式锁
            redisTemplate.delete(goodsId);
        }

        //2,设置锁的过期时间，但是程序执行不一定是该时长。
        try {
            //设置10秒的锁过期时间，但是程序执行不一定是10秒，将锁设置值和设置过期时间两步变为原子操作
            boolean flag = redisTemplate.opsForValue().setIfAbsent(goodsId, userId, 10, TimeUnit.SECONDS);
            //boolean flag = redisTemplate.opsForValue().setIfAbsent(goodsId,userId);
            // redisTemplate.expire(goodsId,10,TimeUnit.SECONDS);
            if (!flag) {
                log.info("抢购失败");
                return;
            }
            //todo
        } finally {
            //删除分布式锁
            redisTemplate.delete(goodsId);
        }

        //3,设置过期时间，但是过期时间过短，代码没执行完，就将锁释放，
        // 导致第一个线程将第二个线程锁释放，第二个线程将第三个线程所释放
        //可能造成锁永久失效
        //生成唯一ID,
        
        String clientId = UUID.randomUUID().toString();
        try {
            //设置10秒的锁过期时间，但是程序执行不一定是10秒，将锁设置值和设置过期时间两步变为原子操作
            boolean flag = redisTemplate.opsForValue().setIfAbsent(goodsId, clientId, 10, TimeUnit.SECONDS);
            //boolean flag = redisTemplate.opsForValue().setIfAbsent(goodsId,userId);
            // redisTemplate.expire(goodsId,10,TimeUnit.SECONDS);
            if (!flag) {
                log.info("抢购失败");
                return;
            }
            //todo
        } finally {
            if (clientId.equals(redisTemplate.opsForValue().get(goodsId))) {
                //删除分布式锁
                redisTemplate.delete(goodsId);
            }
        }

        //3,设置过期时间，但是过期时间过短，代码没执行完，就将锁释放，
        // 导致第一个线程将第二个线程锁释放，第二个线程将第三个线程所释放
        //可能造成锁永久失效
        //通过开启分线程定时检测锁有没有过期
//       String clientId1 = UUID.randomUUID().toString();
        try {
            int time = 10;
            //设置10秒的锁过期时间，但是程序执行不一定是10秒，将锁设置值和设置过期时间两步变为原子操作
            boolean flag = redisTemplate.opsForValue().setIfAbsent(goodsId, clientId, time, TimeUnit.SECONDS);
            //开启分线程实时检测锁有没有没释放，如果没有被释放，给锁续命，续命时长为设定时长的三分之一
            new Thread(new Runnable() {
                @SneakyThrows
                @Override
                public void run() {
                    while (true) {
                        //如果分布式锁没有被释放，给锁续命
                        if (clientId.equals(redisTemplate.opsForValue().get(goodsId))) {
                            //给分布式锁续命
                            redisTemplate.opsForValue().setIfAbsent(goodsId, clientId, time / 3, TimeUnit.SECONDS);
                        }
                        //如果锁已经被释放，关闭子线程、
                        else {
                            break;
                        }
                        Thread.sleep(3);
                    }
                }
            });
            //boolean flag = redisTemplate.opsForValue().setIfAbsent(goodsId,userId);
            // redisTemplate.expire(goodsId,10,TimeUnit.SECONDS);
            if (!flag) {
                log.info("抢购失败");
                return;
            }
            //todo
        } finally {
            if (clientId.equals(redisTemplate.opsForValue().get(goodsId))) {
                //删除分布式锁
                redisTemplate.delete(goodsId);
            }
        }

    }


    //Redisson分布式锁
    public void createOrderByMq2(Long goodsId, Long userId) {
        //3,设置过期时间，但是过期时间过短，代码没执行完，就将锁释放，
        // 导致第一个线程将第二个线程锁释放，第二个线程将第三个线程所释放
        //可能造成锁永久失效
        //生成唯一ID,
        String clientId = UUID.randomUUID().toString();
        //设置10秒的锁过期时间，但是程序执行不一定是10秒，将锁设置值和设置过期时间两步变为原子操作
        RLock rLock = redisson.getLock(goodsId.toString());
        //设置超时时间，获取到锁的继续执行，没获取到锁的线程阻塞。
        rLock.lock(10,TimeUnit.SECONDS);
        try {
            //todo
        } finally {
            rLock.unlock();

        }


    }


}
