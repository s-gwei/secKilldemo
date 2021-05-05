package com.sun.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sun.demo.pojo.Goods;
import com.sun.demo.vo.GoodsVo;
import org.springframework.stereotype.Repository;


import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * 乐字节：专注线上IT培训
 * 答疑老师微信：lezijie
 *
 * @author zhoubin
 *
 */
@Repository
public interface GoodsMapper extends BaseMapper<Goods> {


	List<GoodsVo> findGoodsVo();


	GoodsVo findGoodsVoByGoodsId(Long goodsId);
}
