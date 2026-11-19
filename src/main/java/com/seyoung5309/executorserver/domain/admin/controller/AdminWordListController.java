package com.seyoung5309.executorserver.domain.admin.controller;

import com.seyoung5309.executorserver.domain.admin.dto.AdminWordListRequest;
import com.seyoung5309.executorserver.domain.admin.dto.AdminWordListResponse;
import com.seyoung5309.executorserver.domain.admin.service.AdminWordListService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "관리자 - 단어 목록", description = "ADMIN 권한 필요. 감염된 컴퓨터 타자 해킹에 쓰는 단어 목록.")
@RestController
@RequestMapping("/api/admin/word-lists")
@RequiredArgsConstructor
public class AdminWordListController {

    private final AdminWordListService adminWordListService;

    @Operation(summary = "단어 목록 전체", description = "ID 순. 단어까지 함께.")
    @GetMapping
    public List<AdminWordListResponse> getAll() {
        return adminWordListService.getAll();
    }

    @Operation(summary = "단어 목록 등록", description = "단어는 앞뒤 공백을 지우고 빈 줄·중복을 뺀다. 이름이 겹치면 409 DUPLICATE_VALUE.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminWordListResponse create(@Valid @RequestBody AdminWordListRequest request) {
        return adminWordListService.create(request);
    }

    @Operation(summary = "단어 목록 수정", description = "모든 값을 보낸다. 단어는 통째로 바뀐다.")
    @PutMapping("/{wordListId}")
    public AdminWordListResponse update(@PathVariable Long wordListId, @Valid @RequestBody AdminWordListRequest request) {
        return adminWordListService.update(wordListId, request);
    }

    @Operation(summary = "단어 목록 삭제", description = "맵이 쓰고 있어도 지운다 (그 맵의 컴퓨터는 게임이 기본 단어로 바꿔 쓴다).")
    @DeleteMapping("/{wordListId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long wordListId) {
        adminWordListService.delete(wordListId);
    }
}
