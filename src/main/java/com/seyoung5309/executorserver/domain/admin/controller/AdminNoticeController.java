package com.seyoung5309.executorserver.domain.admin.controller;

import com.seyoung5309.executorserver.domain.admin.dto.AdminNoticeRequest;
import com.seyoung5309.executorserver.domain.admin.dto.AdminNoticeResponse;
import com.seyoung5309.executorserver.domain.admin.service.AdminNoticeService;
import com.seyoung5309.executorserver.global.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "관리자 - 공지", description = "ADMIN 권한 필요. 숨김·예약·종료 공지까지 모두 다룬다.")
@RestController
@RequestMapping("/api/admin/notices")
@RequiredArgsConstructor
public class AdminNoticeController {

    private final AdminNoticeService adminNoticeService;

    @Operation(summary = "공지 전체 목록", description = "최근 작성순(size 1~50, 기본 20). isShown은 지금 게임에 보이는지.")
    @GetMapping
    public PageResponse<AdminNoticeResponse> getAll(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page는 0 이상이어야 합니다.") int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "size는 1 이상이어야 합니다.")
            @Max(value = 50, message = "size는 50 이하여야 합니다.")
            int size) {
        return adminNoticeService.getAll(page, size);
    }

    @Operation(summary = "공지 한 건")
    @GetMapping("/{noticeId}")
    public AdminNoticeResponse get(@PathVariable Long noticeId) {
        return adminNoticeService.get(noticeId);
    }

    @Operation(summary = "공지 등록",
            description = "publishedAt을 생략하면 지금 게시, 미래면 예약. endsAt은 null이면 계속. isPinned 기본 false, isVisible 기본 true.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminNoticeResponse create(@Valid @RequestBody AdminNoticeRequest request) {
        return adminNoticeService.create(request);
    }

    @Operation(summary = "공지 수정", description = "모든 값을 보낸다. publishedAt을 생략하면 기존 값 유지.")
    @PutMapping("/{noticeId}")
    public AdminNoticeResponse update(@PathVariable Long noticeId, @Valid @RequestBody AdminNoticeRequest request) {
        return adminNoticeService.update(noticeId, request);
    }

    @Operation(summary = "공지 삭제", description = "되돌릴 수 없다. 잠시 내리려면 isVisible = false로 수정한다.")
    @DeleteMapping("/{noticeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long noticeId) {
        adminNoticeService.delete(noticeId);
    }
}
