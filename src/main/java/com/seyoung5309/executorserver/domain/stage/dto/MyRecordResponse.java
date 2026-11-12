package com.seyoung5309.executorserver.domain.stage.dto;

import com.seyoung5309.executorserver.domain.stage.entity.Stage;
import com.seyoung5309.executorserver.domain.stage.entity.UserStage;

/** 스테이지 상세 안의 내 기록 */
public record MyRecordResponse(boolean cleared, StarsResponse stars, Integer bestMoves, int playCount) {

    /** @param record 기록이 없으면 null */
    public static MyRecordResponse of(Stage stage, UserStage record) {
        return new MyRecordResponse(
                record != null && record.isCleared(),
                StarsResponse.of(stage, record),
                record == null ? null : record.getBestMoves(),
                record == null ? 0 : record.getPlayCount()
        );
    }
}
