package com.seyoung5309.executorserver.domain.profile.controller;

import com.seyoung5309.executorserver.domain.profile.dto.EquipmentRequest;
import com.seyoung5309.executorserver.domain.profile.dto.EquipmentResponse;
import com.seyoung5309.executorserver.domain.profile.dto.MyProfileResponse;
import com.seyoung5309.executorserver.domain.profile.dto.ProfileDetailResponse;
import com.seyoung5309.executorserver.domain.profile.dto.UpdateProfileRequest;
import com.seyoung5309.executorserver.domain.profile.service.ProfileService;
import com.seyoung5309.executorserver.global.response.AvailabilityResponse;
import com.seyoung5309.executorserver.global.response.PageResponse;
import com.seyoung5309.executorserver.global.response.UserSummary;
import com.seyoung5309.executorserver.global.security.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

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

    @Operation(summary = "내 프로필 조회")
    @GetMapping("/me")
    public MyProfileResponse getMyProfile(@LoginUser Long userId) {
        return profileService.getMyProfile(userId);
    }

    @Operation(summary = "내 프로필 수정",
            description = "보낸 필드만 수정한다. 튜토리얼의 집행자 이름 입력도 이 API(characterName)를 쓴다. comment에 빈 문자열을 보내면 지워진다.")
    @PatchMapping("/me")
    public MyProfileResponse updateMyProfile(@LoginUser Long userId,
                                             @Valid @RequestBody UpdateProfileRequest request) {
        return profileService.updateMyProfile(userId, request);
    }

    @Operation(summary = "스킨·테마 장착", description = "두 값을 모두 보낸다. null이면 장착을 해제하고 기본값으로 돌아간다.")
    @PutMapping("/me/equipment")
    public EquipmentResponse updateEquipment(@LoginUser Long userId,
                                             @RequestBody EquipmentRequest request) {
        return profileService.updateEquipment(userId, request);
    }

    @Operation(summary = "닉네임 검색", description = "닉네임에 검색어가 포함된 사용자. 나 자신은 제외한다.")
    @GetMapping("/search")
    public PageResponse<UserSummary> search(
            @LoginUser Long userId,
            @RequestParam
            @NotBlank(message = "검색어를 입력해 주세요.")
            @Size(max = 20, message = "검색어는 20자 이하여야 합니다.")
            String nickname,
            @ParameterObject Pageable pageable) {
        return profileService.searchByNickname(userId, nickname, pageable);
    }

    @Operation(summary = "다른 사용자 프로필 조회", description = "클리어 수와 별 개수는 공식 스테이지만 센다.")
    @GetMapping("/{userId}")
    public ProfileDetailResponse getProfile(@LoginUser Long viewerId, @PathVariable Long userId) {
        return profileService.getProfile(viewerId, userId);
    }
}
