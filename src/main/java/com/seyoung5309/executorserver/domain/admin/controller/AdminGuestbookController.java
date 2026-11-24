package com.seyoung5309.executorserver.domain.admin.controller;

import com.seyoung5309.executorserver.domain.guestbook.dto.GuestbookEntryResponse;
import com.seyoung5309.executorserver.domain.guestbook.service.GuestbookService;
import com.seyoung5309.executorserver.global.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "관리자 - 방명록", description = "ADMIN 권한 필요")
@Validated
@RestController
@RequestMapping("/api/admin/guestbook")
@RequiredArgsConstructor
public class AdminGuestbookController {

    private final GuestbookService guestbookService;

    @Operation(summary = "방명록 목록", description = "최신순. q로 이름·내용 검색. size 1~50(기본 20).")
    @GetMapping
    public PageResponse<GuestbookEntryResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page는 0 이상이어야 합니다.") int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "size는 1 이상이어야 합니다.")
            @Max(value = 50, message = "size는 50 이하여야 합니다.")
            int size) {
        return guestbookService.search(q, page, size);
    }

    @Operation(summary = "방명록 글 삭제", description = "부적절한 글. 게임 화면에서 바로 사라진다(다음 새로고침).")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        guestbookService.delete(id);
    }
}
