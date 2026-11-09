package com.seyoung5309.executorserver.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record ReissueRequest(
        @NotBlank(message = "refreshToken을 입력해 주세요.")
        String refreshToken
) {
}
