package com.seyoung5309.executorserver.domain.profile.dto;

import com.seyoung5309.executorserver.domain.auth.entity.Role;
import com.seyoung5309.executorserver.domain.auth.entity.Account;
import com.seyoung5309.executorserver.domain.profile.entity.User;

import java.time.Instant;

/**
 * 내 프로필. equipped*Id가 null이면 기본 (스킨·테마·커서, 꾸미지 않은 델타).
 * avatarVersion: 프로필 사진을 바꾼 시각(초). 사진이 없으면 null (기본 실루엣). 값이 바뀌면 GET /profiles/{userId}/avatar를 다시 받는다
 */
public record MyProfileResponse(
        Long userId,
        String email,
        String nickname,
        String comment,
        int coin,
        Long equippedSkinId,
        Long equippedThemeId,
        Long equippedCursorId,
        Long equippedDeltaId,
        Role role,
        Long avatarVersion
) {

    public static MyProfileResponse from(User profile, Instant avatarUpdatedAt) {
        Account account = profile.getAccount();
        return new MyProfileResponse(
                profile.getUserId(),
                account.getEmail(),
                profile.getNickname(),
                profile.getComment(),
                profile.getCoin(),
                profile.getEquippedSkinId(),
                profile.getEquippedThemeId(),
                profile.getEquippedCursorId(),
                profile.getEquippedDeltaId(),
                account.getRole(),
                avatarUpdatedAt == null ? null : avatarUpdatedAt.getEpochSecond()
        );
    }
}
