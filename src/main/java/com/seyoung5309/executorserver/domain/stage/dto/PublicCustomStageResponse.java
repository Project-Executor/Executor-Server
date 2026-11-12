package com.seyoung5309.executorserver.domain.stage.dto;

import com.seyoung5309.executorserver.global.response.UserSummary;

import java.time.Instant;

/**
 * 공개된 사용자 맵 목록의 한 개.
 *
 * @param clearCount 이 맵을 클리어한 사용자 수 (제작자 포함)
 * @param myCleared  내가 클리어했는지
 */
public record PublicCustomStageResponse(
        Long stageId,
        String title,
        UserSummary creator,
        int parMoves,
        long clearCount,
        boolean myCleared,
        Instant createdAt
) {
}
