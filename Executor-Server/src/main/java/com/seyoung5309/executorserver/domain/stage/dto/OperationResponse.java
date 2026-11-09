package com.seyoung5309.executorserver.domain.stage.dto;

import com.seyoung5309.executorserver.domain.stage.entity.Operation;

import java.util.List;

public record OperationResponse(
        Long operationId,
        String name,
        String description,
        int sortOrder,
        List<StageSummaryResponse> stages
) {

    public static OperationResponse of(Operation operation, List<StageSummaryResponse> stages) {
        return new OperationResponse(
                operation.getId(),
                operation.getName(),
                operation.getDescription(),
                operation.getSortOrder(),
                stages
        );
    }
}
