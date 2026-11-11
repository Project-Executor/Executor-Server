package com.seyoung5309.executorserver.domain.admin.controller;

import com.seyoung5309.executorserver.domain.admin.dto.AdminGuidebookRequest;
import com.seyoung5309.executorserver.domain.admin.dto.AdminGuidebookResponse;
import com.seyoung5309.executorserver.domain.admin.service.AdminGuidebookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "관리자 - 도감", description = "ADMIN 권한 필요")
@RestController
@RequestMapping("/api/admin/guidebooks")
@RequiredArgsConstructor
public class AdminGuidebookController {

    private final AdminGuidebookService adminGuidebookService;

    @Operation(summary = "도감 등록", description = "stageId는 선택. 이름은 겹칠 수 없다(409 DUPLICATE_VALUE).")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminGuidebookResponse create(@Valid @RequestBody AdminGuidebookRequest request) {
        return adminGuidebookService.create(request);
    }

    @Operation(summary = "도감 수정", description = "모든 값을 보낸다.")
    @PutMapping("/{guidebookId}")
    public AdminGuidebookResponse update(@PathVariable Long guidebookId,
                                         @Valid @RequestBody AdminGuidebookRequest request) {
        return adminGuidebookService.update(guidebookId, request);
    }

    @Operation(summary = "도감 삭제", description = "사용자의 해금 기록도 함께 삭제된다.")
    @DeleteMapping("/{guidebookId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long guidebookId) {
        adminGuidebookService.delete(guidebookId);
    }
}
