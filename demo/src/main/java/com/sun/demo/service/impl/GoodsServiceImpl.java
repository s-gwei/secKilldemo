package com.sun.demo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.sun.demo.mapper.GoodsMapper;
import com.sun.demo.pojo.Goods;
import com.sun.demo.service.IGoodsService;
import com.sun.demo.vo.GoodsVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 * 乐字节：专注线上IT培训
 * 答疑老师微信：lezijie
 *
 * @author zhoubin
 *
 */
@Service
public class GoodsServiceImpl extends ServiceImpl<GoodsMapper, Goods> implements IGoodsService {

	@Autowired
	private GoodsMapper goodsMapper;


	@Override
	public List<GoodsVo> findGoodsVo() {
		return goodsMapper.findGoodsVo();
	}

	@Override
	public GoodsVo findGoodsVoByGoodsId(Long goodsId) {
		return goodsMapper.findGoodsVoByGoodsId(goodsId);
	}


}
