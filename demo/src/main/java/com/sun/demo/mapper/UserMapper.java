package com.sun.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sun.demo.pojo.User;
import org.springframework.stereotype.Repository;

@Repository
public interface UserMapper extends BaseMapper<User> {
}
