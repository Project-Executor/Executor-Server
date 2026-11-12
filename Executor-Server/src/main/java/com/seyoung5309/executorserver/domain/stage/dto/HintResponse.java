package com.seyoung5309.executorserver.domain.stage.dto;

import tools.jackson.databind.JsonNode;

/**
 * @param content            map_data의 hints[hintIndex] 그대로. 보통 문자열이지만 Unity가 객체로 정하면 객체로 나간다
 * @param remainingHintItems 쓰고 남은 힌트 아이템 수 (힌트 종류가 여러 개면 합친 값)
 */
public record HintResponse(int hintIndex, JsonNode content, long remainingHintItems) {
}
