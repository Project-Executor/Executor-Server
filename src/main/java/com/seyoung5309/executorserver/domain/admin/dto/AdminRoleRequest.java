package com.seyoung5309.executorserver.domain.admin.dto;

import com.seyoung5309.executorserver.domain.auth.entity.Role;
import jakarta.validation.constraints.NotNull;

/** 권한 변경. role은 USER 또는 ADMIN */
public record AdminRoleRequest(
        @NotNull(message = "role을 입력해 주세요.")
        Role role
) {
}
