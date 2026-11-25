package com.seyoung5309.executorserver.domain.admin.controller;

import com.seyoung5309.executorserver.domain.guestbook.dto.GuestbookEntryResponse;
import com.seyoung5309.executorserver.domain.guestbook.service.GuestbookService;
import com.seyoung5309.executorserver.global.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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

    @Operation(summary = "방명록 켜짐 여부", description = "꺼져 있으면 게임에서 방명록 위젯·창이 보이지 않는다.")
    @GetMapping("/settings")
    public GuestbookSettings settings() {
        return new GuestbookSettings(guestbookService.isEnabled());
    }

    @Operation(summary = "방명록 켜기/끄기", description = "전시할 때 켠다. 게임은 다음 새로고침(10초 안)에 반영한다.")
    @PutMapping("/settings")
    public GuestbookSettings updateSettings(@RequestBody @Valid GuestbookSettings request) {
        guestbookService.setEnabled(request.enabled());
        return new GuestbookSettings(guestbookService.isEnabled());
    }

    /** @param enabled 방명록을 게임에 보일지 */
    public record GuestbookSettings(@NotNull(message = "enabled를 입력해 주세요.") Boolean enabled) {
    }

    @Operation(summary = "방명록 글 삭제", description = "부적절한 글. 게임 화면에서 바로 사라진다(다음 새로고침).")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        guestbookService.delete(id);
    }
}
