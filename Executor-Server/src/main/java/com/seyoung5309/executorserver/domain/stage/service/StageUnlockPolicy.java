package com.seyoung5309.executorserver.domain.stage.service;

import com.seyoung5309.executorserver.domain.stage.entity.Stage;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 공식 스테이지 해금 규칙.
 * 공식 스테이지를 플레이 순서(작전 sortOrder -> stageNo)로 한 줄로 세웠을 때, 아래 중 하나면 열린다.
 * <ol>
 *   <li>첫 번째 스테이지</li>
 *   <li>바로 앞 스테이지를 클리어함</li>
 *   <li>이미 클리어한 스테이지 (관리자가 중간에 스테이지를 끼워 넣어도 깬 스테이지가 다시 잠기지 않도록)</li>
 * </ol>
 * 사용자 맵에는 적용하지 않는다 (공개된 맵은 누구나 플레이).
 */
public final class StageUnlockPolicy {

    private StageUnlockPolicy() {
    }

    /**
     * @param officialStagesInPlayOrder 공식 스테이지 전체 (플레이 순서대로 정렬된 상태)
     * @param clearedStageIds           사용자가 클리어한 스테이지 ID
     * @return 열린 스테이지 ID
     */
    public static Set<Long> unlockedStageIds(List<Stage> officialStagesInPlayOrder, Set<Long> clearedStageIds) {
        Set<Long> unlocked = new HashSet<>();
        Long previousId = null;
        for (Stage stage : officialStagesInPlayOrder) {
            boolean isFirst = previousId == null;
            boolean previousCleared = previousId != null && clearedStageIds.contains(previousId);
            boolean alreadyCleared = clearedStageIds.contains(stage.getId());
            if (isFirst || previousCleared || alreadyCleared) {
                unlocked.add(stage.getId());
            }
            previousId = stage.getId();
        }
        return unlocked;
    }
}
