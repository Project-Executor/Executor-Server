package com.seyoung5309.executorserver.domain.stage.dto;

import com.seyoung5309.executorserver.domain.stage.entity.Stage;
import com.seyoung5309.executorserver.domain.stage.entity.StageType;
import com.seyoung5309.executorserver.domain.stage.entity.UserStage;

/** 작전 목록 안의 스테이지 한 칸 (내 진행도 포함) */
public record StageSummaryResponse(
        Long stageId,
        Integer stageNo,
        StageType stageType,
        String title,
        int parMoves,
        int rewardCoin,
        boolean unlocked,
        boolean cleared,
        StarsResponse stars,
        Integer bestMoves,
        int playCount
) {

    /** @param record 기록이 없으면 null */
    public static StageSummaryResponse of(Stage stage, UserStage record, boolean unlocked) {
        return new StageSummaryResponse(
                stage.getId(),
                stage.getStageNo(),
                stage.getStageType(),
                stage.getTitle(),
                stage.getParMoves(),
                stage.getRewardCoin(),
                unlocked,
                record != null && record.isCleared(),
                StarsResponse.of(stage, record),
                record == null ? null : record.getBestMoves(),
                record == null ? 0 : record.getPlayCount()
        );
    }
}
