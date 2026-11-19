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
 * @param moves                   기록 (구간 최고 기록의 합, docs/map-data.md 3.3). 클리어했으면 필수 (1~9999)
 * @param hiddenFound             별 3 조건 달성 여부 (기본 false). 조건은 맵의 stars.bonus로 게임이 판정한다
 *                                (숨겨진 요소 모두 수집 또는 컴퓨터를 시간 안에 복구). 클리어했을 때만 반영
 * @param encounteredGuidebookIds 이번 판에서 만난 이상 개체의 도감 ID. 클리어하지 못해도 해금한다
 * @param unstableCount           불안정 체크포인트(출구 포함) 수 (기본 0). 별 2 판정에 쓴다
 * @param rewinds                 워프한 횟수 (통계용, 지금은 저장하지 않는다)
 */
public record StageResultRequest(
        @NotNull(message = "cleared를 입력해 주세요.")
        Boolean cleared,

        @Min(value = 1, message = "moves는 1 이상이어야 합니다.")
        @Max(value = 9999, message = "moves는 9999 이하여야 합니다.")
        Integer moves,

        Boolean hiddenFound,

        @Size(max = 50, message = "encounteredGuidebookIds는 50개 이하여야 합니다.")
        List<@NotNull Long> encounteredGuidebookIds,

        @Min(value = 0, message = "unstableCount는 0 이상이어야 합니다.")
        @Max(value = 999, message = "unstableCount는 999 이하여야 합니다.")
        Integer unstableCount,

        @Min(value = 0, message = "rewinds는 0 이상이어야 합니다.")
        Integer rewinds
) {

    public int unstableCountOrZero() {
        return unstableCount == null ? 0 : unstableCount;
    }

    public boolean hiddenFoundOrFalse() {
        return Boolean.TRUE.equals(hiddenFound);
    }

    public List<Long> encounteredOrEmpty() {
        return encounteredGuidebookIds == null ? List.of() : encounteredGuidebookIds;
    }
}
