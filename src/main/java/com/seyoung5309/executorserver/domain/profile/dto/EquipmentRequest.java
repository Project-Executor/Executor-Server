package com.seyoung5309.executorserver.domain.profile.dto;

/**
 * 장착할 스킨·테마·커서·델타 꾸미기. PUT이므로 모든 값을 보낸다. null(또는 빠진 값)이면 장착 해제(기본값).
 */
public record EquipmentRequest(Long skinItemId, Long themeItemId, Long cursorItemId, Long deltaItemId) {
}
