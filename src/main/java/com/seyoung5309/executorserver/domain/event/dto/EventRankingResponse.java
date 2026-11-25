package com.seyoung5309.executorserver.domain.event.dto;

import com.seyoung5309.executorserver.domain.stage.entity.StageType;

import java.time.Instant;
import java.util.List;

/**
 * 이벤트 순위.
 *
 * @param stageType EVENT_TURNS(score = 턴, 적을수록 위) / EVENT_TYPING(score = 친 단어 수, 많을수록 위)
 * @param total     올라온 기록 수
 * @param rankings  위에서부터 (같은 점수는 같은 순위, 이름순)
 */
public record EventRankingResponse(
        Long stageId,
        String title,
        StageType stageType,
        long total,
        List<Entry> rankings
) {
    public record Entry(Long recordId, int rank, String name, int score, Instant createdAt) {
    }
}
