package com.hackathon.global.exception;

import lombok.Getter;

/**
 * 애플리케이션에서 의도적으로 발생시키는 모든 커스텀 예외의 기반 클래스.
 *
 * <p>도메인별 예외는 이 클래스를 상속하고 생성자에서 적절한 {@link ErrorCodeSpec}을 전달한다.</p>
 */
@Getter
public abstract class AbstractCustomException extends RuntimeException {

    private final ErrorCodeSpec errorCode;

    protected AbstractCustomException(ErrorCodeSpec errorCode) {
        this(errorCode, errorCode.getMessage());
    }

    protected AbstractCustomException(ErrorCodeSpec errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    protected AbstractCustomException(ErrorCodeSpec errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    protected AbstractCustomException(ErrorCodeSpec errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
