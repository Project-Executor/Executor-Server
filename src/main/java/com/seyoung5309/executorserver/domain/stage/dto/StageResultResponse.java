package com.seyoung5309.executorserver.domain.stage.dto;

import com.seyoung5309.executorserver.domain.guidebook.entity.Guidebook;
import com.seyoung5309.executorserver.domain.guidebook.entity.GuidebookCategory;

import java.util.List;

/**
 * 결과 제출 응답.
 * <ul>
 *   <li>thisRun: 이번 판의 별 조건 달성 여부</li>
 *   <li>stars: 지금까지 누적된 별</li>
 *   <li>firstClear가 false면 rewardCoin은 0</li>
 *   <li>nextStageId: 클리어한 공식 스테이지의 다음 스테이지 (없으면 null)</li>
 * </ul>
 */
public record StageResultResponse(
        boolean cleared,
        RunResult thisRun,
        StarsResponse stars,
        Integer bestMoves,
        boolean isNewBest,
        boolean firstClear,
        int rewardCoin,
        int totalCoin,
        List<UnlockedGuidebook> unlockedGuidebooks,
        Long nextStageId
) {

    public record RunResult(boolean clear, boolean par, boolean hidden) {
    }

    public record UnlockedGuidebook(Long guidebookId, GuidebookCategory category, String title) {

        public static UnlockedGuidebook from(Guidebook guidebook) {
            return new UnlockedGuidebook(guidebook.getId(), guidebook.getCategory(), guidebook.getTitle());
        }
    }
}
