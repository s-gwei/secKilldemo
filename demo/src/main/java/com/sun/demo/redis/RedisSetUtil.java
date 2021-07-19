package com.sun.demo.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * author sungw
 *Set使用场景
 * @description
 * @date 2021/6/29
 */
public class RedisSetUtil {


    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 1.微信小程序抽奖
     */
    public void luckDraw(){
        //加入抽奖集合
        redisTemplate.opsForSet().add("luckDraw","张三");
        redisTemplate.opsForSet().add("luckDraw","李四");
        //查看所有参与抽奖的用户
        Set set = redisTemplate.opsForSet().members("luckDraw");
        //抽取n名中奖者,中奖者不踢出set
        List list = redisTemplate.opsForSet().randomMembers("luckDraw",2);
        //抽取一等奖2名中奖者,中奖者踢出set
        List list1  = redisTemplate.opsForSet().pop("luckDraw",2);

    }
    /**
     * 2，微信微博点赞，收藏，标签
     */
    public void like(){
        //点赞，张三，李四给消息1点赞
        redisTemplate.opsForSet().add("消息id","张三");
        redisTemplate.opsForSet().add("消息id","李四");
        //给消息1点赞总数
        long  a =  redisTemplate.opsForSet().size("消息id");
        //李四取消点赞
        redisTemplate.opsForSet().remove("消息id","李四");
        //检查张三是否给消息1点赞
        boolean flag = redisTemplate.opsForSet().isMember("消息id","张三");
        //获取点赞列表
        Set set  = redisTemplate.opsForSet().members("消息id");

    }
    /**
     * 3，交友软件，用户之间相互关注模型
     */
    public void follow(){
        //张三关注了1，2，3，4
        redisTemplate.opsForSet().add("张三","1","2","3","4");
        //李四关注了1，2，3，4，5
        redisTemplate.opsForSet().add("李四","1","2","3","4","5");
        //王五关注了2，4，5，6，7
        redisTemplate.opsForSet().add("王五","2","4","5","6","7");

        //张三和李四共同关注的人,,intersect求两个set交集
        Set set = redisTemplate.opsForSet().intersect("张三","李四");
        //并集
        Set set1 =redisTemplate.opsForSet().union("张三","李四");


        //找到张三关注的人
        Set set3 = redisTemplate.opsForSet().members("张三");
        Set set4 = new HashSet();
        //获取张三关注的人，关注的人，展示在软件上
        //也就是我关注的人，也关注了它
        for(Object obj : set3){
            set4.addAll(redisTemplate.opsForSet().members(obj.toString()));
        }

        //可能认识的人
        Set set2 = redisTemplate.opsForSet().difference("张三","李四");

    }
}
