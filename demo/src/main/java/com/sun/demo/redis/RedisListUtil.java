package com.sun.demo.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * author sungw
 *
 * @description redis中list集合
 * @date 2021/6/29
 */
public class RedisListUtil {


    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 常用数据结构可以用list轻松实现
     * Stack(栈) = lpush+lpop 先进后出
     * Queue(队列) = lpush + rpop 先进先出
     * Blocking MQ(阻塞队列) = lpush + BRPOP
     * 如果有元素直接拿走，没有元素等着，等到有元素在拿走
     *
     */

    public void stack(){
        //栈,先进后出
        redisTemplate.opsForList().leftPush("stack",1);
        redisTemplate.opsForList().leftPush("stack",2);
        String stack = (String) redisTemplate.opsForList().leftPop("stack");
        System.out.println(stack);
        stack = (String) redisTemplate.opsForList().leftPop("stack");
        //队列，先进先出
        redisTemplate.opsForList().leftPush("queue",1);
        redisTemplate.opsForList().leftPush("queue",2);
        redisTemplate.opsForList().leftPush("queue",3);
        String queue = (String) redisTemplate.opsForList().rightPop("queue");
        System.out.println(queue);
        queue = (String) redisTemplate.opsForList().rightPop("queue");
        System.out.println(queue);
        //阻塞队列
        redisTemplate.opsForList().leftPush("Blocking",1);
        redisTemplate.opsForList().leftPush("Blocking",2);
        redisTemplate.opsForList().leftPush("Blocking",3);
        //获取左边第一个元素，如果没有等待三秒，超过等待时间没有则退出
        String block = (String) redisTemplate.opsForList().leftPop("Blocking",3, TimeUnit.SECONDS);
    }

    /**
     * 2,微信订阅号
     *
     *  公众号先给在线的关注者发消息，然后异步给非在线的发消息
     */
     public void wechatList(){
         //张三关注了两个公众号，公众号给它发消息
         String msg = "111";
         redisTemplate.opsForList().leftPush("张三",msg);
         msg = "222";
         redisTemplate.opsForList().leftPush("张三",msg);
         //在张三的信息列表就有两条数据
         //获取张三列表中的4条数据
         List list = redisTemplate.opsForList().range("张三",0,4);
     }


}
