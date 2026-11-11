package com.seyoung5309.executorserver.domain.stage.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * @param hintIndex 받을 힌트 번호. 0부터 hintCount - 1까지 (위쪽 범위는 서비스에서 맵 데이터로 검사)
 */
public record HintRequest(
        @NotNull(message = "hintIndex를 입력해 주세요.")
        @Min(value = 0, message = "hintIndex는 0 이상이어야 합니다.")
        Integer hintIndex
) {
}
