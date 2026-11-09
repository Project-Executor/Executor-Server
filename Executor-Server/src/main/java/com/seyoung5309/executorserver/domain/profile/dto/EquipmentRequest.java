package com.seyoung5309.executorserver.domain.profile.dto;

/**
 * 장착할 스킨·테마. PUT이므로 두 값을 모두 보낸다. null이면 장착 해제(기본값).
 */
public record EquipmentRequest(Long skinItemId, Long themeItemId) {
}
