package com.seyoung5309.executorserver.domain.guidebook.dto;

import com.seyoung5309.executorserver.domain.guidebook.entity.Guidebook;
import com.seyoung5309.executorserver.domain.guidebook.entity.GuidebookCategory;

import java.time.Instant;
import java.util.List;

/**
 * 도감 목록. 분류를 지정했으면 개수도 그 분류 안에서 센다.
 * 잠긴 항목은 분류만 보이고 title·image·unlockedAt은 null.
 */
public record GuidebookListResponse(long unlockedCount, long totalCount, List<Item> items) {

    public record Item(Long guidebookId, GuidebookCategory category, boolean unlocked,
                       String title, String image, Instant unlockedAt) {

        /** @param unlockedAt 해금하지 않았으면 null */
        public static Item of(Guidebook guidebook, Instant unlockedAt) {
            boolean unlocked = unlockedAt != null;
            return new Item(
                    guidebook.getId(),
                    guidebook.getCategory(),
                    unlocked,
                    unlocked ? guidebook.getTitle() : null,
                    unlocked ? guidebook.getImage() : null,
                    unlockedAt
            );
        }
    }
}
