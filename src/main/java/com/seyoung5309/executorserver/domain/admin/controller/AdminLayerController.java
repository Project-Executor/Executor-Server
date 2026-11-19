package com.seyoung5309.executorserver.domain.admin.controller;

import com.seyoung5309.executorserver.domain.admin.dto.AdminLayerRequest;
import com.seyoung5309.executorserver.domain.admin.dto.AdminLayerResponse;
import com.seyoung5309.executorserver.domain.admin.service.AdminLayerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "관리자 - 레이어", description = "ADMIN 권한 필요. 레이어(대주제) 안에 작전(소주제)이 들어간다.")
@RestController
@RequestMapping("/api/admin/layers")
@RequiredArgsConstructor
public class AdminLayerController {

    private final AdminLayerService adminLayerService;

    @Operation(summary = "레이어 전체 목록", description = "순서대로. operationCount는 소속 작전 수.")
    @GetMapping
    public List<AdminLayerResponse> getAll() {
        return adminLayerService.getAll();
    }

    @Operation(summary = "레이어 등록", description = "이름과 sortOrder는 다른 레이어와 겹칠 수 없다(409 DUPLICATE_VALUE).")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminLayerResponse create(@Valid @RequestBody AdminLayerRequest request) {
        return adminLayerService.create(request);
    }

    @Operation(summary = "레이어 수정", description = "모든 값을 보낸다. 순서를 바꾸면 해금 순서도 바뀐다.")
    @PutMapping("/{layerId}")
    public AdminLayerResponse update(@PathVariable Long layerId, @Valid @RequestBody AdminLayerRequest request) {
        return adminLayerService.update(layerId, request);
    }

    @Operation(summary = "레이어 삭제", description = "작전이 남아 있으면 409 LAYER_HAS_OPERATIONS.")
    @DeleteMapping("/{layerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long layerId) {
        adminLayerService.delete(layerId);
    }
}
