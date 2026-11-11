package com.seyoung5309.executorserver.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * API 명세서 0.3 오류 코드 목록과 1:1로 맞춘다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 400
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),

    // 401
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 Refresh Token입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),

    // 403
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다."),
    STAGE_LOCKED(HttpStatus.FORBIDDEN, "아직 해금되지 않은 스테이지입니다."),

    // 404
    NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다."),

    // 409
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),
    NOT_ENOUGH_COIN(HttpStatus.CONFLICT, "코인이 부족합니다."),
    ALREADY_OWNED(HttpStatus.CONFLICT, "이미 보유한 아이템입니다."),
    NOT_ENOUGH_ITEM(HttpStatus.CONFLICT, "아이템 수량이 부족합니다."),
    ALREADY_CLAIMED(HttpStatus.CONFLICT, "이미 수령한 우편입니다."),
    MAIL_EXPIRED(HttpStatus.CONFLICT, "만료된 우편입니다."),
    ALREADY_FRIEND(HttpStatus.CONFLICT, "이미 친구입니다."),
    REQUEST_ALREADY_EXISTS(HttpStatus.CONFLICT, "대기 중인 친구 요청이 이미 있습니다."),
    REQUEST_ALREADY_HANDLED(HttpStatus.CONFLICT, "이미 처리된 친구 요청입니다."),
    NOT_FRIEND(HttpStatus.CONFLICT, "친구에게만 메시지를 보낼 수 있습니다."),
    NOT_VERIFIED(HttpStatus.CONFLICT, "제작자가 클리어한 맵만 공개할 수 있습니다."),
    PUBLISHED_STAGE(HttpStatus.CONFLICT, "공개된 맵은 수정할 수 없습니다."),
    OPERATION_HAS_STAGES(HttpStatus.CONFLICT, "스테이지가 남아 있는 작전은 삭제할 수 없습니다."),
    DUPLICATE_VALUE(HttpStatus.CONFLICT, "이미 사용 중인 값입니다."),

    // 500
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;
}
