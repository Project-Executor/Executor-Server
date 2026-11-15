package com.seyoung5309.executorserver.domain.auth.dto;

/** 회원가입 응답. 닉네임은 가입 후 튜토리얼에서 정한다 (PATCH /profiles/me) */
public record SignupResponse(Long userId, String email) {
}
