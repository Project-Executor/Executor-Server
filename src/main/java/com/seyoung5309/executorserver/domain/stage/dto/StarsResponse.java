package com.seyoung5309.executorserver.domain.stage.dto;

import com.seyoung5309.executorserver.domain.stage.entity.Stage;
import com.seyoung5309.executorserver.domain.stage.entity.UserStage;
import com.seyoung5309.executorserver.domain.stage.service.StarRules;

/**
 * 누적 별 평가.
 * clear = 별 1 (클리어), par = 별 2 조건을 달성한 적 있음, hidden = 별 3 조건을 달성한 적 있음.
 * 별 2·3의 조건은 맵마다 다르다 (map_data의 stars, 게임이 판정). par·hidden이라는 이름은 클라이언트 호환을 위해 그대로 둔다.
 * max = 이 스테이지의 별 수 (튜토리얼 1, 그 밖에는 stars.count 1~3, 기본 3). max보다 높은 별은 세지 않는다.
 */
public record StarsResponse(boolean clear, boolean par, boolean hidden, int count, int max) {

    /** @param record 기록이 없으면 null */
    public static StarsResponse of(Stage stage, UserStage record) {
        int max = StarRules.starCount(stage);
        boolean clear = record != null && record.isCleared();
        boolean par = max >= 2 && clear && record.isStableCleared();
        boolean hidden = max >= 3 && clear && record.isHiddenFound();
        int count = (clear ? 1 : 0) + (par ? 1 : 0) + (hidden ? 1 : 0);
        return new StarsResponse(clear, par, hidden, count, max);
    }
}
