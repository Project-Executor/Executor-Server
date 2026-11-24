package com.seyoung5309.executorserver.domain.guestbook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 방명록 남기기 (로그인 없음).
 *
 * @param name     앞뒤 공백을 지우고 1~10자
 * @param message  앞뒤 공백을 지우고 1~100자
 * @param deviceId 게임이 PC마다 만든 임의 값 (같은 기기 30초 연속 작성 제한). 없으면 제한 없음
 */
public record GuestbookRequest(
        @NotBlank(message = "이름을 입력해 주세요.")
        @Size(max = 40, message = "이름은 10자 이하여야 합니다.")
        String name,

        @NotBlank(message = "남길 말을 입력해 주세요.")
        @Size(max = 400, message = "남길 말은 100자 이하여야 합니다.")
        String message,

        @Size(max = 64, message = "deviceId는 64자 이하여야 합니다.")
        String deviceId
) {
}
