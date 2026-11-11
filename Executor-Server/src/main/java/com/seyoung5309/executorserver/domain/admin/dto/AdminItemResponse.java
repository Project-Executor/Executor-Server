package com.seyoung5309.executorserver.domain.admin.dto;

import com.seyoung5309.executorserver.domain.item.entity.Item;
import com.seyoung5309.executorserver.domain.item.entity.ItemType;

import java.time.Instant;

public record AdminItemResponse(
        Long itemId,
        String name,
        String image,
        String description,
        ItemType itemType,
        int price,
        boolean isActive,
        Instant createdAt
) {
    public static AdminItemResponse from(Item item) {
        return new AdminItemResponse(item.getId(), item.getName(), item.getImage(), item.getDescription(),
                item.getItemType(), item.getPrice(), item.isActive(), item.getCreatedAt());
    }
}
