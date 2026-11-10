package com.seyoung5309.executorserver.domain.guidebook.repository;

import java.time.Instant;

/** 사용자가 해금한 도감 ID와 해금 시각 */
public record UnlockedGuidebookRow(Long guidebookId, Instant unlockedAt) {
}
