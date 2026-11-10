package com.seyoung5309.executorserver.domain.stage.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 한 판의 결과 (클리어, 게임 오버, 포기 모두 제출).
 *
 * @param cleared                 클리어 여부 (필수)
 * @param moves                   사용한 턴 수. 클리어했으면 필수 (1~9999)
 * @param hiddenFound             숨겨진 버그 처치 / 피스 수집 여부 (기본 false). 클리어했을 때만 반영
 * @param encounteredGuidebookIds 이번 판에서 만난 이상 개체의 도감 ID. 클리어하지 못해도 해금한다
 */
public record StageResultRequest(
        @NotNull(message = "cleared를 입력해 주세요.")
        Boolean cleared,

        @Min(value = 1, message = "moves는 1 이상이어야 합니다.")
        @Max(value = 9999, message = "moves는 9999 이하여야 합니다.")
        Integer moves,

        Boolean hiddenFound,

        @Size(max = 50, message = "encounteredGuidebookIds는 50개 이하여야 합니다.")
        List<@NotNull Long> encounteredGuidebookIds
) {

    public boolean hiddenFoundOrFalse() {
        return Boolean.TRUE.equals(hiddenFound);
    }

    public List<Long> encounteredOrEmpty() {
        return encounteredGuidebookIds == null ? List.of() : encounteredGuidebookIds;
    }
}
