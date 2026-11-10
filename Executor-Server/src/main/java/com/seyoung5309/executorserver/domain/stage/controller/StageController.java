package com.seyoung5309.executorserver.domain.stage.controller;

import com.seyoung5309.executorserver.domain.stage.dto.StageDetailResponse;
import com.seyoung5309.executorserver.domain.stage.dto.StageResultRequest;
import com.seyoung5309.executorserver.domain.stage.dto.StageResultResponse;
import com.seyoung5309.executorserver.domain.stage.service.StageResultService;
import com.seyoung5309.executorserver.domain.stage.service.StageService;
import com.seyoung5309.executorserver.global.security.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "스테이지", description = "작전·스테이지 목록, 상세, 결과 제출, 순위")
@RestController
@RequestMapping("/api/stages")
@RequiredArgsConstructor
public class StageController {

    private final StageService stageService;
    private final StageResultService stageResultService;

    @Operation(summary = "스테이지 상세",
            description = "공식 스테이지와 사용자 맵 모두 이 API로 불러온다. mapData에서 hints는 빠지고 개수만 hintCount로 준다. "
                    + "잠긴 공식 스테이지는 403 STAGE_LOCKED, 남의 비공개 맵은 403 FORBIDDEN.")
    @GetMapping("/{stageId}")
    public StageDetailResponse getStage(@LoginUser Long userId, @PathVariable Long stageId) {
        return stageService.getStageDetail(userId, stageId);
    }

    @Operation(summary = "플레이 결과 제출",
            description = "한 판이 끝날 때마다(클리어, 게임 오버, 포기) 제출한다. 도전 횟수 +1, 클리어 시 기록 갱신. "
                    + "첫 클리어일 때만 보상 코인을 지급한다(공식 스테이지만). 같은 결과를 다시 보내도 보상은 한 번만 나간다.")
    @PostMapping("/{stageId}/results")
    public StageResultResponse submitResult(@LoginUser Long userId,
                                            @PathVariable Long stageId,
                                            @Valid @RequestBody StageResultRequest request) {
        return stageResultService.submit(userId, stageId, request);
    }
}
