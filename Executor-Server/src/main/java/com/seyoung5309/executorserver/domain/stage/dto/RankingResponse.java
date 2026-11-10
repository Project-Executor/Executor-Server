package com.seyoung5309.executorserver.domain.stage.dto;

import com.seyoung5309.executorserver.global.response.UserSummary;

import java.util.List;

/**
 * 스테이지 최소 턴 순위.
 *
 * @param rankings 상위 N명
 * @param me       내 순위 (상위 N명 밖이어도 계산한다). 클리어하지 않았으면 null
 */
public record RankingResponse(List<Entry> rankings, MyRank me) {

    public record Entry(int rank, UserSummary user, int bestMoves) {
    }

    public record MyRank(long rank, int bestMoves) {
    }
}
