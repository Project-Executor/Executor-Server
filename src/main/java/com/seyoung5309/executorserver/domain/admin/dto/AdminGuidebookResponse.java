package com.seyoung5309.executorserver.domain.admin.dto;

import com.seyoung5309.executorserver.domain.guidebook.entity.Guidebook;
import com.seyoung5309.executorserver.domain.guidebook.entity.GuidebookCategory;

public record AdminGuidebookResponse(
        Long guidebookId,
        Long stageId,
        GuidebookCategory category,
        String title,
        String image,
        String comment
) {
    public static AdminGuidebookResponse from(Guidebook guidebook) {
        return new AdminGuidebookResponse(
                guidebook.getId(),
                guidebook.getStage() == null ? null : guidebook.getStage().getId(),
                guidebook.getCategory(),
                guidebook.getTitle(),
                guidebook.getImage(),
                guidebook.getComment());
    }
}
