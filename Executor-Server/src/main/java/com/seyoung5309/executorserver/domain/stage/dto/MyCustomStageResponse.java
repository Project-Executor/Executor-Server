package com.seyoung5309.executorserver.domain.stage.dto;

import java.time.Instant;

/**
 * 내가 만든 맵 한 개 (비공개 포함).
 *
 * @param verified 내가 이 맵을 클리어했는지. true여야 공개할 수 있다
 */
public record MyCustomStageResponse(
        Long stageId,
        String title,
        int parMoves,
        boolean isPublic,
        boolean verified,
        Instant updatedAt
) {
}
