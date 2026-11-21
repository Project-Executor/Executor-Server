package com.seyoung5309.executorserver.domain.profile.dto;

import com.seyoung5309.executorserver.domain.auth.entity.Role;
import com.seyoung5309.executorserver.domain.auth.entity.Account;
import com.seyoung5309.executorserver.domain.profile.entity.User;

/** 내 프로필. equipped*Id가 null이면 기본 (스킨·테마·커서, 꾸미지 않은 델타) */
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
        Role role
) {

    public static MyProfileResponse from(User profile) {
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
                account.getRole()
        );
    }
}
