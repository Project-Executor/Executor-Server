package com.seyoung5309.executorserver.domain.stage.dto;

import com.seyoung5309.executorserver.domain.stage.entity.Stage;
import com.seyoung5309.executorserver.domain.stage.entity.StageType;
import com.seyoung5309.executorserver.domain.stage.entity.UserStage;

/**
 * 누적 별 평가.
 * clear = 클리어, par = 최소 기록이 기준 턴 이하, hidden = 숨겨진 요소 달성.
 * 튜토리얼은 clear 하나만 있다 (max = 1).
 */
public record StarsResponse(boolean clear, boolean par, boolean hidden, int count, int max) {

    /** @param record 기록이 없으면 null */
    public static StarsResponse of(Stage stage, UserStage record) {
        boolean tutorial = stage.getStageType() == StageType.TUTORIAL;
        boolean clear = record != null && record.isCleared();
        boolean par = !tutorial && clear
                && record.getBestMoves() != null && record.getBestMoves() <= stage.getParMoves();
        boolean hidden = !tutorial && clear && record.isHiddenFound();
        int count = (clear ? 1 : 0) + (par ? 1 : 0) + (hidden ? 1 : 0);
        return new StarsResponse(clear, par, hidden, count, tutorial ? 1 : 3);
    }
}
