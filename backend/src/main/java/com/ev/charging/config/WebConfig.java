package com.ev.charging.config;

import com.ev.charging.interceptor.RateLimitInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * Web配置类 - 配置跨域、拦截器、消息转换器等
 *
 * 安全特性:
 * 1. CORS限制允许的来源
 * 2. 频率限制拦截（防止DOS攻击）
 * 3. UTF-8编码确保
 *
 * 注意：JWT认证由Spring Security的JwtAuthenticationFilter处理，
 * 不再使用JwtInterceptor，避免双重认证机制冲突
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    /**
     * 配置内容协商，确保默认返回JSON并使用UTF-8编码
     * 这会影响所有响应，包括异常处理返回的内容
     */
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer
                .favorParameter(false)
                .ignoreAcceptHeader(false)
                .defaultContentType(new MediaType("application", "json", StandardCharsets.UTF_8))
                .mediaType("json", new MediaType("application", "json", StandardCharsets.UTF_8));
    }

    /**
     * CORS由SecurityConfig统一管理（Spring Security的CorsFilter优先级高于WebMvc的CORS配置）
     * 不在此处重复配置，避免维护两套CORS规则导致不一致
     */

    /**
     * 配置拦截器
     *
     * 仅配置频率限制拦截器，防止DOS攻击
     * JWT认证由Spring Security的JwtAuthenticationFilter统一处理
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 频率限制拦截器
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/**")
                .order(1);
    }

    /**
     * 扩展消息转换器，确保UTF-8编码和日期格式
     * 使用extendMessageConverters而非configureMessageConverters，
     * 避免clear()破坏ByteArrayHttpMessageConverter等关键默认转换器
     */
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        // 移除默认的String和JSON转换器，替换为我们自定义的版本
        converters.removeIf(c -> c instanceof StringHttpMessageConverter || c instanceof MappingJackson2HttpMessageConverter);

        // 1. 配置字符串转换器使用UTF-8
        StringHttpMessageConverter stringConverter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
        stringConverter.setWriteAcceptCharset(false); // 不写入Accept-Charset头

        // 支持所有文本类型
        stringConverter.setSupportedMediaTypes(Arrays.asList(
                new MediaType("text", "plain", StandardCharsets.UTF_8),
                new MediaType("text", "html", StandardCharsets.UTF_8),
                MediaType.APPLICATION_JSON
        ));
        converters.add(stringConverter);

        // 2. 配置JSON转换器使用UTF-8和日期格式
        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
        jsonConverter.setDefaultCharset(StandardCharsets.UTF_8);

        // 明确支持的媒体类型
        jsonConverter.setSupportedMediaTypes(Arrays.asList(
                new MediaType("application", "json", StandardCharsets.UTF_8),
                new MediaType("application", "*+json", StandardCharsets.UTF_8),
                new MediaType("text", "json", StandardCharsets.UTF_8)
        ));

        // 配置ObjectMapper
        ObjectMapper objectMapper = jsonConverter.getObjectMapper();

        // 禁用Unicode转义（确保中文原样输出）
        objectMapper.configure(com.fasterxml.jackson.core.JsonGenerator.Feature.ESCAPE_NON_ASCII, false);

        // 配置日期时间格式
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(dateTimeFormatter));
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(dateTimeFormatter));

        objectMapper.registerModule(javaTimeModule);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        jsonConverter.setObjectMapper(objectMapper);

        converters.add(jsonConverter);
    }
}
