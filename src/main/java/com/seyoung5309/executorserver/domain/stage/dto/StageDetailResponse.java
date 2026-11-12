package com.seyoung5309.executorserver.domain.stage.dto;

import com.seyoung5309.executorserver.domain.stage.entity.StageType;
import com.seyoung5309.executorserver.global.response.UserSummary;
import tools.jackson.databind.JsonNode;

import java.util.List;

/**
 * 스테이지 상세.
 * <ul>
 *   <li>creator: 공식 스테이지는 null, 사용자 맵은 제작자</li>
 *   <li>mapData: map_data에서 hints를 뺀 JSON (객체 그대로)</li>
 *   <li>hintCount: 힌트 아이템으로 받을 수 있는 힌트 개수</li>
 *   <li>viewedHintIndexes: 내가 이미 본 힌트 번호 (오름차순). 이 번호는 다시 요청해도 아이템을 쓰지 않는다</li>
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
        int hintCount,
        List<Integer> viewedHintIndexes,
        JsonNode mapData,
        MyRecordResponse myRecord
) {
}
