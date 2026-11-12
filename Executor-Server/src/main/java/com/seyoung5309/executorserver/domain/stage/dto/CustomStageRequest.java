package com.seyoung5309.executorserver.domain.stage.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.JsonNode;

/**
 * 사용자 맵 저장·수정 (PUT이므로 모든 값을 보낸다).
 *
 * @param mapData 맵 에디터가 만든 JSON 객체 그대로 (최대 256KB, hints가 있으면 배열)
 */
public record CustomStageRequest(
        @NotBlank(message = "title을 입력해 주세요.")
        @Size(max = 100, message = "title은 100자 이하여야 합니다.")
        String title,

        @NotNull(message = "parMoves를 입력해 주세요.")
        @Min(value = 1, message = "parMoves는 1 이상이어야 합니다.")
        @Max(value = 9999, message = "parMoves는 9999 이하여야 합니다.")
        Integer parMoves,

        @NotNull(message = "mapData를 입력해 주세요.")
        JsonNode mapData
) {
}
