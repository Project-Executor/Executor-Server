package com.seyoung5309.executorserver.domain.notice.controller;

import com.seyoung5309.executorserver.domain.notice.dto.NoticeDetailResponse;
import com.seyoung5309.executorserver.domain.notice.dto.NoticeSummaryResponse;
import com.seyoung5309.executorserver.domain.notice.service.NoticeService;
import com.seyoung5309.executorserver.global.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "공지사항", description = "로그인 없이 볼 수 있다 (시작 화면에서도 보여 줄 수 있게)")
@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @Operation(summary = "공지 목록",
            description = "지금 게시 중인 공지만. 상단 고정 먼저, 그다음 게시 시각 최신순(size 1~50, 기본 20). 내용은 상세에서.")
    @GetMapping
    public PageResponse<NoticeSummaryResponse> getNotices(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page는 0 이상이어야 합니다.") int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "size는 1 이상이어야 합니다.")
            @Max(value = 50, message = "size는 50 이하여야 합니다.")
            int size) {
        return noticeService.getNotices(page, size);
    }

    @Operation(summary = "공지 상세", description = "숨겼거나 게시 기간이 아닌 공지는 404.")
    @GetMapping("/{noticeId}")
    public NoticeDetailResponse getNotice(@PathVariable Long noticeId) {
        return noticeService.getNotice(noticeId);
    }
}
