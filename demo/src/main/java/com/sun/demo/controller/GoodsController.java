package com.sun.demo.controller;


import com.sun.demo.pojo.User;
import com.sun.demo.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.thymeleaf.spring5.view.ThymeleafViewResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 商品
 * 乐字节：专注线上IT培训
 * 答疑老师微信：lezijie
 *
 * @author zhoubin
 * @since 1.0.0
 */
@Controller
@RequestMapping("/goods")
public class GoodsController {

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private IUserService userService;

    /**
     * 功能描述: 跳转商品列表页
     * windows优化前QPS：1332
     * 缓存QPS：2342
     *
     * @param:
     * @return: 乐字节：专注线上IT培训
     * 答疑老师微信：lezijie
     * @since: 1.0.0
     * @Author: zhoubin
     */
//    @RequestMapping(value = "/toList")
////    @ResponseBody
//    public String toList(Model model, User user,
//                         HttpServletRequest request, HttpServletResponse response,
//                         @CookieValue("userTicket") String userTicket) {
////        if (StringUtils.isEmpty(user)) {
////            return "/login";
////        }
////        user = userService.getUserByCookie(userTicket, request, response);
////        if (null == user) {
////            return "/login";
////        }
////        model.addAttribute("user", user);
//        return "goodsList";
//    }
    @RequestMapping(value = "/toList")
//    @ResponseBody
    public String toList(Model model, User user) {
//        if (StringUtils.isEmpty(user)) {
//            return "/login";
//        }
//        user = userService.getUserByCookie(userTicket, request, response);
//        if (null == user) {
//            return "/login";
//        }
        model.addAttribute("goodsList", goodsService.findGoodsVo());
        model.addAttribute("user", user);
        return "goodsList";
    }

}