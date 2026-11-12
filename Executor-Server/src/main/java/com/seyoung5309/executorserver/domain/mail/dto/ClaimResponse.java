package com.seyoung5309.executorserver.domain.mail.dto;

import com.seyoung5309.executorserver.domain.item.entity.ItemType;

/**
 * 우편 한 통을 받은 결과.
 *
 * @param rewardCoin    첨부 코인
 * @param convertedCoin 이미 가진 스킨·테마를 코인으로 바꾼 금액 (바꾼 게 없으면 0)
 * @param rewardItem    실제로 지급된 아이템. 첨부가 없거나 코인으로 바꿨으면 null
 * @param totalCoin     받은 뒤 내 코인
 */
public record ClaimResponse(Long mailId, int rewardCoin, int convertedCoin, GrantedItem rewardItem, int totalCoin) {

    public record GrantedItem(Long itemId, ItemType itemType, int quantity) {
    }
}
