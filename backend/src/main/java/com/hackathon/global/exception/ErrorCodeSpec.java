package com.hackathon.global.exception;

/**
 * 커스텀 예외가 HTTP 응답으로 변환되기 위해 제공해야 하는 오류 정보.
 * 도메인별 오류 enum도 이 인터페이스를 구현할 수 있다.
 */
public interface ErrorCodeSpec {

    int getStatus();

    String getMessage();
}
