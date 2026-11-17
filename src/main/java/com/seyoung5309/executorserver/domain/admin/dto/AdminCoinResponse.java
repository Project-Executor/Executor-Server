package com.seyoung5309.executorserver.domain.admin.dto;

/** 코인 조정 결과. coin은 조정한 뒤의 보유 코인 */
public record AdminCoinResponse(Long userId, int coin) {
}
