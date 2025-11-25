package com.ev.charging.controller;

import com.ev.charging.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查控制器
 */
@Tag(name = "系统健康检查", description = "系统健康状态检查接口")
@RestController
@RequestMapping(value = "", produces = "application/json;charset=UTF-8")
public class HealthController {

    @Operation(
            summary = "健康检查",
            description = "检查系统是否正常运行"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "系统运行正常")
    })
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());
        return Result.success(health);
    }
}
