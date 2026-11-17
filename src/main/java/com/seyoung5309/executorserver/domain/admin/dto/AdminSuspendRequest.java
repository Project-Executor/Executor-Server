package com.seyoung5309.executorserver.domain.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * 계정 정지.
 *
 * @param until  정지 기한 (ISO-8601 UTC). null이면 영구 정지. 값이 있으면 지금보다 뒤여야 한다
 * @param reason 정지 사유. 사용자가 로그인할 때 보는 메시지에 들어간다
 */
public record AdminSuspendRequest(
        Instant until,

        @NotBlank(message = "reason을 입력해 주세요.")
        @Size(max = 200, message = "reason은 200자 이하여야 합니다.")
        String reason
) {
}
