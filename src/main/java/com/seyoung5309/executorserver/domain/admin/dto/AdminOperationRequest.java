package com.seyoung5309.executorserver.domain.admin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 작전 등록·수정 (PUT이므로 모든 값을 보낸다).
 *
 * @param sortOrder 표시 순서. 튜토리얼이 0이고 작전끼리 겹칠 수 없다
 */
public record AdminOperationRequest(
        @NotBlank(message = "name을 입력해 주세요.")
        @Size(max = 100, message = "name은 100자 이하여야 합니다.")
        String name,

        @Size(max = 255, message = "description은 255자 이하여야 합니다.")
        String description,

        @NotNull(message = "sortOrder를 입력해 주세요.")
        @Min(value = 0, message = "sortOrder는 0 이상이어야 합니다.")
        Integer sortOrder
) {
}
