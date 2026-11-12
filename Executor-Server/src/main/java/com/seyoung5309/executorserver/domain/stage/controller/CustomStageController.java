package com.seyoung5309.executorserver.domain.stage.controller;

import com.seyoung5309.executorserver.domain.stage.dto.*;
import com.seyoung5309.executorserver.domain.stage.service.CustomStageService;
import com.seyoung5309.executorserver.global.response.PageResponse;
import com.seyoung5309.executorserver.global.security.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "사용자 맵", description = "맵 에디터로 만든 맵 저장·공개·관리. 플레이는 /api/stages API를 그대로 쓴다.")
@RestController
@RequestMapping("/api/custom-stages")
@RequiredArgsConstructor
public class CustomStageController {

    private final CustomStageService customStageService;

    @Operation(summary = "공개된 사용자 맵 목록", description = "최신순(size 1~50, 기본 20). 클리어한 사람 수와 내 클리어 여부 포함.")
    @GetMapping
    public PageResponse<PublicCustomStageResponse> getPublicMaps(
            @LoginUser Long userId,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page는 0 이상이어야 합니다.") int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "size는 1 이상이어야 합니다.")
            @Max(value = 50, message = "size는 50 이하여야 합니다.")
            int size) {
        return customStageService.getPublicMaps(userId, page, size);
    }

    @Operation(summary = "내가 만든 맵", description = "비공개 포함, 최근 수정 순. verified는 내가 클리어했는지(공개 조건).")
    @GetMapping("/me")
    public List<MyCustomStageResponse> getMyMaps(@LoginUser Long userId) {
        return customStageService.getMyMaps(userId);
    }

    @Operation(summary = "맵 저장", description = "비공개로 저장한다. mapData는 JSON 객체(최대 256KB). 보상 코인은 항상 0.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomStageStateResponse create(@LoginUser Long userId, @Valid @RequestBody CustomStageRequest request) {
        return customStageService.create(userId, request);
    }

    @Operation(summary = "맵 수정",
            description = "비공개 맵만(공개 중이면 409 PUBLISHED_STAGE). 수정하면 이 맵의 기록이 모두 지워져 다시 클리어해야 공개할 수 있다.")
    @PutMapping("/{stageId}")
    public CustomStageStateResponse update(@LoginUser Long userId, @PathVariable Long stageId,
                                           @Valid @RequestBody CustomStageRequest request) {
        return customStageService.update(userId, stageId, request);
    }

    @Operation(summary = "공개 / 비공개 전환", description = "공개하려면 내가 클리어한 기록이 있어야 한다(아니면 409 NOT_VERIFIED).")
    @PatchMapping("/{stageId}/visibility")
    public CustomStageStateResponse changeVisibility(@LoginUser Long userId, @PathVariable Long stageId,
                                                     @Valid @RequestBody VisibilityRequest request) {
        return customStageService.changeVisibility(userId, stageId, request.isPublic());
    }

    @Operation(summary = "맵 삭제", description = "플레이 기록도 함께 삭제된다. 이 맵에 대한 신고는 남는다.")
    @DeleteMapping("/{stageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@LoginUser Long userId, @PathVariable Long stageId) {
        customStageService.delete(userId, stageId);
    }
}
