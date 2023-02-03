package com.sun.demo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sun.demo.exception.GlobalException;
import com.sun.demo.mapper.UserMapper;
import com.sun.demo.pojo.User;
import com.sun.demo.service.IUserService;
import com.sun.demo.util.CookieUtil;
import com.sun.demo.util.MD5Util;
import com.sun.demo.util.UUIDUtil;
import com.sun.demo.vo.LoginVo;
import com.sun.demo.vo.RespBean;
import com.sun.demo.vo.RespBeanEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public int saveUserCount(Long userid) {
        //根据不同用户id生成调用次数的key
        String limitKey = "LIMIT" + "_" + userid;

        //获取redis中指定key的调用次数
        String limitNum = (String) redisTemplate.opsForValue().get(limitKey);
        int limit =-1;
        if (limitNum == null) {
            //第一次调用放入redis中设置为0
            redisTemplate.opsForValue().set(limitKey, "0", 1, TimeUnit.SECONDS);
//            redisTemplate.opsForValue().set(limitKey, "0");
        } else {
            //不是第一次调用每次+1
            limit = Integer.parseInt(limitNum) + 1;
            redisTemplate.opsForValue().set(limitKey, String.valueOf(limit), 1, TimeUnit.SECONDS);
//            redisTemplate.opsForValue().set(limitKey, String.valueOf(limit));
        }
        return limit;//返回调用次数
    }

    @Override
    public boolean getUserCount(Long userId) {
        //根据userid对应key获取调用次数
        String limitKey = "LIMIT"+ "_" + userId;
        //跟库用户调用次数的key获取redis中调用次数
        String limitNum = (String) redisTemplate.opsForValue().get(limitKey);
        if (limitNum == null) {
            //为空直接抛弃说明key出现异常
            log.error("该用户没有访问申请验证值记录，疑似异常");
            return true;
        }
        return Integer.parseInt(limitNum) > 3; //false代表没有超过 true代表超过
    }

    @Override
    public RespBean doLogin(LoginVo loginVo, HttpServletRequest request, HttpServletResponse response) {
        String ticket = CookieUtil.getCookieValue(request,"userTicket");
        User user = (User) redisTemplate.opsForValue().get("user:" + ticket);
        if(user != null){
            CookieUtil.setCookie(request, response, "userTicket", ticket);
            return RespBean.success(ticket);
        }else{
            String mobile = loginVo.getMobile();
            String password = loginVo.getPassword();
            // //参数校验
            // if (StringUtils.isEmpty(mobile)||StringUtils.isEmpty(password)){
            // 	return RespBean.error(RespBeanEnum.LOGIN_ERROR);
            // }
            // if (!ValidatorUtil.isMobile(mobile)){
            // 	return RespBean.error(RespBeanEnum.MOBILE_ERROR);
            // }
            //根据手机号获取用户
             user = userMapper.selectById(mobile);
            if (null == user) {
                throw new GlobalException(RespBeanEnum.LOGIN_ERROR);
            }
            //判断密码是否正确
            if (!MD5Util.formPassToDBPass(password, user.getSlat()).equals(user.getPassword())) {
                throw new GlobalException(RespBeanEnum.LOGIN_ERROR);
            }
            //生成cookie
             ticket = UUIDUtil.uuid();
            //将用户信息存入redis中
            redisTemplate.opsForValue().set("user:" + ticket, user, 30, TimeUnit.MINUTES);
            //将链接url放在redis
//            String md5 = getMd5(user.getId().toString(),1l);
            // request.getSession().setAttribute(ticket,user);
            CookieUtil.setCookie(request, response, "userTicket", ticket);
//            return RespBean.success(ticket+";"+md5);
            return RespBean.success(ticket);
        }
    }
    public String getMd5(String userid, Long id) {
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
            redisTemplate.opsForValue().set(userid + id, md5);
        } catch (Exception e) {
            e.printStackTrace();
            return "获取md5失败: " + e.getMessage();
        }
        return  md5;
    }

    @Override
    public User getUserByCookie(String userTicket, HttpServletRequest request, HttpServletResponse response) {
       if(StringUtils.isEmpty(userTicket)){
           return null;
       }
       User user = (User) redisTemplate.opsForValue().get("user:" +userTicket);
       if(user != null){
           CookieUtil.setCookie(request, response, "userTicket", userTicket);
       }
        return user;
    }
}
