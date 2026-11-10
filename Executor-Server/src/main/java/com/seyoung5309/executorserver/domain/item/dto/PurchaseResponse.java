package com.seyoung5309.executorserver.domain.item.dto;

/**
 * @param quantity      구매한 뒤의 보유 수량
 * @param spentCoin     이번 구매에 쓴 코인
 * @param remainingCoin 구매한 뒤 남은 코인
 */
public record PurchaseResponse(Long itemId, int quantity, int spentCoin, int remainingCoin) {
}
