package com.seyoung5309.executorserver.domain.auth.controller;

import com.seyoung5309.executorserver.domain.auth.dto.LoginRequest;
import com.seyoung5309.executorserver.domain.auth.dto.LoginResponse;
import com.seyoung5309.executorserver.domain.auth.dto.ReissueRequest;
import com.seyoung5309.executorserver.domain.auth.dto.SignupRequest;
import com.seyoung5309.executorserver.domain.auth.dto.SignupResponse;
import com.seyoung5309.executorserver.domain.auth.dto.TokenResponse;
import com.seyoung5309.executorserver.domain.auth.service.AuthService;
import com.seyoung5309.executorserver.global.security.LoginUser;
import com.seyoung5309.executorserver.global.response.AvailabilityResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "인증", description = "회원가입, 로그인, 토큰")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "회원가입", description = "users와 profiles를 함께 만든다. 이메일은 소문자로 저장된다.")
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public SignupResponse signup(@Valid @RequestBody SignupRequest request) {
        return authService.signup(request);
    }

    @Operation(summary = "로그인", description = "Access Token(30분)과 Refresh Token(14일)을 발급한다.")
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @Operation(summary = "토큰 재발급",
            description = "Refresh Token으로 두 토큰을 모두 새로 발급한다. 사용한 Refresh Token은 다시 쓸 수 없다.")
    @PostMapping("/reissue")
    public TokenResponse reissue(@Valid @RequestBody ReissueRequest request) {
        return authService.reissue(request.refreshToken());
    }

    @Operation(summary = "로그아웃", description = "Refresh Token을 무효화한다. Access Token이 필요하다.")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@LoginUser Long userId) {
        authService.logout(userId);
    }

    @Operation(summary = "이메일 중복 확인")
    @GetMapping("/check-email")
    public AvailabilityResponse checkEmail(
            @RequestParam
            @NotBlank(message = "이메일을 입력해 주세요.")
            @Email(message = "이메일 형식이 올바르지 않습니다.")
            String email) {
        return new AvailabilityResponse(authService.isEmailAvailable(email));
    }
}
