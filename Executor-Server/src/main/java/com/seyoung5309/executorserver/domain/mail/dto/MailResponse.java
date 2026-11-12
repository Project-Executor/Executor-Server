package com.seyoung5309.executorserver.domain.mail.dto;

import com.seyoung5309.executorserver.domain.item.entity.Item;
import com.seyoung5309.executorserver.domain.item.entity.ItemType;
import com.seyoung5309.executorserver.domain.mail.entity.Mail;

import java.time.Instant;

/**
 * 우편 목록의 우편 한 통.
 *
 * @param rewardItem 첨부 아이템이 없으면 null
 * @param expiresAt  null이면 무기한
 */
public record MailResponse(
        Long mailId,
        String title,
        String content,
        int rewardCoin,
        RewardItem rewardItem,
        int rewardItemQuantity,
        boolean claimed,
        Instant expiresAt,
        Instant createdAt
) {
    public record RewardItem(Long itemId, String name, ItemType itemType) {
    }

    public static MailResponse from(Mail mail) {
        Item item = mail.getRewardItem();
        return new MailResponse(
                mail.getId(),
                mail.getTitle(),
                mail.getContent(),
                mail.getRewardCoin(),
                item == null ? null : new RewardItem(item.getId(), item.getName(), item.getItemType()),
                mail.getRewardItemQuantity(),
                mail.getClaimedAt() != null,
                mail.getExpiresAt(),
                mail.getCreatedAt());
    }
}
