package com.seyoung5309.executorserver.domain.guidebook.controller;

import com.seyoung5309.executorserver.domain.guidebook.dto.GuidebookDetailResponse;
import com.seyoung5309.executorserver.domain.guidebook.dto.GuidebookListResponse;
import com.seyoung5309.executorserver.domain.guidebook.service.GuidebookService;
import com.seyoung5309.executorserver.global.security.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "도감", description = "이상 개체·개발 용어 도감")
@RestController
@RequestMapping("/api/guidebooks")
@RequiredArgsConstructor
public class GuidebookController {

    private final GuidebookService guidebookService;

    @Operation(summary = "도감 목록",
            description = "category(BUG, VIRUS, TERM)를 빼면 전체. 잠긴 항목은 분류만 보이고 이름·이미지는 null. "
                    + "개수는 지정한 분류 안에서 센다.")
    @GetMapping
    public GuidebookListResponse getGuidebooks(@LoginUser Long userId,
                                               @RequestParam(required = false) String category) {
        return guidebookService.getGuidebooks(userId, GuidebookService.parseCategory(category));
    }

    @Operation(summary = "도감 상세", description = "아직 해금하지 않은 항목은 403 FORBIDDEN.")
    @GetMapping("/{guidebookId}")
    public GuidebookDetailResponse getGuidebook(@LoginUser Long userId, @PathVariable Long guidebookId) {
        return guidebookService.getGuidebook(userId, guidebookId);
    }
}
