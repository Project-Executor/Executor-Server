package com.seyoung5309.executorserver.domain.stage.dto;

/**
 * 저장·수정·공개 전환 결과.
 *
 * @param verified 제작자가 이 맵을 클리어했는지. true여야 공개할 수 있다
 */
public record CustomStageStateResponse(Long stageId, boolean isPublic, boolean verified) {
}
