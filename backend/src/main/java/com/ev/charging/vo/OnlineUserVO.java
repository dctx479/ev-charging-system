package com.ev.charging.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnlineUserVO {
    private Long userId;
    private String nickname;
    private String avatar;
    private String carModel;
    private String carPlate;
    private Boolean isCharging;
}
