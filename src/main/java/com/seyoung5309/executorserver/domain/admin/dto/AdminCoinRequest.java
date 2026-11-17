package com.seyoung5309.executorserver.domain.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 코인 조정.
 *
 * @param amount 더할 코인. 음수면 뺀다 (0 불가). 빼서 0보다 작아지면 409 NOT_ENOUGH_COIN
 */
public record AdminCoinRequest(
        @NotNull(message = "amount를 입력해 주세요.")
        @Min(value = -1_000_000, message = "amount는 -1,000,000 이상이어야 합니다.")
        @Max(value = 1_000_000, message = "amount는 1,000,000 이하여야 합니다.")
        Integer amount
) {
}
