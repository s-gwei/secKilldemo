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

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public RespBean doLogin(LoginVo loginVo, HttpServletRequest request, HttpServletResponse response) {
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
        User user = userMapper.selectById(mobile);
        if (null == user) {
            throw new GlobalException(RespBeanEnum.LOGIN_ERROR);
        }
        //判断密码是否正确
        if (!MD5Util.formPassToDBPass(password, user.getSlat()).equals(user.getPassword())) {
            throw new GlobalException(RespBeanEnum.LOGIN_ERROR);
        }
        //生成cookie
        String ticket = UUIDUtil.uuid();
        //将用户信息存入redis中
        redisTemplate.opsForValue().set("user:" + ticket, user);
        // request.getSession().setAttribute(ticket,user);
        CookieUtil.setCookie(request, response, "userTicket", ticket);
        return RespBean.success(ticket);
//        return RespBean.success();
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
