package com.seyoung5309.executorserver.domain.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 단어 목록 등록·수정 (PUT이므로 모든 값을 보낸다).
 *
 * @param words 단어. 앞뒤 공백을 지우고 빈 줄·중복은 뺀다. 남은 단어가 1개 이상이어야 하고 하나는 50자 이하
 */
public record AdminWordListRequest(
        @NotBlank(message = "name을 입력해 주세요.")
        @Size(max = 100, message = "name은 100자 이하여야 합니다.")
        String name,

        @Size(max = 255, message = "description은 255자 이하여야 합니다.")
        String description,

        @NotNull(message = "words를 입력해 주세요.")
        @Size(max = 1000, message = "words는 1000개 이하여야 합니다.")
        List<String> words
) {
}
