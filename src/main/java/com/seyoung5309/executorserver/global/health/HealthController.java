package com.seyoung5309.executorserver.global.health;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 서버 연결 확인 (게임 로그인 창의 "$ ping api"). 로그인 없이 부를 수 있고 DB를 건드리지 않는다 */
@Tag(name = "Health", description = "서버 연결 확인")
@RestController
public class HealthController {

    @Operation(summary = "서버 연결 확인", description = "항상 {\"status\":\"ok\"}. 게임이 응답 시간을 재는 데 쓴다.")
    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
}
