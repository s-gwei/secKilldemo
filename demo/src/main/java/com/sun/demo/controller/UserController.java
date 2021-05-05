package com.sun.demo.controller;



import com.sun.demo.pojo.User;
import com.sun.demo.vo.RespBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * <p>
 * 前端控制器
 * </p>
 * <p>
 * 乐字节：专注线上IT培训
 * 答疑老师微信：lezijie
 *
 * @author zhoubin
 */
@Controller
@RequestMapping("/user")
public class UserController {



	/**
	 * 功能描述: 用户信息(测试)
	 *
	 * @param:
	 * @return: 乐字节：专注线上IT培训
	 * 答疑老师微信：lezijie
	 * @since: 1.0.0
	 * @Author:zhoubin
	 */
	@RequestMapping("/info")
	@ResponseBody
	public RespBean info(User user) {
		System.out.println(user.getNickname());
		return RespBean.success(user);
	}



}
