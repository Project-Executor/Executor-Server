package com.seyoung5309.executorserver.domain.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

/**
 * 우편 발송.
 *
 * @param receiverIds        받는 사용자 ID. toAll이 true면 무시한다
 * @param toAll              true면 모든 사용자에게 한 통씩 보낸다 (기본 false)
 * @param rewardCoin         첨부 코인 (생략하면 0)
 * @param rewardItemId       첨부 아이템. 없으면 null이고 rewardItemQuantity는 0이어야 한다
 * @param rewardItemQuantity 첨부 아이템 수량 (기본 0). 스킨·테마는 1, 힌트는 1~99
 * @param expiresAt          만료 시각. null이면 무기한, 값이 있으면 지금보다 뒤여야 한다
 */
public record AdminMailRequest(
        @Size(max = 1000, message = "receiverIds는 한 번에 1000명까지 보낼 수 있습니다.")
        List<@NotNull(message = "receiverIds에 null이 있습니다.") Long> receiverIds,

        Boolean toAll,

        @NotBlank(message = "title을 입력해 주세요.")
        @Size(max = 100, message = "title은 100자 이하여야 합니다.")
        String title,

        @Size(max = 2000, message = "content는 2000자 이하여야 합니다.")
        String content,

        @Min(value = 0, message = "rewardCoin은 0 이상이어야 합니다.")
        @Max(value = 1_000_000, message = "rewardCoin은 1,000,000 이하여야 합니다.")
        Integer rewardCoin,

        Long rewardItemId,

        @Min(value = 0, message = "rewardItemQuantity는 0 이상이어야 합니다.")
        @Max(value = 99, message = "rewardItemQuantity는 99 이하여야 합니다.")
        Integer rewardItemQuantity,

        Instant expiresAt
) {
    public int rewardCoinOrZero() {
        return rewardCoin == null ? 0 : rewardCoin;
    }

    public int rewardItemQuantityOrZero() {
        return rewardItemQuantity == null ? 0 : rewardItemQuantity;
    }
}
