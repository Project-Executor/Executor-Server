package com.seyoung5309.executorserver.domain.admin.dto;

import com.seyoung5309.executorserver.domain.guidebook.entity.GuidebookCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 도감 등록·수정 (PUT이므로 모든 값을 보낸다).
 *
 * @param stageId  처음 등장하는 스테이지. 없으면 null
 * @param language 언어 (예: Java). 없으면 null
 * @param code     코드 예시 (여러 줄). 없으면 null
 * @param remedy   대처 방법 한 줄. 없으면 null
 * @param mapNote  맵에서의 모습. 없으면 null
 */
public record AdminGuidebookRequest(
        Long stageId,

        @NotNull(message = "category를 입력해 주세요. (BUG, VIRUS, TERM)")
        GuidebookCategory category,

        @NotBlank(message = "title을 입력해 주세요.")
        @Size(max = 255, message = "title은 255자 이하여야 합니다.")
        String title,

        @Size(max = 255, message = "image는 255자 이하여야 합니다.")
        String image,

        @NotBlank(message = "comment를 입력해 주세요.")
        @Size(max = 10000, message = "comment는 10000자 이하여야 합니다.")
        String comment,

        @Size(max = 50, message = "language는 50자 이하여야 합니다.")
        String language,

        @Size(max = 2000, message = "code는 2000자 이하여야 합니다.")
        String code,

        @Size(max = 255, message = "remedy는 255자 이하여야 합니다.")
        String remedy,

        @Size(max = 255, message = "mapNote는 255자 이하여야 합니다.")
        String mapNote
) {
}
