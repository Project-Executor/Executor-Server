package com.seyoung5309.executorserver.domain.item.dto;

import com.seyoung5309.executorserver.domain.item.entity.Item;
import com.seyoung5309.executorserver.domain.item.entity.ItemType;

/** 상점 목록의 아이템 한 개. 보유하지 않았으면 owned = false, quantity = 0 */
public record ShopItemResponse(
        Long itemId,
        String name,
        String image,
        String description,
        ItemType itemType,
        int price,
        boolean owned,
        int quantity
) {
    public static ShopItemResponse of(Item item, int ownedQuantity) {
        return new ShopItemResponse(item.getId(), item.getName(), item.getImage(), item.getDescription(),
                item.getItemType(), item.getPrice(), ownedQuantity > 0, ownedQuantity);
    }
}
