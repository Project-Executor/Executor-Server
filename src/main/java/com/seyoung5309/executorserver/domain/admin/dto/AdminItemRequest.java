package com.seyoung5309.executorserver.domain.admin.dto;

import com.seyoung5309.executorserver.domain.item.entity.ItemType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 아이템 등록·수정 (PUT이므로 모든 값을 보낸다).
 *
 * @param isActive 판매 중 여부. 등록할 때는 무시하고 항상 true, 수정할 때는 필수 (서비스에서 검사)
 */
public record AdminItemRequest(
        @NotBlank(message = "name을 입력해 주세요.")
        @Size(max = 100, message = "name은 100자 이하여야 합니다.")
        String name,

        @Size(max = 255, message = "image는 255자 이하여야 합니다.")
        String image,

        @Size(max = 255, message = "description은 255자 이하여야 합니다.")
        String description,

        @NotNull(message = "itemType을 입력해 주세요. (SKIN, THEME, HINT)")
        ItemType itemType,

        @NotNull(message = "price를 입력해 주세요.")
        @Min(value = 0, message = "price는 0 이상이어야 합니다.")
        Integer price,

        Boolean isActive
) {
}
