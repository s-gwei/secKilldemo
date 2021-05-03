package com.sun.demo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sun.demo.pojo.User;
import com.sun.demo.vo.LoginVo;
import com.sun.demo.vo.RespBean;
import org.springframework.stereotype.Repository;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public interface IUserService extends IService<User> {
    RespBean doLogin(LoginVo loginVo, HttpServletRequest request, HttpServletResponse response);

    /**
     * 功能描述: 根据cookie获取用户
     *
     * @param:
     * @return: 乐字节：专注线上IT培训
     * 答疑老师微信：lezijie
     * @since: 1.0.0
     * @Author: zhoubin
     */
    User getUserByCookie(String userTicket, HttpServletRequest request, HttpServletResponse response);

}
