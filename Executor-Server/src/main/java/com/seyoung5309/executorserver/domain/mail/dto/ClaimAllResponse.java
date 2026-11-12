package com.seyoung5309.executorserver.domain.mail.dto;

import java.util.List;

/**
 * 받을 수 있는 우편을 모두 받은 결과.
 *
 * @param rewardItems 실제로 지급된 아이템 (같은 아이템은 수량을 합친다, 아이템 ID 순)
 */
public record ClaimAllResponse(int claimedCount, int rewardCoin, int convertedCoin, List<ItemQuantity> rewardItems,
                               int totalCoin) {

    public record ItemQuantity(Long itemId, int quantity) {
    }
}
