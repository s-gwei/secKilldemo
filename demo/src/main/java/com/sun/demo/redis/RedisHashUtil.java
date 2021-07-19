package com.sun.demo.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * author sungw
 *
 * @description hash的使用场景
 * @date 2021/6/29
 */
public class RedisHashUtil {


    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 1.获取修改表的某个字段,而不用获取整个user
     */
    public  void put(){
        Map map = new HashMap();
        redisTemplate.opsForHash().put("user1","name","zhangsan");
        redisTemplate.opsForHash().put("user1","age","22");
        redisTemplate.opsForHash().put("user1","sex","nv");
        redisTemplate.opsForHash().putAll("user2",map);
        //获取整个user
         map = redisTemplate.opsForHash().entries("user1");
        //获取user的年龄
        String age = (String) redisTemplate.opsForHash().get("user1","age");
    }
    /**
     * 2,购物车数据
     *
     * 以用户id为key
     * 商品id为field
     * 商品数量为value
     *
     */
    public void shopCar(){
        //添加购物车
        redisTemplate.opsForHash().put("userId","goodId",1);
        //增加数量
        redisTemplate.opsForHash().increment("userId","goodId",1);
        //商品总数
        long a = redisTemplate.opsForHash().size("userId");
        //删除商品
        redisTemplate.opsForHash().delete("userId","goodId");
        //获取购物车中所有商品
        Map map = redisTemplate.opsForHash().entries("userId");
    }

}
