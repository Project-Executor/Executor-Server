package com.seyoung5309.executorserver.domain.report.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 신고. 대상은 사용자와 스테이지 중 하나만 지정한다. 버그 제보·기타는 대상을 비워도 된다.
 */
public record CreateReportRequest(
        @NotNull(message = "categoryId를 입력해 주세요.")
        Long categoryId,

        Long targetUserId,

        Long targetStageId,

        @NotBlank(message = "title을 입력해 주세요.")
        @Size(max = 255, message = "title은 255자 이하여야 합니다.")
        String title,

        @NotBlank(message = "comment를 입력해 주세요.")
        @Size(max = 2000, message = "comment는 2000자 이하여야 합니다.")
        String comment
) {
}
