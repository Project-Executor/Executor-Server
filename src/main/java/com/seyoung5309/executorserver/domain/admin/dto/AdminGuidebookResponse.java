package com.seyoung5309.executorserver.domain.admin.dto;

import com.seyoung5309.executorserver.domain.guidebook.entity.Guidebook;
import com.seyoung5309.executorserver.domain.guidebook.entity.GuidebookCategory;

public record AdminGuidebookResponse(
        Long guidebookId,
        Long stageId,
        GuidebookCategory category,
        String title,
        String image,
        String comment,
        String language,
        String code,
        String remedy,
        String mapNote
) {
    public static AdminGuidebookResponse from(Guidebook guidebook) {
        return new AdminGuidebookResponse(
                guidebook.getId(),
                guidebook.getStage() == null ? null : guidebook.getStage().getId(),
                guidebook.getCategory(),
                guidebook.getTitle(),
                guidebook.getImage(),
                guidebook.getComment(),
                guidebook.getLanguage(),
                guidebook.getCode(),
                guidebook.getRemedy(),
                guidebook.getMapNote());
    }
}
