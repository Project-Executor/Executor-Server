package com.seyoung5309.executorserver.domain.admin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 작전 안 스테이지들의 스테이지 선택 기판 위치를 한 번에 바꾼다.
 * 목록에 없는 스테이지는 그대로 둔다.
 *
 * @param positions 스테이지별 위치. x, y는 노드 가운데의 비율 좌표(0~1, 왼쪽 위 기준)이고 둘 다 null이면 자동 배치로 되돌린다
 */
public record AdminStageBoardRequest(
        @NotNull(message = "positions를 보내 주세요.")
        List<@Valid @NotNull(message = "positions 항목이 비어 있습니다.") Position> positions
) {
    public record Position(
            @NotNull(message = "stageId를 입력해 주세요.")
            Long stageId,

            @DecimalMin(value = "0.0", message = "x는 0 이상이어야 합니다.")
            @DecimalMax(value = "1.0", message = "x는 1 이하여야 합니다.")
            Double x,

            @DecimalMin(value = "0.0", message = "y는 0 이상이어야 합니다.")
            @DecimalMax(value = "1.0", message = "y는 1 이하여야 합니다.")
            Double y
    ) {
    }
}
