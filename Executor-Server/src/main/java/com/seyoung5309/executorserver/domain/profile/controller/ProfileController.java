package com.seyoung5309.executorserver.domain.profile.controller;

import com.seyoung5309.executorserver.domain.profile.service.ProfileService;
import com.seyoung5309.executorserver.global.response.AvailabilityResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "프로필", description = "프로필 조회·수정, 닉네임, 장착")
@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @Operation(summary = "닉네임 중복 확인", description = "로그인 없이 호출할 수 있다.")
    @GetMapping("/check-nickname")
    public AvailabilityResponse checkNickname(
            @RequestParam
            @NotBlank(message = "닉네임을 입력해 주세요.")
            @Size(min = 2, max = 20, message = "닉네임은 2~20자여야 합니다.")
            String nickname) {
        return new AvailabilityResponse(profileService.isNicknameAvailable(nickname));
    }
}
