package com.ev.charging.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ev.charging.security.JwtAuthenticationEntryPoint;
import com.ev.charging.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Spring Security 安全配置
 * 
 * 安全特性:
 * 1. JWT Token认证
 * 2. 基于角色的访问控制(RBAC)
 * 3. CSRF保护(API模式下禁用)
 * 4. CORS跨域配置
 * 5. 密码加密(BCrypt)
 * 6. 登录失败限制(通过自定义Service实现)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // 启用@PreAuthorize注解
@RequiredArgsConstructor
public class SecurityConfig {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @org.springframework.beans.factory.annotation.Value("${cors.allowed-origins:http://localhost:5173,http://localhost:5174,http://localhost:5175,http://127.0.0.1:5173,http://127.0.0.1:5174,http://127.0.0.1:5175}")
    private String allowedOrigins;

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    /**
     * 配置安全过滤器链
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 禁用CSRF(因为使用JWT，不需要CSRF保护)
            .csrf(AbstractHttpConfigurer::disable)

            // 配置CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // 安全响应头
            .headers(headers -> headers
                .frameOptions(frame -> frame.deny())
                .contentTypeOptions(contentType -> {})
            )

            // 配置异常处理
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(403);
                    Map<String, Object> body = Map.of("code", 403, "message", "权限不足，拒绝访问", "data", "");
                    response.getWriter().write(OBJECT_MAPPER.writeValueAsString(body));
                })
            )
            
            // 配置会话管理(无状态)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // 配置请求授权
            .authorizeHttpRequests(auth -> auth
                // 公开接口 - 无需认证
                .requestMatchers(
                    "/auth/login",               // 登录
                    "/auth/register",            // 注册
                    "/health",               // 健康检查
                    "/actuator/health",      // Spring Actuator健康检查(仅health公开)
                    "/error",                // 错误页面
                    "/favicon.ico",          // 浏览器图标
                    "/ws/**",                // WebSocket连接
                    "/swagger-ui/**",        // Swagger UI
                    "/v3/api-docs/**",       // OpenAPI文档
                    "/swagger-resources/**", // Swagger资源
                    "/webjars/**"            // Swagger依赖
                ).permitAll()

                // AI预测接口 - 允许游客访问
                .requestMatchers("/ai/**").permitAll()

                // 允许游客访问的接口 (GET only)
                .requestMatchers(HttpMethod.GET,
                    "/stations/**",          // 充电站查询
                    "/piles/**",             // 充电桩查询（GET方法）
                    "/nearby/**",            // 周边服务查询
                    "/credits/products",     // 积分商品列表（CarbonCreditController）
                    "/credits/products/*",   // 积分商品详情（CarbonCreditController）
                    "/credit-products/**"    // 积分商品列表（CreditProductController）
                ).permitAll()

                // 数据源监控接口 - 仅ADMIN可访问
                .requestMatchers("/monitor/datasource/**").hasRole("ADMIN")

                // 充电站/充电桩写操作 - 需要ADMIN或OPERATOR角色
                .requestMatchers(HttpMethod.POST, "/stations/**", "/piles/**").hasAnyRole("ADMIN", "OPERATOR")
                .requestMatchers(HttpMethod.PUT, "/stations/**", "/piles/**").hasAnyRole("ADMIN", "OPERATOR")
                .requestMatchers(HttpMethod.DELETE, "/stations/**", "/piles/**").hasAnyRole("ADMIN", "OPERATOR")
                .requestMatchers(HttpMethod.PATCH, "/piles/**").hasAnyRole("ADMIN", "OPERATOR")

                // 管理后台接口 - 需要ADMIN或OPERATOR角色
                .requestMatchers("/admin/**").hasAnyRole("ADMIN", "OPERATOR")

                // Actuator端点 - 仅ADMIN可访问(health已在上方公开)
                .requestMatchers("/actuator/**").hasRole("ADMIN")

                // V2G管理端接口 - 需要ADMIN或OPERATOR角色 (必须在其他/v2g/**规则前)
                .requestMatchers("/v2g/admin/**").hasAnyRole("ADMIN", "OPERATOR")

                // 用户端接口 - 需要USER角色
                .requestMatchers("/users/**").hasRole("USER")
                .requestMatchers("/orders/**").hasRole("USER")
                .requestMatchers("/queue/**").hasRole("USER")

                // 能源监控数据 - 需要USER角色或ADMIN/OPERATOR
                .requestMatchers(HttpMethod.GET, "/energy/**").hasAnyRole("USER", "ADMIN", "OPERATOR")

                // 碳积分相关端点 - 需要USER角色 (但/credits/products GET是公开的)
                .requestMatchers(HttpMethod.POST, "/credits/**").hasRole("USER")
                .requestMatchers(HttpMethod.PUT, "/credits/**").hasRole("USER")
                .requestMatchers(HttpMethod.DELETE, "/credits/**").hasRole("USER")
                .requestMatchers("/credits/balance", "/credits/history", "/credits/statistics",
                               "/credits/pending", "/credits/redeem", "/credits/checkin", "/credits/exchange",
                               "/credits/exchange/records").hasRole("USER")

                // V2G用户端接口 - 需要USER角色
                .requestMatchers("/v2g/discharge/**", "/v2g/records", "/v2g/statistics",
                               "/v2g/price/forecast", "/v2g/strategy/set", "/v2g/profit-estimate").hasRole("USER")

                // 代充服务路由规则（顺序很重要，更具体的规则必须在前面）

                // 1. 代充师傅注册 - 需要USER角色（已登录用户申请成为代充师傅）
                .requestMatchers(HttpMethod.POST, "/valet/charger/register").hasRole("USER")

                // 2. 代充管理端接口 - 需要ADMIN角色
                .requestMatchers(HttpMethod.GET, "/valet/order/admin/all").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/valet/charger/*/audit").hasRole("ADMIN")

                // 3. 代充统计接口 - 需要ADMIN或OPERATOR角色
                .requestMatchers(HttpMethod.GET, "/valet/statistics").hasAnyRole("ADMIN", "OPERATOR")

                // 4. 用户端代充接口（非师傅端）- 需要USER角色
                .requestMatchers(HttpMethod.GET, "/valet/charger/list").hasAnyRole("USER", "ADMIN", "OPERATOR")
                .requestMatchers(HttpMethod.GET, "/valet/charger/my").hasRole("USER")
                .requestMatchers(HttpMethod.POST, "/valet/order/create").hasRole("USER")
                .requestMatchers(HttpMethod.GET, "/valet/order/my").hasRole("USER")

                // 5. 代充师傅接口（接单、完成、状态更新等）- 允许USER角色
                // 注册后的师傅仍是USER角色，IDOR防护由Controller层处理（findByUserId验证所有权）
                .requestMatchers("/valet/**").hasRole("USER")

                // 聊天接口 - 需要USER角色
                .requestMatchers(HttpMethod.GET, "/chat/history", "/chat/online-users").hasRole("USER")
                .requestMatchers(HttpMethod.POST, "/chat/**").hasRole("USER")

                // 其他所有请求都需要认证
                .anyRequest().authenticated()
            )
            
            // 添加JWT过滤器
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 配置CORS跨域
     * 安全修复: 限制允许的来源、HTTP方法、请求头，避免CSRF攻击
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 允许的来源 - 从配置读取（支持环境变量 CORS_ALLOWED_ORIGINS 覆盖）
        configuration.setAllowedOriginPatterns(Arrays.asList(allowedOrigins.split(",")));

        // 允许的HTTP方法 - 限定为必要的方法
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        // 允许的请求头 - 限定为必要的头信息（避免通配符）
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization", "Content-Type", "X-Requested-With"
        ));

        // 允许携带凭证（Cookie、Authorization）
        configuration.setAllowCredentials(true);

        // 预检请求的有效期(秒) - 1小时
        configuration.setMaxAge(3600L);

        // 暴露的响应头
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization", "Content-Type"
        ));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * 密码编码器 - 使用BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 认证管理器
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
