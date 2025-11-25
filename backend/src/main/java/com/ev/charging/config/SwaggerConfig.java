package com.ev.charging.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger/OpenAPI 配置
 * 访问地址: http://localhost:8080/api/swagger-ui.html
 * API文档: http://localhost:8080/api/v3/api-docs
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("新能源汽车充电站点管理系统 API")
                        .version("1.0.0")
                        .description("""
                                ## 系统简介
                                新能源汽车充电站点管理系统提供完整的充电服务管理功能，包括：

                                ### 核心功能
                                - 用户认证与授权（JWT）
                                - 充电站与充电桩管理
                                - 智能订单系统
                                - AI充电预测
                                - 智能排队系统
                                - 碳积分体系
                                - V2G双向充电
                                - 充电搭子社交
                                - 代充服务
                                - 周边服务推荐

                                ### 技术架构
                                - 后端: Spring Boot 3.x + JPA + Redis
                                - 前端: Vue3 + Vant4/Element Plus
                                - AI服务: Python Flask + Scikit-learn
                                - 数据库: MySQL 8.0

                                ### 认证说明
                                大部分接口需要JWT认证，请先调用登录接口获取token，然后在请求头中添加：
                                ```
                                Authorization: Bearer {token}
                                ```
                                """)
                        .contact(new Contact()
                                .name("开发团队")
                                .email("dev@example.com")
                                .url("https://github.com/your-repo"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080/api")
                                .description("本地开发环境"),
                        new Server()
                                .url("http://localhost:8080/api")
                                .description("生产环境")
                ))
                .components(new Components()
                        .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)
                                .name("Authorization")
                                .description("JWT认证token，格式: Bearer {token}")))
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }
}
