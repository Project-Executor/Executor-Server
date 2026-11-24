package com.seyoung5309.executorserver.domain.event.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 이벤트 기록 올리기 (로그인 없음).
 *
 * @param name  순위에 보일 이름 (앞뒤 공백을 지우고 1~12자, 같은 스테이지에서 겹치면 409)
 * @param score 턴수 이벤트는 기록 턴(1 이상), 타자 이벤트는 친 단어 수(0 이상)
 */
public record EventRecordRequest(
        @NotBlank(message = "이름을 입력해 주세요.")
        @Size(max = 40, message = "이름은 12자 이하여야 합니다.")
        String name,

        @NotNull(message = "score를 입력해 주세요.")
        @Min(value = 0, message = "score는 0 이상이어야 합니다.")
        @Max(value = 100000, message = "score가 너무 큽니다.")
        Integer score
) {
}
