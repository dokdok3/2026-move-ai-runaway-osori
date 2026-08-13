package com.hackathon.global.exception;

public class BusinessException extends AbstractCustomException {

    public BusinessException(ErrorCodeSpec errorCode) {
        super(errorCode);
    }

    public BusinessException(ErrorCodeSpec errorCode, String message) {
        super(errorCode, message);
    }
}
