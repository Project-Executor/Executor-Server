package com.seyoung5309.executorserver.domain.event.controller;

import com.seyoung5309.executorserver.domain.event.dto.EventRankingResponse;
import com.seyoung5309.executorserver.domain.event.dto.EventRecordRequest;
import com.seyoung5309.executorserver.domain.event.dto.EventRecordResponse;
import com.seyoung5309.executorserver.domain.event.service.EventRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "이벤트 스테이지", description = "전시용. 로그인 없이 기록을 올리고 순위를 본다")
@Validated
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventRecordService eventRecordService;

    @Operation(summary = "이벤트 순위",
            description = "턴수 이벤트는 score(턴)가 적을수록, 타자 이벤트는 score(친 단어 수)가 많을수록 위. "
                    + "같은 점수는 같은 순위, 이름순. 이벤트 스테이지가 아니면 404.")
    @GetMapping("/{stageId}/records")
    public EventRankingResponse getRanking(@PathVariable Long stageId,
                                           @RequestParam(defaultValue = "20")
                                           @Min(value = 1, message = "size는 1 이상이어야 합니다.")
                                           @Max(value = 100, message = "size는 100 이하여야 합니다.")
                                           int size) {
        return eventRecordService.getRanking(stageId, size);
    }

    @Operation(summary = "이벤트 기록 올리기",
            description = "이름(앞뒤 공백 제외 1~12자)과 score. 같은 스테이지에 같은 이름(대소문자 무시)이 있으면 "
                    + "409 DUPLICATE_VALUE(이미 사용 중인 이름입니다). 응답은 이 기록의 순위.")
    @PostMapping("/{stageId}/records")
    @ResponseStatus(HttpStatus.CREATED)
    public EventRecordResponse submit(@PathVariable Long stageId, @Valid @RequestBody EventRecordRequest request) {
        return eventRecordService.submit(stageId, request);
    }
}
