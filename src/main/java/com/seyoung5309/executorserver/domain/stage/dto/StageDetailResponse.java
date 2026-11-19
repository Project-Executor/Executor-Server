package com.seyoung5309.executorserver.domain.stage.dto;

import com.seyoung5309.executorserver.domain.stage.entity.StageType;
import com.seyoung5309.executorserver.global.response.UserSummary;
import tools.jackson.databind.JsonNode;


/**
 * 스테이지 상세.
 * <ul>
 *   <li>creator: 공식 스테이지는 null, 사용자 맵은 제작자</li>
 *   <li>mapData: map_data JSON (객체 그대로)</li>
 * </ul>
 */
public record StageDetailResponse(
        Long stageId,
        Long operationId,
        Integer stageNo,
        StageType stageType,
        String title,
        UserSummary creator,
        int parMoves,
        int rewardCoin,
        JsonNode mapData,
        MyRecordResponse myRecord
) {
}
