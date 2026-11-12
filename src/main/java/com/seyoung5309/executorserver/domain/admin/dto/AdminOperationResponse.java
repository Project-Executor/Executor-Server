package com.seyoung5309.executorserver.domain.admin.dto;

import com.seyoung5309.executorserver.domain.stage.entity.Operation;

public record AdminOperationResponse(Long operationId, String name, String description, int sortOrder) {

    public static AdminOperationResponse from(Operation operation) {
        return new AdminOperationResponse(operation.getId(), operation.getName(), operation.getDescription(),
                operation.getSortOrder());
    }
}
