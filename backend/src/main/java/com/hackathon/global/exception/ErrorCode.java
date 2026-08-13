package com.hackathon.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode implements ErrorCodeSpec {
    UNAUTHORIZED(401, "사용자 식별에 실패했습니다."),
    CARGO_NOT_FOUND(404, "화물을 찾을 수 없습니다."),
    CARGO_ALREADY_MATCHED(409, "이미 배차된 화물입니다."),
    CARGO_NOT_MODIFIABLE(400, "배차 완료된 화물은 수정할 수 없습니다."),
    VEHICLE_NOT_SUITABLE(400, "차량 적재량이 부족합니다."),
    DRIVER_NOT_FOUND(404, "기사를 찾을 수 없습니다."),
    SHIPPER_NOT_FOUND(404, "화주를 찾을 수 없습니다."),
    AI_PARSING_FAILED(422, "요청 내용에서 출발지와 도착지를 읽지 못했습니다."),
    AI_CALL_FAILED(502, "AI 호출에 실패했습니다.");

    private final int status;
    private final String message;
}
