package com.seyoung5309.executorserver.domain.item.repository;

/** 사용자가 가진 아이템 ID와 보유 수량 */
public record OwnedItemRow(Long itemId, int quantity) {
}
