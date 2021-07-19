package com.sun.demo.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Set;

/**
 * author sungw
 *
 * @description Zset应用场景
 * @date 2021/6/29
 */
public class RedisZSetUtil {

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 1,排行榜
     */
    public void rankingList(){
        //新闻阅读排行榜，点击一次加1
        redisTemplate.opsForZSet().incrementScore("hotNews","新闻id",1);
        //展示排行榜前十的新闻,zset是从小打到排列的，reverseRange倒叙获取前十的数据
        Set set = redisTemplate.opsForZSet().reverseRange("hotNews",0,10);

    }
}
