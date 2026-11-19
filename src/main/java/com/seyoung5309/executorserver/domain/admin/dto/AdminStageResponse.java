package com.seyoung5309.executorserver.domain.admin.dto;

import com.seyoung5309.executorserver.domain.stage.entity.StageType;
import tools.jackson.databind.JsonNode;

import java.time.Instant;

/** 관리자용 스테이지 정보 */
public record AdminStageResponse(
        Long stageId,
        Long operationId,
        int stageNo,
        StageType stageType,
        String title,
        JsonNode mapData,
        int parMoves,
        int rewardCoin,
        Instant createdAt,
        Instant updatedAt
) {
}
