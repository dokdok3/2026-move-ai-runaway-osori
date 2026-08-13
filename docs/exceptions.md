# 예외 처리 구조

## 클래스 구조

```text
RuntimeException
└── AbstractCustomException
    ├── BusinessException                 현재 공통 비즈니스 예외
    ├── CargoNotFoundException            화물 담당자가 추가할 예외 예시
    ├── DriverNotFoundException           기사 담당자가 추가할 예외 예시
    └── OpenAiCallException               AI 담당자가 추가할 예외 예시
```

`GlobalExceptionHandler`는 `AbstractCustomException` 하나를 처리한다. 따라서 새로운
도메인 예외를 추가해도 핸들러를 매번 수정할 필요가 없다.

```text
Controller / Service / Client
          │ throw
          ▼
도메인별 CustomException
          │ extends
          ▼
AbstractCustomException ────── ErrorCodeSpec (HTTP status, 기본 메시지)
                                      ▲
                                      ├── ErrorCode (공통)
                                      └── 도메인별 ErrorCode enum (선택)
          │
          ▼
GlobalExceptionHandler
          │
          ▼
ApiResponse.fail(message) + ErrorCode의 HTTP status
```

## 커스텀 예외 추가 방법

각 담당자는 자신의 도메인 패키지에 예외를 만들고 `AbstractCustomException`을
상속한다.

```java
package com.hackathon.domain.cargo.exception;

import com.hackathon.global.exception.AbstractCustomException;
import com.hackathon.global.exception.ErrorCode;

public class CargoNotFoundException extends AbstractCustomException {

    public CargoNotFoundException() {
        super(ErrorCode.CARGO_NOT_FOUND);
    }
}
```

서비스에서는 다음처럼 사용한다.

```java
Cargo cargo = cargoRepository.findById(cargoId)
        .orElseThrow(CargoNotFoundException::new);
```

오류 코드가 많아지면 도메인별 enum을 만들 수 있다. 이 경우
`ErrorCodeSpec`을 구현하면 전역 핸들러가 동일하게 처리한다.

```java
@Getter
@RequiredArgsConstructor
public enum CargoErrorCode implements ErrorCodeSpec {
    NOT_FOUND(404, "화물을 찾을 수 없습니다."),
    ALREADY_MATCHED(409, "이미 배차된 화물입니다.");

    private final int status;
    private final String message;
}
```

상황에 따라 메시지나 원인을 보존해야 하면 기반 클래스의 다른 생성자를 호출한다.

```java
public class OpenAiCallException extends AbstractCustomException {

    public OpenAiCallException(Throwable cause) {
        super(ErrorCode.AI_CALL_FAILED, cause);
    }
}
```

## 규칙

- 예상 가능한 애플리케이션 오류만 `AbstractCustomException`을 상속한다.
- HTTP 상태와 사용자에게 노출할 기본 메시지는 `ErrorCode`에서 관리한다.
- 예외 메시지에 API 키, 요청 전문, 개인정보 같은 민감 정보를 넣지 않는다.
- 복구할 수 없는 프로그래밍 오류는 억지로 커스텀 예외로 감싸지 않고 최종
  `Exception` 핸들러에서 500으로 처리한다.
- 도메인 예외를 추가할 때 `GlobalExceptionHandler`에는 별도 핸들러를 추가하지 않는다.
