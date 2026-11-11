package com.seyoung5309.executorserver.domain.admin.dto;

import com.seyoung5309.executorserver.domain.stage.entity.StageType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.JsonNode;

/**
 * 공식 스테이지 등록·수정 (PUT이므로 모든 값을 보낸다). 공개 여부는 항상 true다.
 *
 * @param mapData 맵 데이터 JSON 객체. 문자열로 감싸지 않고 객체 그대로 보낸다.
 *                "hints"가 있으면 배열이어야 한다 (그 밖의 구조는 Unity가 정한 대로 검사하지 않는다)
 */
public record AdminStageRequest(
        @NotNull(message = "operationId를 입력해 주세요.")
        Long operationId,

        @NotNull(message = "stageNo를 입력해 주세요.")
        @Min(value = 1, message = "stageNo는 1 이상이어야 합니다.")
        Integer stageNo,

        @NotNull(message = "stageType을 입력해 주세요. (TUTORIAL, NORMAL, BOSS)")
        StageType stageType,

        @NotBlank(message = "title을 입력해 주세요.")
        @Size(max = 100, message = "title은 100자 이하여야 합니다.")
        String title,

        @NotNull(message = "mapData를 입력해 주세요.")
        JsonNode mapData,

        @NotNull(message = "parMoves를 입력해 주세요.")
        @Min(value = 1, message = "parMoves는 1 이상이어야 합니다.")
        @Max(value = 9999, message = "parMoves는 9999 이하여야 합니다.")
        Integer parMoves,

        @NotNull(message = "rewardCoin을 입력해 주세요.")
        @Min(value = 0, message = "rewardCoin은 0 이상이어야 합니다.")
        Integer rewardCoin
) {
}
