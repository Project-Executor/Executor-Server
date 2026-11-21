package com.seyoung5309.executorserver.domain.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 인물 등록·수정 (PUT이므로 모든 값을 보낸다).
 *
 * @param characterKey 일러스트 폴더 ID (영문 소문자·숫자·-·_)
 * @param name         대사의 speaker와 같은 이름
 * @param tagColor     이름표 바탕색 #RRGGBB (대소문자 무관, 대문자로 저장)
 * @param protagonist  주인공(대화창 왼쪽)인지. null이면 false
 */
public record AdminCharacterRequest(
        @NotBlank(message = "characterKey를 입력해 주세요.")
        @Pattern(regexp = "^\\s*[a-z0-9_-]{1,50}\\s*$", message = "characterKey는 영문 소문자·숫자·-·_ 50자 이하여야 합니다.")
        String characterKey,

        @NotBlank(message = "name을 입력해 주세요.")
        @Size(max = 50, message = "name은 50자 이하여야 합니다.")
        String name,

        @NotBlank(message = "tagColor를 입력해 주세요.")
        @Pattern(regexp = "^\\s*#[0-9A-Fa-f]{6}\\s*$", message = "tagColor는 #RRGGBB 형식이어야 합니다. (예: #FF9A3C)")
        String tagColor,

        Boolean protagonist
) {
}
