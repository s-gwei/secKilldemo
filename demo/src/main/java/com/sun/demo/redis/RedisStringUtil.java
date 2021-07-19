package com.sun.demo.redis;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * author sungw
 *
 * @description redisString类型使用
 *
 * String类型使用场景
 *
 *
 * @date 2021/6/29
 */
@Component
public class RedisStringUtil {

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 1.缓存元素
     */
    public void put() {
        //存取元素
        redisTemplate.opsForValue().set("k1", "v1");
        redisTemplate.opsForValue().set("k1", "v2");
        String k1 = (String) redisTemplate.opsForValue().get("k1");
        //设置超时时间,10s后数据自动过期
        redisTemplate.opsForValue().set("k1", "v1", 10, TimeUnit.SECONDS);
        //删除元素,返回值true or false
        boolean flag = redisTemplate.delete("k1");

    }

    /**
     * 2，分布式锁
     * 设置过期时间，但是过期时间过短，代码没执行完，就将锁释放，
     * 导致第一个线程将第二个线程锁释放，第二个线程将第三个线程所释放
     * 可能造成锁永久失效
     * 通过开启分线程定时检测锁有没有过期
     */
    public void setnx() {
        String clientId1 = UUID.randomUUID().toString();
        try {
            int time = 10;
            //设置10秒的锁过期时间，但是程序执行不一定是10秒，将锁设置值和设置过期时间两步变为原子操作
            boolean flag = redisTemplate.opsForValue().setIfAbsent("k1", clientId1, time, TimeUnit.SECONDS);
            //开启分线程实时检测锁有没有没释放，如果没有被释放，给锁续命，续命时长为设定时长的三分之一
            new Thread(new Runnable() {
                @SneakyThrows
                @Override
                public void run() {
                    while (true) {
                        //如果分布式锁没有被释放，给锁续命
                        if (clientId1.equals(redisTemplate.opsForValue().get("k1"))) {
                            //给分布式锁续命
                            redisTemplate.opsForValue().setIfAbsent("k1", clientId1, time / 3, TimeUnit.SECONDS);
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
                System.out.println("抢购失败");
                return;
            }
            //todo
        } finally {
            if (clientId1.equals(redisTemplate.opsForValue().get("k1"))) {
                //删除分布式锁
                redisTemplate.delete("k1");
            }
        }
    }

    /**
     *3.计数器
     */
    public  void count(){
        redisTemplate.opsForValue().increment("loginCount",1);
    }
    /**
     * 4.分布式系统全局序列号
     * 原子加1，
     * 批量拿100个，在代码中加1，到100后再次拿
     */
    public  void orderId(){
       Long a =  redisTemplate.opsForValue().increment("loginCount",100);
    }

}
