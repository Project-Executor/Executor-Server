package com.seyoung5309.executorserver.domain.stage.controller;

import com.seyoung5309.executorserver.domain.stage.dto.OperationResponse;
import com.seyoung5309.executorserver.domain.stage.service.StageService;
import com.seyoung5309.executorserver.global.security.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "스테이지", description = "작전·스테이지 목록, 상세, 결과 제출, 순위")
@RestController
@RequestMapping("/api/operations")
@RequiredArgsConstructor
public class OperationController {

    private final StageService stageService;

    @Operation(summary = "작전·스테이지 목록과 내 진행도",
            description = "공식 스테이지를 작전 순서 -> 스테이지 번호 순으로 돌려준다. "
                    + "해금 규칙: 첫 스테이지, 바로 앞 스테이지를 클리어함, 또는 이미 클리어한 스테이지.")
    @GetMapping
    public List<OperationResponse> getOperations(@LoginUser Long userId) {
        return stageService.getOperations(userId);
    }
}
