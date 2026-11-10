package com.seyoung5309.executorserver.domain.item.dto;

/**
 * 구매 요청. 본문이 없거나 quantity가 없으면 1개.
 * 스킨·테마는 quantity를 무시하고 1개로 처리하고, 힌트는 1~99개만 허용한다 (서비스에서 검사).
 */
public record PurchaseRequest(Integer quantity) {
}
