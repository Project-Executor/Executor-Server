package com.seyoung5309.executorserver.domain.stage.dto;

import com.seyoung5309.executorserver.domain.stage.entity.Stage;
import com.seyoung5309.executorserver.domain.stage.entity.StageType;
import com.seyoung5309.executorserver.domain.stage.entity.UserStage;

/**
 * 누적 별 평가.
 * clear = 클리어, par = 별 2 (불안정 체크포인트 허용 개수 이하로 클리어한 적 있음), hidden = 숨겨진 요소 달성.
 * par라는 이름은 클라이언트 호환을 위해 그대로 둔다 (예전 기준: 최소 기록 <= 기준 턴).
 * 튜토리얼은 clear 하나만 있다 (max = 1).
 */
public record StarsResponse(boolean clear, boolean par, boolean hidden, int count, int max) {

    /** @param record 기록이 없으면 null */
    public static StarsResponse of(Stage stage, UserStage record) {
        boolean tutorial = stage.getStageType() == StageType.TUTORIAL;
        boolean clear = record != null && record.isCleared();
        boolean par = !tutorial && clear && record.isStableCleared();
        boolean hidden = !tutorial && clear && record.isHiddenFound();
        int count = (clear ? 1 : 0) + (par ? 1 : 0) + (hidden ? 1 : 0);
        return new StarsResponse(clear, par, hidden, count, tutorial ? 1 : 3);
    }
}
