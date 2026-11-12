package com.seyoung5309.executorserver.domain.admin.dto;

import com.seyoung5309.executorserver.domain.stage.entity.StageType;
import tools.jackson.databind.JsonNode;

import java.time.Instant;

/**
 * 관리자용 스테이지 정보. 사용자용과 달리 mapData에 hints가 그대로 들어 있다.
 *
 * @param hintCount mapData의 hints 개수
 */
public record AdminStageResponse(
        Long stageId,
        Long operationId,
        int stageNo,
        StageType stageType,
        String title,
        JsonNode mapData,
        int hintCount,
        int parMoves,
        int rewardCoin,
        Instant createdAt,
        Instant updatedAt
) {
}
