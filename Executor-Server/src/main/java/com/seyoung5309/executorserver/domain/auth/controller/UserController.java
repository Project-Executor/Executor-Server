package com.seyoung5309.executorserver.domain.auth.controller;

import com.seyoung5309.executorserver.domain.auth.dto.WithdrawRequest;
import com.seyoung5309.executorserver.domain.auth.service.AuthService;
import com.seyoung5309.executorserver.global.security.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "인증", description = "회원가입, 로그인, 토큰")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;

    @Operation(summary = "회원 탈퇴",
            description = "비밀번호를 다시 확인한 뒤 계정과 관련 데이터를 삭제한다. 신고 기록은 남고 신고자는 NULL이 된다.")
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdraw(@LoginUser Long userId, @Valid @RequestBody WithdrawRequest request) {
        authService.withdraw(userId, request.password());
    }
}
