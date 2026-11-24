package com.seyoung5309.executorserver.domain.guestbook.dto;

/**
 * 남긴 글과 보상.
 *
 * @param rewardCoin 이번에 받은 코인 (로그인한 사용자의 그날 첫 글이면 10, 아니면 0)
 * @param totalCoin  보상을 받았으면 받은 뒤 보유 코인, 아니면 null
 */
public record GuestbookPostResponse(GuestbookEntryResponse entry, int rewardCoin, Integer totalCoin) {
}
