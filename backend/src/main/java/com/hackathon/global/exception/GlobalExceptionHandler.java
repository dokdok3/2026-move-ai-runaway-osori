package com.hackathon.global.exception;

import com.hackathon.global.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AbstractCustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomException(AbstractCustomException e) {
        log.warn("custom exception: type={}, errorCode={}",
                e.getClass().getSimpleName(), e.getErrorCode(), e);
        return ResponseEntity.status(e.getErrorCode().getStatus())
                .body(ApiResponse.fail(e.getMessage()));
    }

    /** AI 연동 전까지 TODO로 남겨둔 기능이 호출됐을 때. 메시지에 어떤 부분이 미구현인지 그대로 담아 내려준다. */
    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotImplemented(UnsupportedOperationException e) {
        log.warn("not implemented", e);
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(ApiResponse.fail(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        log.warn("validation failed", e);
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(this::toUserMessage)
                .distinct()
                .reduce((left, right) -> left + " " + right)
                .orElse("잘못된 요청입니다.");
        return ResponseEntity.badRequest().body(ApiResponse.fail(message));
    }

    private String toUserMessage(FieldError fieldError) {
        return switch (fieldError.getField()) {
            case "origin.sido" -> "출발지 시도는 필수입니다.";
            case "origin.sigungu" -> "출발지 시군구는 필수입니다.";
            case "destination.sido" -> "도착지 시도는 필수입니다.";
            case "destination.sigungu" -> "도착지 시군구는 필수입니다.";
            default -> fieldError.getDefaultMessage();
        };
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
        log.error("unexpected error", e);
        return ResponseEntity.internalServerError()
                .body(ApiResponse.fail("서버 오류가 발생했습니다."));
    }
}
