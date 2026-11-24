package com.seyoung5309.executorserver.domain.profile.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 프로필 사진 올리기.
 *
 * @param image PNG 또는 JPEG 파일을 Base64로 인코딩한 문자열 (디코딩해서 512KB 이하). 게임은 256x256 PNG로 잘라 보낸다
 */
public record AvatarRequest(
        @NotBlank(message = "image를 입력해 주세요.")
        String image
) {
}
