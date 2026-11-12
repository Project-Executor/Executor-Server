package com.seyoung5309.executorserver.global.response;

import com.seyoung5309.executorserver.domain.profile.entity.Profile;

/**
 * 다른 사용자를 표시할 때 쓰는 공통 객체 (API 명세서 0.4).
 * { "userId": 12, "nickname": "debugger", "equippedSkinId": 3 }
 */
public record UserSummary(Long userId, String nickname, Long equippedSkinId) {

    public static UserSummary from(Profile profile) {
        return new UserSummary(profile.getUserId(), profile.getNickname(), profile.getEquippedSkinId());
    }
}
