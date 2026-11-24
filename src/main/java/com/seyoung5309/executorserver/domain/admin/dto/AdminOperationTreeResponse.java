package com.seyoung5309.executorserver.domain.admin.dto;

import com.seyoung5309.executorserver.domain.stage.entity.Operation;
import com.seyoung5309.executorserver.domain.stage.entity.Stage;
import com.seyoung5309.executorserver.domain.stage.entity.StageType;

import java.time.Instant;
import java.util.List;

/**
 * 관리자 작전 목록 항목. 소속 공식 스테이지 요약을 함께 담는다 (맵 데이터는 스테이지 상세에서).
 *
 * @param sortOrder 레이어 안에서의 순서
 * @param layerId   소속 레이어
 * @param stages    스테이지 번호 순. boardX/boardY는 스테이지 선택 기판 위치(0~1), null이면 자동 배치
 */
public record AdminOperationTreeResponse(
        Long operationId,
        String name,
        String description,
        int sortOrder,
        Long layerId,
        List<StageSummary> stages
) {
    public record StageSummary(
            Long stageId,
            int stageNo,
            StageType stageType,
            String title,
            int parMoves,
            int rewardCoin,
            Double boardX,
            Double boardY,
            Instant updatedAt
    ) {
        public static StageSummary from(Stage stage) {
            return new StageSummary(stage.getId(), stage.getStageNo(), stage.getStageType(), stage.getTitle(),
                    stage.getParMoves(), stage.getRewardCoin(), stage.getBoardX(), stage.getBoardY(), stage.getUpdatedAt());
        }
    }

    public static AdminOperationTreeResponse of(Operation operation, List<Stage> stages) {
        return new AdminOperationTreeResponse(operation.getId(), operation.getName(), operation.getDescription(),
                operation.getSortOrder(), operation.getLayerId(), stages.stream().map(StageSummary::from).toList());
    }
}
