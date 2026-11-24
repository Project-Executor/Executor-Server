package com.seyoung5309.executorserver.domain.admin.controller;

import com.seyoung5309.executorserver.domain.event.service.EventRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "관리자 - 이벤트 기록", description = "ADMIN 권한 필요. 순위 조회는 공개 API(GET /api/events/{stageId}/records)를 쓴다")
@RestController
@RequestMapping("/api/admin/events/{stageId}/records")
@RequiredArgsConstructor
public class AdminEventController {

    private final EventRecordService eventRecordService;

    @Operation(summary = "기록 하나 삭제", description = "부적절한 이름 등. 없거나 다른 스테이지의 기록이면 404.")
    @DeleteMapping("/{recordId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long stageId, @PathVariable Long recordId) {
        eventRecordService.delete(stageId, recordId);
    }

    @Operation(summary = "기록 전부 삭제", description = "전시 시작 전 초기화. 응답은 지운 수 {\"deleted\": n}.")
    @DeleteMapping
    public Map<String, Integer> deleteAll(@PathVariable Long stageId) {
        return Map.of("deleted", eventRecordService.deleteAll(stageId));
    }
}
