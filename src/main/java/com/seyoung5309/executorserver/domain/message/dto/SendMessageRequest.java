package com.seyoung5309.executorserver.domain.message.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
        @NotNull(message = "receiverId를 입력해 주세요.")
        Long receiverId,

        @NotBlank(message = "content를 입력해 주세요.")
        @Size(max = 500, message = "content는 500자 이하여야 합니다.")
        String content
) {
}
