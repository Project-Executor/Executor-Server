package com.seyoung5309.executorserver.domain.admin.controller;

import com.seyoung5309.executorserver.domain.admin.dto.AdminItemRequest;
import com.seyoung5309.executorserver.domain.admin.dto.AdminItemResponse;
import com.seyoung5309.executorserver.domain.admin.service.AdminItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "관리자 - 아이템", description = "ADMIN 권한 필요. 삭제 API는 없고 판매 중단은 isActive = false.")
@RestController
@RequestMapping("/api/admin/items")
@RequiredArgsConstructor
public class AdminItemController {

    private final AdminItemService adminItemService;

    @Operation(summary = "아이템 전체 목록", description = "판매 중단된 아이템까지 모두 나온다.")
    @GetMapping
    public List<AdminItemResponse> getAll() {
        return adminItemService.getAll();
    }

    @Operation(summary = "아이템 등록", description = "항상 판매 중으로 등록한다(isActive 무시). 이름은 겹칠 수 없다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminItemResponse create(@Valid @RequestBody AdminItemRequest request) {
        return adminItemService.create(request);
    }

    @Operation(summary = "아이템 수정",
            description = "모든 값을 보낸다(isActive 필수). 가진 사용자가 있는 아이템은 itemType을 바꿀 수 없다.")
    @PutMapping("/{itemId}")
    public AdminItemResponse update(@PathVariable Long itemId, @Valid @RequestBody AdminItemRequest request) {
        return adminItemService.update(itemId, request);
    }
}
