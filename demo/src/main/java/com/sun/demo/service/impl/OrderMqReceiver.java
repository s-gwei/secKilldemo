package com.sun.demo.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.sun.demo.service.IOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * author sungw
 *
 * @description 消费者
 * @date 2021/5/24
 */
@Component
@RabbitListener(queues = "orderQueue")
@Slf4j
public class OrderMqReceiver {

    @Autowired
    private IOrderService orderService;

    @RabbitHandler
    public void process(String message) {
        log.info("OrderMqReceiver收到消息开始用户下单流程: " + message);
        JSONObject jsonObject = JSONObject.parseObject(message);
        try {
            jsonObject.getLong("goodsId");
            orderService.createOrderByMq(jsonObject.getLong("goodsId"),jsonObject.getLong("userId"));
        } catch (Exception e) {
            log.error("消息处理异常：", e);
        }
    }
}
