package com.sun.demo.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * author sungw
 *
 * @description 消息队列工具类
 * @date 2021/5/24
 */
@Configuration
public class RabbitMqConfig {
        @Bean
        public Queue orderQueue() {
            return new Queue("orderQueue");
        }


}
