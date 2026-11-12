package com.seyoung5309.executorserver.domain.guidebook.dto;

import com.seyoung5309.executorserver.domain.guidebook.entity.GuidebookCategory;

import java.time.Instant;

/** 해금한 도감 항목의 상세. stageId는 처음 등장하는 스테이지 (없으면 null) */
public record GuidebookDetailResponse(
        Long guidebookId,
        GuidebookCategory category,
        String title,
        String image,
        String comment,
        Long stageId,
        Instant unlockedAt
) {
}
