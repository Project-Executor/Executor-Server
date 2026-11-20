package com.seyoung5309.executorserver.domain.admin.dto;

/** @param sentCount 실제로 보낸 메일 수 (받는 사람 수) */
public record AdminMailResponse(int sentCount) {
}
