package com.seyoung5309.executorserver.domain.admin.dto;

import com.seyoung5309.executorserver.domain.auth.entity.Role;
import com.seyoung5309.executorserver.domain.item.entity.ItemType;
import com.seyoung5309.executorserver.domain.item.entity.UserItem;

import java.time.Instant;
import java.util.List;

/**
 * 관리자 유저 상세.
 *
 * @param nickname          가입 후 닉네임을 정하기 전이면 null
 * @param equippedSkinId    null이면 기본 스킨
 * @param equippedThemeId   null이면 기본 테마
 * @param clearedStageCount 클리어한 공식 스테이지 수
 * @param totalStars        공식 스테이지에서 얻은 별 합계
 * @param items             보유 아이템 (수량 0인 힌트는 빠짐, 아이템 ID 순)
 * @param suspendedUntil    정지 기한. null이면 정지 아님, 9999-12-31T23:59:59Z면 영구. 지난 시각이면 이미 풀린 것
 */
public record AdminUserDetailResponse(
        Long userId,
        String email,
        String nickname,
        String comment,
        Role role,
        int coin,
        Instant createdAt,
        Instant suspendedUntil,
        String suspendReason,
        Long equippedSkinId,
        Long equippedThemeId,
        long clearedStageCount,
        long totalStars,
        List<OwnedItem> items
) {
    public record OwnedItem(Long itemId, String name, ItemType itemType, int quantity, Instant acquiredAt) {
        /** userItem.item이 읽혀 있어야 한다 (fetch join) */
        public static OwnedItem from(UserItem userItem) {
            return new OwnedItem(userItem.getItem().getId(), userItem.getItem().getName(),
                    userItem.getItem().getItemType(), userItem.getQuantity(), userItem.getAcquiredAt());
        }
    }
}
