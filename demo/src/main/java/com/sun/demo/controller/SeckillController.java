package com.sun.demo.controller;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.google.common.util.concurrent.RateLimiter;
import com.sun.demo.mapper.IOrderMapper;
import com.sun.demo.pojo.Order;
import com.sun.demo.pojo.SeckillGoods;
import com.sun.demo.pojo.SeckillOrder;
import com.sun.demo.pojo.User;
import com.sun.demo.service.*;
import com.sun.demo.util.CookieUtil;
import com.sun.demo.util.MD5Util;
import com.sun.demo.vo.GoodsVo;
import com.sun.demo.vo.RespBeanEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 秒杀
 */
@Controller
@RequestMapping("seckill")
@Slf4j
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

    @Autowired
    private IUserService userService;

    @Autowired
    private AmqpTemplate rabbitTemplate;

    //创建令牌桶实例
    //每秒1个令牌生成10令牌，从输出可看出很平滑，这种实现将突发请求速率平均成固定请求速率。
    private RateLimiter rateLimiter = RateLimiter.create(10);



    //开发一个秒杀方法 乐观锁防止超卖+ 令牌桶算法限流 + 单用户访问频率限制,
    //令牌桶算法限流 + 单用户访问频率限制，限制每个用户1s只能访问3次，实现单用户只能购买到一个秒杀商品
    @GetMapping("killtokenlimit")
    @ResponseBody
    public String killtokenlimit(Model model, User user, Long goodsId, String md5) {
        //验证请求是否合法
        boolean flag = checkMD5(user.getId()+""+goodsId, md5);
        if (!flag) {
//             throw  new RuntimeException("没有携带验证签名,请求不合法!");
//             throw  new RuntimeException("当前请求数据不合法,请稍后再试!");
            return "secKillFail";
        }
        //加入令牌桶的限流措施
        if (!rateLimiter.tryAcquire(3, TimeUnit.SECONDS)) {
            log.info("抛弃请求: 抢购失败,当前秒杀活动过于火爆,请重试");
            return "抢购失败,当前秒杀活动过于火爆,请重试!";
        }
        try {
            //单用户调用调用接口的频率限制
            Integer count = userService.saveUserCount(user.getId());
            log.info("用户截至该次的访问次数为: [{}]", count);
            //进行调用次数判断
            boolean isBanned = userService.getUserCount(user.getId());
            if (isBanned) {
                log.info("购买失败,超过频率限制!");
                return "购买失败，超过频率限制!";
            }
            //根据秒杀商品id 去调用秒杀业务
            String orderId = doSeckillTest(model, user, goodsId);
            return "秒杀成功,订单id为: " + String.valueOf(orderId);
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }




    @GetMapping("sale")
    @ResponseBody
    public void sale() {
        //1.没有获取到token请求一直知道获取到token 令牌
        System.out.println("等待的时间: " + rateLimiter.acquire());
        //2.设置一个等待时间,如果在等待的时间内获取到了token 令牌,则处理业务,如果在等待时间内没有获取到响应token则抛弃
        if (!rateLimiter.tryAcquire(2, TimeUnit.SECONDS)) {
            System.out.println("当前请求被限流,直接抛弃,无法调用后续秒杀逻辑....");
        }
        System.out.println("处理业务.....................");
    }


    /**
     * 去秒杀
     * windows qps:785
     * 使用悲观锁解决超卖问题
     */
    //开发一个秒杀方法 乐观锁防止超卖+ 令s牌桶算法限流
    @RequestMapping("skills")
    public String killtoken(Model model, User user, Long goodsId) {
        //加入令牌桶的限流措施
        if (!rateLimiter.tryAcquire(1, TimeUnit.SECONDS)) {
            log.info("抛弃请求: 抢购失败,当前秒杀活动过于火爆,请重试");
            return "secKillFail";
        }
        try {
            //1，库存商品是否足够
            SeckillGoods seckillGoods = seckillGoodsService.getOne(new QueryWrapper<SeckillGoods>()
                    .eq("goods_id", goodsId));
            if (seckillGoods.getStockCount() - seckillGoods.getCount() < 1) {
                model.addAttribute("errmsg", RespBeanEnum.EMPTY_STOCK.getMessage());
                return "secKillFail";
            }
            //2,判断用户是否已购买
            if (!userIsBuy(model, user, goodsId)) {
                return "secKillFail";
            }
            //3,如果版本号一致，则可以修改秒杀库存
            boolean result = seckillGoodsService.update(new UpdateWrapper<SeckillGoods>()
                    .setSql("version=" + (seckillGoods.getVersion() + 1))
                    .setSql("count=" + (seckillGoods.getCount() + 1))
                    .eq("goods_id", seckillGoods.getGoodsId())
                    .eq("version", seckillGoods.getVersion()));
//        boolean result = seckillGoodsService.updateCount(seckillGoods);
            if (!result) {
                return "secKillFail";
            }
            //4,保存抢到商品的用户
            redisTemplate.opsForValue().set("order:" + user.getId() + ":" + goodsId, goodsId);
            //5,创建订单和秒杀订单
            Order order = skillOrder(user, goodsId);
            model.addAttribute("order", order);
            model.addAttribute("seckillGoods", seckillGoods);
            log.info("秒杀成功");
            return "orderDetail";
        } catch (Exception e) {
            e.printStackTrace();
            return "secKillFail";
        }
    }

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
        Order order = null;
        //synchronized不能放在@Transactional方法内
        synchronized (this) {
            SeckillOrder seckillOrder = (SeckillOrder) redisTemplate.opsForValue().get("order:" + user.getId() + ":" + goodsId);
            if (seckillOrder != null) {
                model.addAttribute("errmsg", RespBeanEnum.REPEATE_ERROR.getMessage());
                return "secKillFail";
            }
            //开始抢购
            order = orderService.seckill(user, goods);
        }

        model.addAttribute("order", order);
        model.addAttribute("goods", goods);
        return "orderDetail";
    }

    /**
     * 解决超卖和一人买多个商品问题
     * windows qps:785
     * <p>
     * 使用乐观锁解决商品超卖问题,但是无法解决一个用户只能购买一件秒杀商品的问题
     * 使用ReentrantLock
     */

    @RequestMapping("doSeckill1")
    public String doSeckillTest(Model model, User user, Long goodsId) {

        //1，库存商品是否足够
        SeckillGoods seckillGoods = seckillGoodsService.getOne(new QueryWrapper<SeckillGoods>()
                .eq("goods_id", goodsId));
        if (seckillGoods.getStockCount() - seckillGoods.getCount() < 1) {
            model.addAttribute("errmsg", RespBeanEnum.EMPTY_STOCK.getMessage());
            return "secKillFail";
        }
        //2,判断用户是否已购买
        if (!userIsBuy(model, user, goodsId)) {
            return "secKillFail";
        }
//        ReentrantLock lock = new ReentrantLock();
//        try{
//            lock.lock();
        //3,如果版本号一致，则可以修改秒杀库存
        boolean result = seckillGoodsService.update(new UpdateWrapper<SeckillGoods>()
                .setSql("version=" + (seckillGoods.getVersion() + 1))
                .setSql("count=" + (seckillGoods.getCount() + 1))
                .eq("goods_id", seckillGoods.getGoodsId())
                .eq("version", seckillGoods.getVersion()));
//        boolean result = seckillGoodsService.updateCount(seckillGoods);
        if (!result) {
            return "secKillFail";
        }
        //4,保存抢到商品的用户
        redisTemplate.opsForValue().set("order:" + user.getId() + ":" + goodsId, goodsId);
//        }finally {
//            lock.unlock();
//        }
        //5,创建订单和秒杀订单
        Order order = skillOrder(user, goodsId);
        model.addAttribute("order", order);
        model.addAttribute("seckillGoods", seckillGoods);
        return "orderDetail";
    }

    private Order skillOrder(User user, Long goodsId) {
        //生成订单
        GoodsVo goods = goodsService.findGoodsVoByGoodsId(goodsId);
        Order order = new Order();
        order.setUserId(user.getId());
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
        seckillOrder.setUserId(user.getId());
        seckillOrder.setOrderId(order.getId());
        seckillOrder.setGoodsId(goods.getId());
        seckillOrderService.save(seckillOrder);

        return order;
    }

    private boolean userIsBuy(Model model, User user, Long goodsId) {
        Integer goodsIds = (Integer) redisTemplate.opsForValue().get("order:" + user.getId() + ":" + goodsId);
        if (goodsIds != null) {
            model.addAttribute("errmsg", RespBeanEnum.REPEATE_ERROR.getMessage());
            return false;
        }
        return true;
    }
















    //生成md5值的方法，隐藏请求url,同时判断是否到抢购时间
    @RequestMapping("md5")
    public String getMd5(String userid, Long id) {
        //判断是否到抢购时间
        String time = (String) redisTemplate.opsForValue().get(id+"time");
        Long currentTime = System.currentTimeMillis();
        if(currentTime < Long.parseLong(time)){
            return null;
        }
        //生成动态的md5，隐藏抢购接口
        String md5;
        try {
            Random r = new Random();
            StringBuilder sb = new StringBuilder(16);
            sb.append(r.nextInt(99999999)).append(r.nextInt(99999999));
            int len = sb.length();
            if (len < 16) {
                for (int i = 0; i < 16 - len; i++) {
                    sb.append("0");
                }
            }
            String salt = sb.toString();
            md5 = MD5Util.formPassToDBPass(userid, salt);
//            redisTemplate.opsForValue().set(userid + id, md5, 3, TimeUnit.SECONDS);
            //设置MD5存活时间，如果在一小时内用户没有抢购，则需要重新生成动态url
            redisTemplate.opsForValue().set(userid + id, md5,60, TimeUnit.MINUTES);
        } catch (Exception e) {
            e.printStackTrace();
            return "获取md5失败: " + e.getMessage();
        }
        return "获取md5信息为: " + md5;
    }
    /**
     * 下单接口：异步处理订单
     *
     * @param goodsId
     * @return
     */
    @RequestMapping(value = "/createOrderWithMq/{md5}", method = {RequestMethod.GET})
    @ResponseBody
    public String createOrderWithMq(Long goodsId, User user,
                                    Model model, String md5) {

        //1，验证请求是否合法
//        String s = getMd5(user.getId().toString(),goodsId);
        boolean flag = checkMD5(user.getId()+""+goodsId, md5);
        if (!flag) {
//             throw  new RuntimeException("没有携带验证签名,请求不合法!");
//             throw  new RuntimeException("当前请求数据不合法,请稍后再试!");
            log.info("当前请求数据不合法,请稍后再试!");
            return "secKillFail";
        }
        //2.单用户调用调用接口的频率限制
        //单用户，每秒最多请求三次
        Integer count = userService.saveUserCount(user.getId());
        log.info("用户截至该次的访问次数为: [{}]", count);
        //进行调用次数判断
        boolean isBanned = userService.getUserCount(user.getId());
        if (isBanned) {
            log.info("购买失败,超过频率限制!");
            return "购买失败，超过频率限制!";
        }
        //3,使用令牌桶限流
        if (!rateLimiter.tryAcquire(3, TimeUnit.SECONDS)) {
            log.info("抛弃请求: 抢购失败,当前秒杀活动过于火爆,请重试");
            return "抢购失败,当前秒杀活动过于火爆,请重试!";
        }
        //4，库存商品是否足够
        Integer count1 = (Integer) redisTemplate.opsForValue().get(goodsId + "count");

        if(count1 == null){
            SeckillGoods seckillGoods = seckillGoodsService.getOne(new QueryWrapper<SeckillGoods>()
                    .eq("goods_id", goodsId));
            if (seckillGoods.getStockCount() - seckillGoods.getCount() < 1) {
                log.info("商品库存已抢光");
                return "商品库存已抢光";
            }
            count1 = seckillGoods.getCount();
        }

        //5,判断用户是否已购买
        if (!userIsBuy(model, user, goodsId)) {
            log.info("您已购买，请勿重复下单");
            return "您已购买，请勿重复下单";
        }
        // 有库存，则将log用户id和商品id封装为消息体传给消息队列处理
        // 注意这里的有库存和已经下单都是缓存中的结论，存在不可靠性，在消息队列中会查表再次验证
        log.info("有库存：[{}]", count1);
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("goodsId", goodsId);
            jsonObject.put("userId", user.getId());
            sendToOrderQueue(jsonObject.toJSONString());
            return "秒杀请求提交成功";
        } catch (Exception e) {
            log.error("下单接口：异步处理订单异常：", e);
            return "秒杀请求失败，服务器正忙.....";
        }

    }

    /**
     * 检查请求是否合法
     * @param id
     * @param md5
     * @return
     */
    private boolean checkMD5(String id, String md5) {
        String s = (String) redisTemplate.opsForValue().get(id);
        if (s == null) {
            return false;
        }
        if (!s.equals(md5)) {
            return false;
        }
        return true;
    }

    /**
     * 向消息队列orderQueue发送消息
     *
     * @param message
     */
    private void sendToOrderQueue(String message) {
        log.info("这就去通知消息队列开始下单：[{}]", message);
        this.rabbitTemplate.convertAndSend("orderQueue", message);
    }
}
