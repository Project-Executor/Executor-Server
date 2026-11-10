package com.seyoung5309.executorserver.domain.stage.controller;

import com.seyoung5309.executorserver.domain.stage.dto.StageDetailResponse;
import com.seyoung5309.executorserver.domain.stage.service.StageService;
import com.seyoung5309.executorserver.global.security.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "스테이지", description = "작전·스테이지 목록, 상세, 결과 제출, 순위")
@RestController
@RequestMapping("/api/stages")
@RequiredArgsConstructor
public class StageController {

    private final StageService stageService;

    @Operation(summary = "스테이지 상세",
            description = "공식 스테이지와 사용자 맵 모두 이 API로 불러온다. mapData에서 hints는 빠지고 개수만 hintCount로 준다. "
                    + "잠긴 공식 스테이지는 403 STAGE_LOCKED, 남의 비공개 맵은 403 FORBIDDEN.")
    @GetMapping("/{stageId}")
    public StageDetailResponse getStage(@LoginUser Long userId, @PathVariable Long stageId) {
        return stageService.getStageDetail(userId, stageId);
    }
}
