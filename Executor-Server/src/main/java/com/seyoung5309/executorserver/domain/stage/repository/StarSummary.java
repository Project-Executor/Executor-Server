package com.seyoung5309.executorserver.domain.stage.repository;

/**
 * 공식 스테이지 클리어·별 집계.
 *
 * @param clearedCount 클리어한 공식 스테이지 수 (= 별 1 개수)
 * @param parCount     기준 턴 달성 수 (별 2)
 * @param hiddenCount  숨겨진 요소 달성 수 (별 3)
 */
public record StarSummary(long clearedCount, long parCount, long hiddenCount) {

    public long totalStars() {
        return clearedCount + parCount + hiddenCount;
    }
}
