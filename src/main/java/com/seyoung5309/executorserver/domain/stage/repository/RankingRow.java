package com.seyoung5309.executorserver.domain.stage.repository;

/** 순위 조회 결과 한 줄 */
public record RankingRow(Long userId, String nickname, Long equippedSkinId, Integer bestMoves) {
}
