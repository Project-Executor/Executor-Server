package com.seyoung5309.executorserver.domain.stage.dto;

import com.seyoung5309.executorserver.global.response.UserSummary;

import java.time.Instant;
import java.util.List;

/**
 * 스테이지 최소 턴 순위.
 *
 * @param rankings 상위 N명
 * @param me       내 순위 (상위 N명 밖이어도 계산한다). 클리어하지 않았으면 null
 * @param total    이 범위(전체 / 나 + 친구)에서 클리어한 사람 수
 */
public record RankingResponse(List<Entry> rankings, MyRank me, long total) {

    /** @param clearedAt 첫 클리어 시각 (같은 턴이면 이 시각이 빠른 쪽이 앞) */
    public record Entry(int rank, UserSummary user, int bestMoves, Instant clearedAt) {
    }

    public record MyRank(long rank, int bestMoves) {
    }
}
