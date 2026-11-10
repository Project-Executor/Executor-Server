package com.seyoung5309.executorserver.domain.item.dto;

import com.seyoung5309.executorserver.domain.item.entity.Item;
import com.seyoung5309.executorserver.domain.item.entity.ItemType;
import com.seyoung5309.executorserver.domain.item.entity.UserItem;

import java.time.Instant;

/** 내 보유 아이템 한 개. equipped는 지금 장착 중인 스킨·테마인지 (힌트는 항상 false) */
public record MyItemResponse(
        Long itemId,
        String name,
        ItemType itemType,
        String image,
        int quantity,
        boolean equipped,
        Instant acquiredAt
) {
    public static MyItemResponse of(UserItem userItem, boolean equipped) {
        Item item = userItem.getItem();
        return new MyItemResponse(item.getId(), item.getName(), item.getItemType(), item.getImage(),
                userItem.getQuantity(), equipped, userItem.getAcquiredAt());
    }
}
