package com.seyoung5309.executorserver.domain.stage.dto;

import com.seyoung5309.executorserver.domain.stage.entity.Layer;
import com.seyoung5309.executorserver.domain.stage.entity.Operation;

import java.util.List;

public record OperationResponse(
        Long operationId,
        String name,
        String description,
        /** 레이어 안에서의 순서 */
        int sortOrder,
        /** 소속 레이어 (스토리 대주제) */
        LayerSummary layer,
        List<StageSummaryResponse> stages
) {
    public record LayerSummary(Long layerId, String name, String description, int sortOrder) {

        public static LayerSummary from(Layer layer) {
            return new LayerSummary(layer.getId(), layer.getName(), layer.getDescription(), layer.getSortOrder());
        }
    }

    /** operation.layer가 읽혀 있어야 한다 (fetch join) */
    public static OperationResponse of(Operation operation, List<StageSummaryResponse> stages) {
        return new OperationResponse(
                operation.getId(),
                operation.getName(),
                operation.getDescription(),
                operation.getSortOrder(),
                LayerSummary.from(operation.getLayer()),
                stages
        );
    }
}
