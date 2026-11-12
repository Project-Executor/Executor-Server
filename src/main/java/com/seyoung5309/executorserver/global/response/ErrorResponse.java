package com.seyoung5309.executorserver.global.response;

import com.seyoung5309.executorserver.global.exception.ErrorCode;

/**
 * 공통 오류 응답. { "status": 400, "code": "INVALID_INPUT", "message": "..." }
 */
public record ErrorResponse(int status, String code, String message) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return of(errorCode, errorCode.getMessage());
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(errorCode.getStatus().value(), errorCode.name(), message);
    }
}
