package com.seyoung5309.executorserver.domain.stage.repository;

/** 스테이지 하나의 클리어한 사용자 수 */
public record StageClearCount(Long stageId, long count) {
}
