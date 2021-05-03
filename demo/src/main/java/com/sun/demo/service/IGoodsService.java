package com.sun.demo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sun.demo.vo.GoodsVo;


import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * 乐字节：专注线上IT培训
 * 答疑老师微信：lezijie
 *
 * @author zhoubin
 *
 */
public interface IGoodsService extends IService<Goods> {

	/**
	 * 功能描述: 获取商品列表
	 *
	 * @param:
	 * @return:
	 *
	 * 乐字节：专注线上IT培训
	 * 答疑老师微信：lezijie
	 * @since: 1.0.0
	 * @Author:zhoubin
	 */
	List<GoodsVo> findGoodsVo();


}
