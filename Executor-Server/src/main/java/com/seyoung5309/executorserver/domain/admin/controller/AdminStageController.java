package com.seyoung5309.executorserver.domain.admin.controller;

import com.seyoung5309.executorserver.domain.admin.dto.AdminStageRequest;
import com.seyoung5309.executorserver.domain.admin.dto.AdminStageResponse;
import com.seyoung5309.executorserver.domain.admin.service.AdminStageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "관리자 - 스테이지", description = "ADMIN 권한 필요. 공식 스테이지만 다룬다.")
@RestController
@RequestMapping("/api/admin/stages")
@RequiredArgsConstructor
public class AdminStageController {

    private final AdminStageService adminStageService;

    @Operation(summary = "공식 스테이지 상세", description = "사용자용과 달리 mapData에 hints가 그대로 들어 있다.")
    @GetMapping("/{stageId}")
    public AdminStageResponse get(@PathVariable Long stageId) {
        return adminStageService.get(stageId);
    }

    @Operation(summary = "공식 스테이지 등록",
            description = "mapData는 JSON 객체 그대로 보낸다(hints가 있으면 배열). 항상 공개 상태로 만든다. "
                    + "같은 작전에 같은 stageNo가 있으면 409 DUPLICATE_VALUE.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminStageResponse create(@Valid @RequestBody AdminStageRequest request) {
        return adminStageService.create(request);
    }

    @Operation(summary = "공식 스테이지 수정", description = "모든 값을 보낸다. 사용자들의 기록은 유지한다.")
    @PutMapping("/{stageId}")
    public AdminStageResponse update(@PathVariable Long stageId, @Valid @RequestBody AdminStageRequest request) {
        return adminStageService.update(stageId, request);
    }

    @Operation(summary = "공식 스테이지 삭제", description = "사용자 기록도 함께 삭제되고, 도감의 stageId는 null이 된다.")
    @DeleteMapping("/{stageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long stageId) {
        adminStageService.delete(stageId);
    }
}
