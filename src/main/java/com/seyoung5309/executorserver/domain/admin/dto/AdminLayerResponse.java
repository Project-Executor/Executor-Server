package com.seyoung5309.executorserver.domain.admin.dto;

import com.seyoung5309.executorserver.domain.stage.entity.Layer;

/** @param operationCount 소속 작전 수 (0이어야 삭제할 수 있다) */
public record AdminLayerResponse(Long layerId, String name, String description, int sortOrder, long operationCount) {

    public static AdminLayerResponse of(Layer layer, long operationCount) {
        return new AdminLayerResponse(layer.getId(), layer.getName(), layer.getDescription(), layer.getSortOrder(), operationCount);
    }
}
