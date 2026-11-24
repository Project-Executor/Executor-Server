package com.seyoung5309.executorserver.domain.event.dto;

/**
 * 올린 기록의 순위.
 *
 * @param rank  같은 점수는 같은 순위
 * @param total 이 기록을 포함한 전체 기록 수
 */
public record EventRecordResponse(Long recordId, String name, int score, int rank, long total) {
}
