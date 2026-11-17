package com.seyoung5309.executorserver.domain.admin.controller;

import com.seyoung5309.executorserver.domain.admin.dto.AdminOperationRequest;
import com.seyoung5309.executorserver.domain.admin.dto.AdminOperationResponse;
import com.seyoung5309.executorserver.domain.admin.dto.AdminOperationTreeResponse;
import com.seyoung5309.executorserver.domain.admin.service.AdminOperationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "관리자 - 작전", description = "ADMIN 권한 필요")
@RestController
@RequestMapping("/api/admin/operations")
@RequiredArgsConstructor
public class AdminOperationController {

    private final AdminOperationService adminOperationService;

    @Operation(summary = "작전 전체 목록",
            description = "표시 순서대로. 작전마다 소속 공식 스테이지 요약(번호 순, 맵 데이터 제외)을 담는다. 해금과 상관없이 전부 나온다.")
    @GetMapping
    public List<AdminOperationTreeResponse> getAll() {
        return adminOperationService.getAll();
    }

    @Operation(summary = "작전 등록", description = "이름과 sortOrder는 다른 작전과 겹칠 수 없다(409 DUPLICATE_VALUE).")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminOperationResponse create(@Valid @RequestBody AdminOperationRequest request) {
        return adminOperationService.create(request);
    }

    @Operation(summary = "작전 수정", description = "모든 값을 보낸다.")
    @PutMapping("/{operationId}")
    public AdminOperationResponse update(@PathVariable Long operationId,
                                         @Valid @RequestBody AdminOperationRequest request) {
        return adminOperationService.update(operationId, request);
    }

    @Operation(summary = "작전 삭제", description = "소속 스테이지가 있으면 409 OPERATION_HAS_STAGES.")
    @DeleteMapping("/{operationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long operationId) {
        adminOperationService.delete(operationId);
    }
}
