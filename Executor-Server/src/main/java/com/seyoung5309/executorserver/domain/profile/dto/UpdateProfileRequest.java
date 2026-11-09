package com.seyoung5309.executorserver.domain.profile.dto;

import jakarta.validation.constraints.Size;

/**
 * 프로필 수정. 보낸 필드만 바꾸고, 보내지 않은(null) 필드는 그대로 둔다.
 * comment에 빈 문자열을 보내면 자기소개를 지운다.
 */
public record UpdateProfileRequest(
        @Size(min = 2, max = 20, message = "닉네임은 2~20자여야 합니다.")
        String nickname,

        @Size(min = 1, max = 20, message = "집행자 이름은 1~20자여야 합니다.")
        String characterName,

        @Size(max = 255, message = "자기소개는 255자 이하여야 합니다.")
        String comment
) {
}
