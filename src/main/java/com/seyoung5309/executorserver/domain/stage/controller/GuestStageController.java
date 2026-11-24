package com.seyoung5309.executorserver.domain.stage.controller;

import com.seyoung5309.executorserver.domain.stage.dto.OperationResponse;
import com.seyoung5309.executorserver.domain.stage.dto.StageDetailResponse;
import com.seyoung5309.executorserver.domain.stage.service.StageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 게스트로 플레이 (로그인 없이, 전시용). 관리자가 등록한 공식 스테이지를 받기만 하고 결과는 저장하지 않는다.
 */
@Tag(name = "게스트", description = "로그인 없이 공식 스테이지 받기 (게스트로 플레이)")
@RestController
@RequestMapping("/api/guest")
@RequiredArgsConstructor
public class GuestStageController {

    private final StageService stageService;

    @Operation(summary = "작전·스테이지 목록 (게스트)", description = "기록 없이, 모든 스테이지를 열린 것으로 돌려준다. 해금은 게임이 정한다.")
    @GetMapping("/operations")
    public List<OperationResponse> getOperations() {
        return stageService.getGuestOperations();
    }

    @Operation(summary = "스테이지 상세 (게스트)", description = "공식 스테이지만. 사용자 맵이나 없는 스테이지는 404.")
    @GetMapping("/stages/{stageId}")
    public StageDetailResponse getStage(@PathVariable Long stageId) {
        return stageService.getGuestStageDetail(stageId);
    }
}
