package com.seyoung5309.executorserver.domain.profile.dto;

/**
 * 다른 사용자의 공개 프로필.
 * clearedStageCount·totalStars는 공식 스테이지만 센다.
 */
public record ProfileDetailResponse(
        Long userId,
        String nickname,
        String comment,
        Long equippedSkinId,
        long clearedStageCount,
        long totalStars,
        boolean isFriend
) {
}
