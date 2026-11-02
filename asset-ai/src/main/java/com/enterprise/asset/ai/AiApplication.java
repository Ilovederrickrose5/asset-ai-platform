package com.enterprise.asset.ai;

import org.springframework.ai.autoconfigure.chat.client.ChatClientAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * AI 知识助手模块启动类
 * 提供基于 RAG 的资产管理制度问答与会话管理
 */
@SpringBootApplication(scanBasePackages = {
        "com.enterprise.asset.ai",
        "com.enterprise.asset.common"
})
@EnableAutoConfiguration(exclude = {
        ChatClientAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class
})
@EnableDiscoveryClient
public class AiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiApplication.class, args);
    }
}
