package com.sun.demo.controller;


import com.sun.demo.pojo.User;
import com.sun.demo.service.IGoodsService;
import com.sun.demo.service.IUserService;
import com.sun.demo.vo.GoodsVo;
import com.sun.demo.vo.RespBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.thymeleaf.spring5.view.ThymeleafViewResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

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

    @Autowired
    private IGoodsService goodsService;

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

    /**
     * 	 * windows优化前QPS：1120
     * 	 linuxqps:207
     * @param model
     * @param user
     * @return
     */
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
    //获取商品详情
    @RequestMapping("/toDatail/{goodsId}")
//    @ResponseBody
    public String toDetail(Model model,User user, @PathVariable Long goodsId) {
        model.addAttribute("user", user);
        GoodsVo goodsVo = goodsService.findGoodsVoByGoodsId(goodsId);
        Date startDate = goodsVo.getStartDate();
        Date endDate = goodsVo.getEndDate();
        Date nowDate = new Date();
        //秒杀状态
        int secKillStatus = 0;
        //秒杀倒计时
        int remainSeconds = 0;
        //秒杀还未开始

        if (nowDate.before(startDate)) {
            remainSeconds = ((int) ((startDate.getTime() - nowDate.getTime()) / 1000));
        } else if (nowDate.after(endDate)) {
            //	秒杀已结束
            secKillStatus = 2;
            remainSeconds = -1;
        } else {
            //秒杀中
            secKillStatus = 1;
            remainSeconds = 0;
        }
        model.addAttribute("remainSeconds", remainSeconds);
        model.addAttribute("secKillStatus", secKillStatus);
        model.addAttribute("goods", goodsVo);
        return "goodsDetail";
    }
}