package com.example.delivery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 后端应用引导类，负责启动 Spring MVC、安全、持久化和 WebSocket 等自动配置。
 * IDEA、Maven 或生产脚本均从这里启动；子包通过组件扫描注册为 Spring Bean。
 */
@SpringBootApplication
public class DeliveryApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeliveryApplication.class, args);
    }
}
