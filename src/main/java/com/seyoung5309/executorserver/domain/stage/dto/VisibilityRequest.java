package com.seyoung5309.executorserver.domain.stage.dto;

import jakarta.validation.constraints.NotNull;

public record VisibilityRequest(
        @NotNull(message = "isPublic을 입력해 주세요.")
        Boolean isPublic
) {
}
