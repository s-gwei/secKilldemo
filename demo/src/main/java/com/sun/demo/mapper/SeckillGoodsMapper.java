package com.sun.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sun.demo.pojo.SeckillGoods;

public interface SeckillGoodsMapper  extends BaseMapper<SeckillGoods> {
    int updateCount(SeckillGoods seckillGoods);
}
