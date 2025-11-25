package com.ev.charging.controller;

import com.ev.charging.common.Result;
import com.ev.charging.entity.ChatMessage;
import com.ev.charging.entity.User;
import com.ev.charging.repository.ChatMessageRepository;
import com.ev.charging.repository.UserRepository;
import com.ev.charging.vo.ChatMessageVO;
import com.ev.charging.vo.OnlineUserVO;
import com.ev.charging.websocket.ChargingChatHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Tag(name = "充电搭子聊天", description = "同站车主实时聊天、在线用户查询等功能")
@RestController
@RequestMapping(value = "/chat", produces = "application/json;charset=UTF-8")
public class ChatController {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private UserRepository userRepository;

    @Operation(
            summary = "获取聊天历史",
            description = "获取指定充电站的聊天历史记录"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功")
    })
    @GetMapping("/history")
    public Result<List<ChatMessageVO>> getChatHistory(
            @Parameter(description = "充电站ID", required = true, example = "1")
            @RequestParam Long stationId,
            @Parameter(description = "返回消息数量", example = "50")
            @RequestParam(defaultValue = "50") Integer limit
    ) {
        Pageable pageable = PageRequest.of(0, limit);
        List<ChatMessage> messages = chatMessageRepository
            .findByStationIdAndIsDeletedOrderByCreateTimeDesc(stationId, (byte) 0, pageable);

        // 反转顺序(最早的在前)
        Collections.reverse(messages);

        return Result.success(messages.stream()
            .map(this::convertToVO)
            .collect(Collectors.toList())
        );
    }

    @Operation(
            summary = "获取在线用户",
            description = "获取当前充电站在线的车主列表（WebSocket连接用户）"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功")
    })
    @GetMapping("/online-users")
    public Result<List<OnlineUserVO>> getOnlineUsers(
            @Parameter(description = "充电站ID", required = true, example = "1")
            @RequestParam Long stationId) {
        // 从WebSocket会话中获取在线用户
        Set<WebSocketSession> sessions = ChargingChatHandler.getStationSessions(stationId);

        // 收集所有在线用户ID
        List<Long> userIds = sessions.stream()
            .map(session -> (Long) session.getAttributes().get("userId"))
            .filter(userId -> userId != null)
            .collect(Collectors.toList());

        if (userIds.isEmpty()) {
            return Result.success(Collections.emptyList());
        }

        // 批量查询用户信息，避免N+1查询
        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
            .collect(Collectors.toMap(User::getId, user -> user));

        List<OnlineUserVO> onlineUsers = userIds.stream()
            .map(userId -> {
                User user = userMap.get(userId);
                if (user == null) {
                    return null;
                }
                return OnlineUserVO.builder()
                    .userId(userId)
                    .nickname(user.getNickname())
                    .avatar(user.getAvatar())
                    .carModel(user.getCarModel())
                    .carPlate(user.getCarPlate())
                    .isCharging(false)
                    .build();
            })
            .filter(vo -> vo != null)
            .collect(Collectors.toList());

        return Result.success(onlineUsers);
    }

    private ChatMessageVO convertToVO(ChatMessage message) {
        ChatMessageVO vo = new ChatMessageVO();
        vo.setMessageId(message.getId());
        BeanUtils.copyProperties(message, vo);
        return vo;
    }
}
