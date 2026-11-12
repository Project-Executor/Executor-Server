package com.seyoung5309.executorserver.global.exception;

import lombok.Getter;

/**
 * 서비스 계층에서 던지는 예외. GlobalExceptionHandler가 ErrorResponse로 바꿔서 응답한다.
 * <pre>
 * throw new BusinessException(ErrorCode.NOT_ENOUGH_COIN);
 * throw new BusinessException(ErrorCode.INVALID_INPUT, "힌트 번호가 범위를 벗어났습니다.");
 * </pre>
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
