# 화물 매칭 API 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 화주가 자연어로 화물을 등록하고 기사가 매칭 점수순 목록에서 수락하는 백엔드 API 10개를 구현한다.

**Architecture:** Java 25 + Spring Boot 3.5, 도메인 단위 패키지. JPA + PostgreSQL(운영/로컬) + Flyway 스키마 관리, 테스트는 H2(create-drop, Flyway 비활성) — 저장소에 이미 세팅되어 있다. Redis는 인프라에 떠 있지만 이 계획에서는 쓰지 않는다(시세는 DB 테이블에 캐싱). AI는 두 곳 — 자연어 파싱과 구간 시세 추정 — 모두 Claude API(structured outputs)로 처리한다. 매칭 점수는 룰 기반이라 외부 호출 없이 응답한다.

**Tech Stack:** Spring Boot 3.5.16, Spring Data JPA, Flyway, PostgreSQL/PostGIS(미사용, 인프라만), Bean Validation, Lombok, springdoc-openapi(기존 설정 재사용), `com.anthropic:anthropic-java`, JUnit 5 + MockMvc

**설계 문서:** `docs/superpowers/specs/cargo-matching-api-design.md`

**대상 저장소:** `dokdok3/2026-move-ai-runaway-osori` (로컬: `/Users/slee/Desktop/project/2026-move-ai-runaway-osori`). Gradle 루트는 `backend/`이며, 이 계획의 모든 상대 경로(`src/...`, `build.gradle.kts` 등)와 `./gradlew` 명령은 저장소 루트가 아니라 `backend/` 기준이다.

## Global Constraints

- Java 25, Spring Boot 3.5.16, Gradle(Kotlin DSL, `build.gradle.kts`) — 전부 저장소에 이미 세팅되어 있다. 새로 만들지 않고 기존 파일에 추가한다
- 패키지 루트: `com.hackathon` (기존 `HackathonApplication`, `config.OpenApiConfig`, `health.HealthController`와 동일 루트). 그 두 기존 파일은 건드리지 않고, 새 코드는 CLAUDE.md 컨벤션대로 `domain/`과 `global/` 아래에 추가한다
- 도메인 단위 패키지 (`domain/{entity}/controller|service|repository|dto|entity`), 레이어 최상위 구조 금지
- DTO는 전부 `record`
- 모든 응답은 `ApiResponse<T>`로 감싼다
- Entity에 `@Setter` / `@Data` 금지. 생성은 정적 팩토리 메서드
- `@ManyToOne`은 쓰지 않는다 (FK 없이 ID 컬럼만 보관) — 해커톤 속도 우선, N+1 원천 차단
- Service는 `HttpServletRequest`를 받지 않는다. 사용자 식별은 `@LoginUser Long userId`
- `Optional.get()` 금지 — `.orElseThrow()`
- `System.out.println` 금지 — `log.info()`
- Claude 모델 ID는 `claude-opus-5` 고정
- **스키마는 Flyway로 관리한다** (`spring.jpa.hibernate.ddl-auto: validate`가 이미 설정되어 있다). 엔티티를 새로 만드는 Task마다 `backend/src/main/resources/db/migration/V{n}__....sql` 마이그레이션을 함께 작성한다. 기존 `V1__enable_postgis.sql`이 있으므로 다음 번호는 `V2`부터다. **테스트는 H2 + create-drop + Flyway 비활성**이라 마이그레이션 없이도 통과한다 — 마이그레이션은 `bootRun`/수동 curl 확인에만 필요하다
- 로컬에서 `bootRun`이나 수동 curl 확인을 하려면 먼저 `docker compose up -d postgres redis`로 인프라를 띄워야 한다 (저장소 루트에서 실행, `.env` 필요 — `cp .env.example .env`)
- **git 저장소다** (origin: `dokdok3/2026-move-ai-runaway-osori`, 브랜치 `main`). 각 Task는 논리적으로 하나의 커밋 단위지만, 사용자가 명시적으로 요청하기 전까지는 커밋하지 않는다 — Task 완료마다 전체 테스트 통과만 확인하고 다음 Task로 넘어간다

---

## 파일 구조

이미 존재하는 `HackathonApplication.java`, `config/OpenApiConfig.java`, `health/HealthController.java`는 그대로 두고, 아래를 추가한다.

```
backend/src/main/java/com/hackathon/
├── HackathonApplication.java   (기존, 수정하지 않음)
├── config/OpenApiConfig.java   (기존, 수정하지 않음)
├── health/HealthController.java (기존, 수정하지 않음)
├── global/
│   ├── response/ApiResponse.java
│   ├── exception/ErrorCode.java
│   ├── exception/BusinessException.java
│   ├── exception/GlobalExceptionHandler.java
│   ├── entity/BaseTimeEntity.java
│   ├── auth/LoginUser.java
│   ├── auth/LoginUserArgumentResolver.java
│   ├── config/WebConfig.java
│   ├── config/JpaAuditingConfig.java
│   └── ai/ClaudeClient.java
└── domain/
    ├── region/controller/RegionController.java
    ├── region/service/RegionService.java
    ├── region/dto/RegionResponse.java
    ├── driver/entity/Driver.java
    ├── driver/entity/DriverRoutePreference.java
    ├── driver/entity/Direction.java
    ├── driver/repository/DriverRepository.java
    ├── driver/repository/DriverRoutePreferenceRepository.java
    ├── driver/service/DriverService.java
    ├── driver/controller/DriverController.java
    ├── driver/dto/DriverResponse.java
    ├── driver/dto/RoutePreferenceRequest.java
    ├── driver/dto/RegionPoint.java
    ├── cargo/entity/Cargo.java
    ├── cargo/entity/CargoStatus.java
    ├── cargo/repository/CargoRepository.java
    ├── cargo/service/CargoService.java
    ├── cargo/service/CargoParseService.java
    ├── cargo/controller/CargoController.java
    ├── cargo/dto/CargoCreateRequest.java
    ├── cargo/dto/CargoUpdateRequest.java
    ├── cargo/dto/CargoDetailResponse.java
    ├── cargo/dto/ParseRequest.java
    ├── cargo/dto/ParsedCargoResponse.java
    ├── fare/entity/FareQuote.java
    ├── fare/repository/FareQuoteRepository.java
    ├── fare/service/FareQuoteService.java
    ├── fare/controller/FareController.java
    ├── fare/dto/FareQuoteResponse.java
    ├── matching/entity/Assignment.java
    ├── matching/repository/AssignmentRepository.java
    ├── matching/service/MatchScoreCalculator.java
    ├── matching/service/LoadService.java
    ├── matching/controller/LoadController.java
    ├── matching/dto/LoadResponse.java
    └── matching/dto/AcceptResponse.java

backend/src/main/resources/db/migration/
├── V1__enable_postgis.sql      (기존)
├── V2__create_driver_tables.sql
├── V3__create_cargo_table.sql
├── V4__create_assignment_table.sql
└── V5__create_fare_quote_table.sql
```

---

### Task 1: 공통 인프라 (기존 프로젝트에 추가)

프로젝트 골격(Gradle, Spring Boot, `application.yaml`, `HackathonApplication`)은 이미 저장소에 있다. 이 Task는 새로 만들지 않고 **기존 파일에 추가**한다.

**Files:**
- Modify: `backend/build.gradle.kts` (Lombok, Claude SDK 의존성 추가)
- Modify: `backend/src/main/resources/application.yaml` (Claude 모델 설정 추가)
- Create: `backend/src/main/java/com/hackathon/global/response/ApiResponse.java`
- Create: `backend/src/main/java/com/hackathon/global/exception/ErrorCode.java`
- Create: `backend/src/main/java/com/hackathon/global/exception/BusinessException.java`
- Create: `backend/src/main/java/com/hackathon/global/exception/GlobalExceptionHandler.java`
- Create: `backend/src/main/java/com/hackathon/global/entity/BaseTimeEntity.java`
- Create: `backend/src/main/java/com/hackathon/global/config/JpaAuditingConfig.java`
- Create: `backend/src/main/java/com/hackathon/global/auth/LoginUser.java`
- Create: `backend/src/main/java/com/hackathon/global/auth/LoginUserArgumentResolver.java`
- Create: `backend/src/main/java/com/hackathon/global/config/WebConfig.java`
- Test: `backend/src/test/java/com/hackathon/global/auth/LoginUserArgumentResolverTest.java`

**Interfaces:**
- Produces:
  - `ApiResponse.ok(T data)` → `ApiResponse<T>`, `ApiResponse.fail(String message)` → `ApiResponse<Void>`
  - `ErrorCode` enum with `getStatus()` / `getMessage()`
  - `new BusinessException(ErrorCode)` — `RuntimeException`, `getErrorCode()`
  - `BaseTimeEntity` — `@MappedSuperclass`, `getCreatedAt()` / `getUpdatedAt()`
  - `@LoginUser` on a `Long` controller parameter → resolves `X-User-Id` header

- [ ] **Step 1: `build.gradle.kts`에 의존성 추가**

`backend/build.gradle.kts`의 `dependencies { ... }` 블록 안에 다음을 추가한다 (기존 `implementation(...)` 항목들은 그대로 둔다):

```kotlin
    implementation("com.anthropic:anthropic-java:2.34.0")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
```

> Lombok 버전은 명시하지 않고 Spring Boot dependency-management BOM이 관리하는 버전을 그대로 쓴다. Java 25(신규 LTS)에서 어노테이션 프로세싱이 실패하면(`./gradlew compileJava` 에러) `org.projectlombok:lombok:<최신버전>`으로 버전을 명시해 올린다 — 실패할 때만 손댄다.

- [ ] **Step 2: `application.yaml`에 Claude 설정 추가**

`backend/src/main/resources/application.yaml` 최상위(들여쓰기 없이) 끝에 추가한다:

```yaml
anthropic:
  model: claude-opus-5

logging:
  level:
    com.hackathon: debug
```

> `ANTHROPIC_API_KEY`는 환경변수로 주입한다. SDK의 `AnthropicOkHttpClient.fromEnv()`가 자동으로 읽는다. `.env`/`compose.yaml`에는 넣지 않는다 — Postgres·Redis만 도커로 띄우고, 앱 프로세스는 로컬 JVM에서 직접 `ANTHROPIC_API_KEY` 환경변수를 읽는다.

- [ ] **Step 3: 빌드 확인**

Run:
```bash
cd /Users/slee/Desktop/project/2026-move-ai-runaway-osori/backend
./gradlew build
```
Expected: `BUILD SUCCESSFUL`. 새 도메인 소스가 아직 없어도 기존 `HackathonApplicationTests`가 통과해야 한다.

- [ ] **Step 4: 공통 클래스 작성**

`global/response/ApiResponse.java`:
```java
package com.hackathon.global.response;

public record ApiResponse<T>(boolean success, T data, String message) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static ApiResponse<Void> fail(String message) {
        return new ApiResponse<>(false, null, message);
    }
}
```

`global/exception/ErrorCode.java`:
```java
package com.hackathon.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    UNAUTHORIZED(401, "사용자 식별에 실패했습니다."),
    CARGO_NOT_FOUND(404, "화물을 찾을 수 없습니다."),
    CARGO_ALREADY_MATCHED(409, "이미 배차된 화물입니다."),
    CARGO_NOT_MODIFIABLE(400, "배차 완료된 화물은 수정할 수 없습니다."),
    VEHICLE_NOT_SUITABLE(400, "차량 적재량이 부족합니다."),
    DRIVER_NOT_FOUND(404, "기사를 찾을 수 없습니다."),
    AI_PARSING_FAILED(422, "요청 내용에서 출발지와 도착지를 읽지 못했습니다."),
    AI_CALL_FAILED(502, "AI 호출에 실패했습니다.");

    private final int status;
    private final String message;
}
```

`global/exception/BusinessException.java`:
```java
package com.hackathon.global.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
```

`global/exception/GlobalExceptionHandler.java`:
```java
package com.hackathon.global.exception;

import com.hackathon.global.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
        log.warn("business exception: {}", e.getErrorCode());
        return ResponseEntity.status(e.getErrorCode().getStatus())
                .body(ApiResponse.fail(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .orElse("잘못된 요청입니다.");
        return ResponseEntity.badRequest().body(ApiResponse.fail(message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
        log.error("unexpected error", e);
        return ResponseEntity.internalServerError()
                .body(ApiResponse.fail("서버 오류가 발생했습니다."));
    }
}
```

`global/entity/BaseTimeEntity.java`:
```java
package com.hackathon.global.entity;

import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity {

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
```

`global/config/JpaAuditingConfig.java`:
```java
package com.hackathon.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
```

- [ ] **Step 5: `@LoginUser` 실패 테스트 작성**

`src/test/java/com/hackathon/global/auth/LoginUserArgumentResolverTest.java`:
```java
package com.hackathon.global.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hackathon.global.exception.BusinessException;
import com.hackathon.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

class LoginUserArgumentResolverTest {

    private final LoginUserArgumentResolver resolver = new LoginUserArgumentResolver();

    @Test
    @DisplayName("X-User-Id 헤더가 있으면 Long으로 변환한다")
    void resolvesHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "42");

        Object result = resolver.resolveArgument(null, null, new ServletWebRequest(request), null);

        assertThat(result).isEqualTo(42L);
    }

    @Test
    @DisplayName("X-User-Id 헤더가 없으면 UNAUTHORIZED")
    void rejectsMissingHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        ServletWebRequest webRequest = new ServletWebRequest(request);

        assertThatThrownBy(() -> resolver.resolveArgument(null, null, webRequest, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("숫자가 아닌 헤더면 UNAUTHORIZED")
    void rejectsNonNumericHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "abc");
        ServletWebRequest webRequest = new ServletWebRequest(request);

        assertThatThrownBy(() -> resolver.resolveArgument(null, null, webRequest, null))
                .isInstanceOf(BusinessException.class);
    }
}
```

- [ ] **Step 6: 테스트 실패 확인**

Run: `./gradlew test --tests '*LoginUserArgumentResolverTest'`
Expected: FAIL — `LoginUserArgumentResolver` 클래스가 없어 컴파일 에러

- [ ] **Step 7: `@LoginUser` 구현**

`global/auth/LoginUser.java`:
```java
package com.hackathon.global.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginUser {
}
```

`global/auth/LoginUserArgumentResolver.java`:
```java
package com.hackathon.global.auth;

import com.hackathon.global.exception.BusinessException;
import com.hackathon.global.exception.ErrorCode;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class LoginUserArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String HEADER = "X-User-Id";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(LoginUser.class)
                && Long.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        String value = webRequest.getHeader(HEADER);
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }
}
```

`global/config/WebConfig.java`:
```java
package com.hackathon.global.config;

import com.hackathon.global.auth.LoginUserArgumentResolver;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final LoginUserArgumentResolver loginUserArgumentResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(loginUserArgumentResolver);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("*")
                .allowedHeaders("*");
    }
}
```

- [ ] **Step 8: 테스트 통과 확인**

Run: `./gradlew test --tests '*LoginUserArgumentResolverTest'`
Expected: PASS (3 tests)

- [ ] **Step 9: 인프라 기동 후 애플리케이션 확인**

Run:
```bash
cd /Users/slee/Desktop/project/2026-move-ai-runaway-osori
cp -n .env.example .env   # 이미 있으면 건너뜀
docker compose up -d postgres redis
cd backend
./gradlew bootRun
```
다른 터미널에서:
```bash
curl -s localhost:8080/api/health
```
Expected: `{"status":"UP"}` (기존 `HealthController`). 확인 후 `bootRun` 프로세스를 종료한다.

---

### Task 2: 지역 목록 API (`GET /api/v1/regions`)

**Files:**
- Create: `domain/region/dto/RegionResponse.java`
- Create: `domain/region/service/RegionService.java`
- Create: `domain/region/controller/RegionController.java`
- Test: `src/test/java/com/hackathon/domain/region/RegionControllerTest.java`

**Interfaces:**
- Consumes: `ApiResponse` (Task 1)
- Produces: `RegionResponse(String sido, List<String> sigungus)`, `RegionService.findAll()` → `List<RegionResponse>`

- [ ] **Step 1: 실패 테스트 작성**

`src/test/java/com/hackathon/domain/region/RegionControllerTest.java`:
```java
package com.hackathon.domain.region;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
class RegionControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("시도별 시군구 트리를 반환한다")
    void returnsRegionTree() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/regions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].sido").value("서울특별시"))
                .andExpect(jsonPath("$.data[0].sigungus[0]").value("강남구"));
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew test --tests '*RegionControllerTest'`
Expected: FAIL — 404 (핸들러 없음)

- [ ] **Step 3: 구현**

`domain/region/dto/RegionResponse.java`:
```java
package com.hackathon.domain.region.dto;

import java.util.List;

public record RegionResponse(String sido, List<String> sigungus) {
}
```

`domain/region/service/RegionService.java`:
```java
package com.hackathon.domain.region.service;

import com.hackathon.domain.region.dto.RegionResponse;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RegionService {

    // 해커톤 범위: 데모에 필요한 시도만 정적으로 보관한다.
    private static final List<RegionResponse> REGIONS = List.of(
            new RegionResponse("서울특별시", List.of("강남구", "송파구", "서초구", "마포구", "성동구")),
            new RegionResponse("경기도", List.of("수원시", "용인시", "평택시", "성남시", "고양시")),
            new RegionResponse("인천광역시", List.of("서구", "남동구", "연수구")),
            new RegionResponse("대전광역시", List.of("유성구", "대덕구", "서구")),
            new RegionResponse("충청남도", List.of("천안시", "아산시", "서산시")),
            new RegionResponse("부산광역시", List.of("강서구", "해운대구", "사상구")),
            new RegionResponse("울산광역시", List.of("남구", "북구")),
            new RegionResponse("광주광역시", List.of("광산구", "북구")),
            new RegionResponse("전라북도", List.of("전주시", "익산시")),
            new RegionResponse("경상남도", List.of("창원시", "김해시"))
    );

    public List<RegionResponse> findAll() {
        return REGIONS;
    }
}
```

`domain/region/controller/RegionController.java`:
```java
package com.hackathon.domain.region.controller;

import com.hackathon.domain.region.dto.RegionResponse;
import com.hackathon.domain.region.service.RegionService;
import com.hackathon.global.response.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/regions")
@RequiredArgsConstructor
public class RegionController {

    private final RegionService regionService;

    @GetMapping
    public ApiResponse<List<RegionResponse>> getRegions() {
        return ApiResponse.ok(regionService.findAll());
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew test --tests '*RegionControllerTest'`
Expected: PASS

---

### Task 3: Driver 엔티티 + seed + `GET /api/v1/drivers/me`

**Files:**
- Create: `domain/driver/entity/Driver.java`
- Create: `domain/driver/entity/Direction.java`
- Create: `domain/driver/entity/DriverRoutePreference.java`
- Create: `domain/driver/repository/DriverRepository.java`
- Create: `domain/driver/repository/DriverRoutePreferenceRepository.java`
- Create: `domain/driver/dto/RegionPoint.java`
- Create: `domain/driver/dto/DriverResponse.java`
- Create: `domain/driver/service/DriverService.java`
- Create: `domain/driver/controller/DriverController.java`
- Create: `global/config/DataInitializer.java`
- Test: `src/test/java/com/hackathon/domain/driver/DriverControllerTest.java`

**Interfaces:**
- Consumes: `BaseTimeEntity`, `@LoginUser`, `ApiResponse`, `ErrorCode.DRIVER_NOT_FOUND` (Task 1)
- Produces:
  - `Driver.create(...)` 정적 팩토리, getters: `getId/getName/getPlateNumber/getVehicleType/getCapacityTon/getBodyType/getRating/getTotalTrips/getCompletionRate/getMinAcceptFare/getContactableFrom/getContactableTo/getRecentTripSummary/getPhoneNumber`
  - `Direction { ORIGIN, DESTINATION }`
  - `DriverRoutePreference.of(Long driverId, Direction direction, String sido, String sigungu)` — `getSido()`, `getSigungu()`, `getDirection()`
  - `RegionPoint(String sido, String sigungu)`
  - `DriverResponse.from(Driver, List<DriverRoutePreference>)`
  - `DriverService.findMe(Long driverId)` → `DriverResponse`
  - `DriverRepository extends JpaRepository<Driver, Long>`
  - `DriverRoutePreferenceRepository.findByDriverId(Long)` → `List<DriverRoutePreference>`, `deleteByDriverId(Long)`

- [ ] **Step 1: 실패 테스트 작성**

`src/test/java/com/hackathon/domain/driver/DriverControllerTest.java`:
```java
package com.hackathon.domain.driver;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
class DriverControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("seed 기사 프로필과 다니는 구간을 반환한다")
    void returnsDriverProfile() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/drivers/me")
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.driverId").value(1))
                .andExpect(jsonPath("$.data.vehicleType").value("카고"))
                .andExpect(jsonPath("$.data.capacityTon").value(25.0))
                .andExpect(jsonPath("$.data.minAcceptFare").value(150000))
                .andExpect(jsonPath("$.data.routePreferences.origins").isArray())
                .andExpect(jsonPath("$.data.routePreferences.destinations").isArray());
    }

    @Test
    @DisplayName("X-User-Id 헤더가 없으면 401")
    void rejectsMissingHeader() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/drivers/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("없는 기사면 404")
    void rejectsUnknownDriver() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/drivers/me")
                        .header("X-User-Id", "9999"))
                .andExpect(status().isNotFound());
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew test --tests '*DriverControllerTest'`
Expected: FAIL — 컴파일 에러 (엔티티/컨트롤러 없음)

- [ ] **Step 3: 엔티티와 Repository 작성**

`domain/driver/entity/Direction.java`:
```java
package com.hackathon.domain.driver.entity;

public enum Direction {
    ORIGIN, DESTINATION
}
```

`domain/driver/entity/Driver.java`:
```java
package com.hackathon.domain.driver.entity;

import com.hackathon.global.entity.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Driver extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String phoneNumber;
    private String plateNumber;

    private String vehicleType;      // 카고, 탑차
    private BigDecimal capacityTon;  // 최대 적재량
    private String bodyType;         // 윙바디, 냉장 등

    private BigDecimal rating;
    private Integer totalTrips;
    private Integer completionRate;
    private Integer minAcceptFare;

    private String contactableFrom;  // "06:00"
    private String contactableTo;    // "20:00"
    private String recentTripSummary;

    public static Driver create(String name, String phoneNumber, String plateNumber,
                                String vehicleType, BigDecimal capacityTon, String bodyType,
                                BigDecimal rating, Integer totalTrips, Integer completionRate,
                                Integer minAcceptFare, String contactableFrom, String contactableTo,
                                String recentTripSummary) {
        Driver driver = new Driver();
        driver.name = name;
        driver.phoneNumber = phoneNumber;
        driver.plateNumber = plateNumber;
        driver.vehicleType = vehicleType;
        driver.capacityTon = capacityTon;
        driver.bodyType = bodyType;
        driver.rating = rating;
        driver.totalTrips = totalTrips;
        driver.completionRate = completionRate;
        driver.minAcceptFare = minAcceptFare;
        driver.contactableFrom = contactableFrom;
        driver.contactableTo = contactableTo;
        driver.recentTripSummary = recentTripSummary;
        return driver;
    }
}
```

`domain/driver/entity/DriverRoutePreference.java`:
```java
package com.hackathon.domain.driver.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DriverRoutePreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long driverId;

    @Enumerated(EnumType.STRING)
    private Direction direction;

    private String sido;

    /** null이면 해당 시도 전체를 뜻한다. */
    private String sigungu;

    public static DriverRoutePreference of(Long driverId, Direction direction,
                                           String sido, String sigungu) {
        DriverRoutePreference pref = new DriverRoutePreference();
        pref.driverId = driverId;
        pref.direction = direction;
        pref.sido = sido;
        pref.sigungu = sigungu;
        return pref;
    }
}
```

`domain/driver/repository/DriverRepository.java`:
```java
package com.hackathon.domain.driver.repository;

import com.hackathon.domain.driver.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverRepository extends JpaRepository<Driver, Long> {
}
```

`domain/driver/repository/DriverRoutePreferenceRepository.java`:
```java
package com.hackathon.domain.driver.repository;

import com.hackathon.domain.driver.entity.DriverRoutePreference;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverRoutePreferenceRepository extends JpaRepository<DriverRoutePreference, Long> {

    List<DriverRoutePreference> findByDriverId(Long driverId);

    void deleteByDriverId(Long driverId);
}
```

- [ ] **Step 3-1: Flyway 마이그레이션 작성 (`V2__create_driver_tables.sql`)**

`backend/src/main/resources/db/migration/V2__create_driver_tables.sql`:
```sql
CREATE TABLE driver (
    id                   BIGSERIAL PRIMARY KEY,
    name                 VARCHAR(50) NOT NULL,
    phone_number         VARCHAR(20),
    plate_number         VARCHAR(20),
    vehicle_type         VARCHAR(20),
    capacity_ton         NUMERIC(6,1),
    body_type            VARCHAR(20),
    rating               NUMERIC(3,1),
    total_trips          INTEGER,
    completion_rate      INTEGER,
    min_accept_fare      INTEGER,
    contactable_from     VARCHAR(5),
    contactable_to       VARCHAR(5),
    recent_trip_summary  VARCHAR(100),
    created_at           TIMESTAMP NOT NULL DEFAULT now(),
    updated_at           TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE driver_route_preference (
    id         BIGSERIAL PRIMARY KEY,
    driver_id  BIGINT NOT NULL,
    direction  VARCHAR(20) NOT NULL,
    sido       VARCHAR(20) NOT NULL,
    sigungu    VARCHAR(20)
);
```

> `spring.jpa.hibernate.ddl-auto: validate`라 이 파일 없이는 `bootRun`이 시작하지 못한다 (`./gradlew test`는 H2 + create-drop이라 이 파일과 무관하게 통과한다). `driver_route_preference.driver_id`에는 FK를 걸지 않는다 — Global Constraints의 "`@ManyToOne` 금지, FK 없이 ID 컬럼만 보관" 원칙을 DB 레벨에서도 그대로 따른다.

- [ ] **Step 5: DTO와 Service, Controller 작성**

`domain/driver/dto/RegionPoint.java`:
```java
package com.hackathon.domain.driver.dto;

public record RegionPoint(String sido, String sigungu) {
}
```

`domain/driver/dto/DriverResponse.java`:
```java
package com.hackathon.domain.driver.dto;

import com.hackathon.domain.driver.entity.Direction;
import com.hackathon.domain.driver.entity.Driver;
import com.hackathon.domain.driver.entity.DriverRoutePreference;
import java.math.BigDecimal;
import java.util.List;

public record DriverResponse(
        Long driverId,
        String name,
        String plateNumber,
        String vehicleType,
        BigDecimal capacityTon,
        String bodyType,
        BigDecimal rating,
        Integer totalTrips,
        Integer completionRate,
        Integer minAcceptFare,
        RoutePreferences routePreferences
) {
    public record RoutePreferences(List<RegionPoint> origins, List<RegionPoint> destinations) {
    }

    public static DriverResponse from(Driver driver, List<DriverRoutePreference> preferences) {
        return new DriverResponse(
                driver.getId(),
                driver.getName(),
                driver.getPlateNumber(),
                driver.getVehicleType(),
                driver.getCapacityTon(),
                driver.getBodyType(),
                driver.getRating(),
                driver.getTotalTrips(),
                driver.getCompletionRate(),
                driver.getMinAcceptFare(),
                new RoutePreferences(points(preferences, Direction.ORIGIN),
                        points(preferences, Direction.DESTINATION))
        );
    }

    private static List<RegionPoint> points(List<DriverRoutePreference> preferences, Direction direction) {
        return preferences.stream()
                .filter(p -> p.getDirection() == direction)
                .map(p -> new RegionPoint(p.getSido(), p.getSigungu()))
                .toList();
    }
}
```

`domain/driver/service/DriverService.java`:
```java
package com.hackathon.domain.driver.service;

import com.hackathon.domain.driver.dto.DriverResponse;
import com.hackathon.domain.driver.entity.Driver;
import com.hackathon.domain.driver.repository.DriverRepository;
import com.hackathon.domain.driver.repository.DriverRoutePreferenceRepository;
import com.hackathon.global.exception.BusinessException;
import com.hackathon.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DriverService {

    private final DriverRepository driverRepository;
    private final DriverRoutePreferenceRepository preferenceRepository;

    @Transactional(readOnly = true)
    public DriverResponse findMe(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DRIVER_NOT_FOUND));
        return DriverResponse.from(driver, preferenceRepository.findByDriverId(driverId));
    }
}
```

`domain/driver/controller/DriverController.java`:
```java
package com.hackathon.domain.driver.controller;

import com.hackathon.domain.driver.dto.DriverResponse;
import com.hackathon.domain.driver.service.DriverService;
import com.hackathon.global.auth.LoginUser;
import com.hackathon.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/drivers")
@RequiredArgsConstructor
public class DriverController {

    private final DriverService driverService;

    @GetMapping("/me")
    public ApiResponse<DriverResponse> getMe(@LoginUser Long driverId) {
        return ApiResponse.ok(driverService.findMe(driverId));
    }
}
```

- [ ] **Step 6: seed 데이터 초기화 작성**

`global/config/DataInitializer.java`:
```java
package com.hackathon.global.config;

import com.hackathon.domain.driver.entity.Direction;
import com.hackathon.domain.driver.entity.Driver;
import com.hackathon.domain.driver.entity.DriverRoutePreference;
import com.hackathon.domain.driver.repository.DriverRepository;
import com.hackathon.domain.driver.repository.DriverRoutePreferenceRepository;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final DriverRepository driverRepository;
    private final DriverRoutePreferenceRepository preferenceRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (driverRepository.count() > 0) {
            return;
        }

        // driverId = 1 — 기사 화면의 로그인 사용자
        Driver me = driverRepository.save(Driver.create(
                "박OO", "010-1111-2222", "34나 5678",
                "카고", new BigDecimal("25.0"), "윙바디",
                new BigDecimal("4.6"), 87, 95, 150_000,
                "06:00", "20:00", "수원→부산 · 3일 전"));

        preferenceRepository.saveAll(List.of(
                DriverRoutePreference.of(me.getId(), Direction.ORIGIN, "경기도", "수원시"),
                DriverRoutePreference.of(me.getId(), Direction.ORIGIN, "경기도", "용인시"),
                DriverRoutePreference.of(me.getId(), Direction.ORIGIN, "서울특별시", null),
                DriverRoutePreference.of(me.getId(), Direction.DESTINATION, "부산광역시", null),
                DriverRoutePreference.of(me.getId(), Direction.DESTINATION, "서울특별시", "송파구")
        ));

        // driverId = 2 — 화주 화면 "배차된 기사" 카드용
        driverRepository.save(Driver.create(
                "김OO", "010-3333-4444", "12가 3456",
                "탑차", new BigDecimal("5.0"), "냉장",
                new BigDecimal("4.8"), 132, 98, 450_000,
                "06:00", "20:00", "서울→대구 · 2일 전"));

        log.info("seed 기사 {}명 생성", driverRepository.count());
    }
}
```

- [ ] **Step 7: 테스트 통과 확인**

Run: `./gradlew test --tests '*DriverControllerTest'`
Expected: PASS (3 tests)

---

### Task 4: 다니는 구간 저장 (`PUT /api/v1/drivers/me/route-preferences`)

**Files:**
- Create: `domain/driver/dto/RoutePreferenceRequest.java`
- Modify: `domain/driver/service/DriverService.java` (`updateRoutePreferences` 추가)
- Modify: `domain/driver/controller/DriverController.java` (PUT 핸들러 추가)
- Test: `src/test/java/com/hackathon/domain/driver/RoutePreferenceTest.java`

**Interfaces:**
- Consumes: `RegionPoint`, `DriverResponse`, `DriverService`, `DriverRoutePreferenceRepository` (Task 3)
- Produces:
  - `RoutePreferenceRequest(List<RegionPoint> origins, List<RegionPoint> destinations)`
  - `DriverService.updateRoutePreferences(Long driverId, RoutePreferenceRequest request)` → `DriverResponse`

- [ ] **Step 1: 실패 테스트 작성**

`src/test/java/com/hackathon/domain/driver/RoutePreferenceTest.java`:
```java
package com.hackathon.domain.driver;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
class RoutePreferenceTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @DisplayName("다니는 구간을 전체 교체하고 저장 결과를 반환한다")
    void replacesPreferences() throws Exception {
        String body = """
                {
                  "origins": [
                    {"sido": "충청남도", "sigungu": "천안시"}
                  ],
                  "destinations": [
                    {"sido": "경기도", "sigungu": null}
                  ]
                }
                """;

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/drivers/me/route-preferences")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.routePreferences.origins.length()").value(1))
                .andExpect(jsonPath("$.data.routePreferences.origins[0].sido").value("충청남도"))
                .andExpect(jsonPath("$.data.routePreferences.origins[0].sigungu").value("천안시"))
                .andExpect(jsonPath("$.data.routePreferences.destinations.length()").value(1))
                .andExpect(jsonPath("$.data.routePreferences.destinations[0].sido").value("경기도"))
                .andExpect(jsonPath("$.data.routePreferences.destinations[0].sigungu").doesNotExist());

        // 다시 조회해도 교체된 값이 유지된다
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/drivers/me")
                        .header("X-User-Id", "1"))
                .andExpect(jsonPath("$.data.routePreferences.origins.length()").value(1));
    }

    @Test
    @DisplayName("origins가 비어 있으면 400")
    void rejectsEmptyOrigins() throws Exception {
        String body = """
                {"origins": [], "destinations": [{"sido": "경기도", "sigungu": null}]}
                """;

        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/drivers/me/route-preferences")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
```

> 이 테스트는 seed 데이터를 변경하므로 Task 7의 `/loads` 테스트와 순서 의존이 생길 수 있다. 두 테스트 클래스 모두 자기 테스트 안에서 필요한 구간을 PUT으로 세팅하도록 작성한다 (Task 7 참고).

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew test --tests '*RoutePreferenceTest'`
Expected: FAIL — 404 (PUT 핸들러 없음)

- [ ] **Step 3: 구현**

`domain/driver/dto/RoutePreferenceRequest.java`:
```java
package com.hackathon.domain.driver.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record RoutePreferenceRequest(
        @NotEmpty(message = "출발 구간을 1개 이상 선택해주세요.")
        List<RegionPoint> origins,

        @NotEmpty(message = "도착 구간을 1개 이상 선택해주세요.")
        List<RegionPoint> destinations
) {
}
```

`DriverService`에 추가:
```java
    @Transactional
    public DriverResponse updateRoutePreferences(Long driverId, RoutePreferenceRequest request) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DRIVER_NOT_FOUND));

        preferenceRepository.deleteByDriverId(driverId);
        preferenceRepository.flush();

        List<DriverRoutePreference> saved = new ArrayList<>();
        request.origins().forEach(p ->
                saved.add(DriverRoutePreference.of(driverId, Direction.ORIGIN, p.sido(), p.sigungu())));
        request.destinations().forEach(p ->
                saved.add(DriverRoutePreference.of(driverId, Direction.DESTINATION, p.sido(), p.sigungu())));

        return DriverResponse.from(driver, preferenceRepository.saveAll(saved));
    }
```

필요한 import 추가: `java.util.ArrayList`, `java.util.List`, `com.hackathon.domain.driver.dto.RoutePreferenceRequest`, `com.hackathon.domain.driver.entity.Direction`, `com.hackathon.domain.driver.entity.DriverRoutePreference`.

`DriverController`에 추가:
```java
    @PutMapping("/me/route-preferences")
    public ApiResponse<DriverResponse> updateRoutePreferences(
            @LoginUser Long driverId,
            @Valid @RequestBody RoutePreferenceRequest request) {
        return ApiResponse.ok(driverService.updateRoutePreferences(driverId, request));
    }
```

필요한 import 추가: `jakarta.validation.Valid`, `org.springframework.web.bind.annotation.PutMapping`, `org.springframework.web.bind.annotation.RequestBody`, `com.hackathon.domain.driver.dto.RoutePreferenceRequest`.

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew test --tests '*RoutePreferenceTest'`
Expected: PASS (2 tests)

- [ ] **Step 5: 전체 테스트 확인**

Run: `./gradlew test`
Expected: 지금까지의 모든 테스트 PASS

---

### Task 5: Cargo 엔티티 + 등록/조회 (`POST /api/v1/cargos`, `GET /api/v1/cargos/{id}`)

**Files:**
- Create: `domain/cargo/entity/CargoStatus.java`
- Create: `domain/cargo/entity/Cargo.java`
- Create: `domain/cargo/repository/CargoRepository.java`
- Create: `domain/cargo/dto/CargoCreateRequest.java`
- Create: `domain/cargo/dto/CargoDetailResponse.java`
- Create: `domain/cargo/service/CargoService.java`
- Create: `domain/cargo/controller/CargoController.java`
- Modify: `global/config/DataInitializer.java` (기사 목록용 화물 seed 추가)
- Test: `src/test/java/com/hackathon/domain/cargo/CargoCreateTest.java`

**Interfaces:**
- Consumes: `BaseTimeEntity`, `@LoginUser`, `ApiResponse`, `ErrorCode.CARGO_NOT_FOUND` (Task 1), `RegionPoint` (Task 3)
- Produces:
  - `CargoStatus { REQUESTED, MATCHED, CANCELED }`
  - `Cargo.create(Long shipperId, String originSido, String originSigungu, String destSido, String destSigungu, String cargoType, BigDecimal weightTon, String vehicleType, String bodyType, Integer desiredFare, LocalDateTime loadingAt, LocalDateTime unloadingAt, Integer distanceKm)`
  - `Cargo` getters: `getId/getShipperId/getOriginSido/getOriginSigungu/getDestSido/getDestSigungu/getCargoType/getWeightTon/getVehicleType/getBodyType/getDesiredFare/getLoadingAt/getUnloadingAt/getDistanceKm/getStatus`
  - `Cargo.changeFare(Integer)`, `Cargo.changeSchedule(LocalDateTime, LocalDateTime)`, `Cargo.changeRoute(String,String,String,String,Integer)`, `Cargo.changeCargoSpec(String, BigDecimal, String, String)`
  - `CargoRepository extends JpaRepository<Cargo, Long>`, `findByStatus(CargoStatus)` → `List<Cargo>`
  - `CargoService.create(Long shipperId, CargoCreateRequest)` → `Long cargoId`
  - `CargoService.findDetail(Long cargoId)` → `CargoDetailResponse`
  - `CargoDetailResponse` — `assignedDriver`와 `fare` 필드는 이 태스크에서 항상 `null`. Task 8/11에서 채운다.

- [ ] **Step 1: 실패 테스트 작성**

`src/test/java/com/hackathon/domain/cargo/CargoCreateTest.java`:
```java
package com.hackathon.domain.cargo;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
class CargoCreateTest {

    @Autowired
    MockMvc mockMvc;

    private static final String BODY = """
            {
              "origin": {"sido": "서울특별시", "sigungu": "강남구"},
              "destination": {"sido": "부산광역시", "sigungu": "해운대구"},
              "cargoType": "냉장",
              "weightTon": 5.0,
              "vehicleType": "탑차",
              "bodyType": null,
              "desiredFare": 500000,
              "loadingAt": "2026-08-12T14:00:00",
              "unloadingAt": "2026-08-12T18:00:00",
              "distanceKm": 325
            }
            """;

    @Test
    @DisplayName("화물을 등록하면 REQUESTED 상태로 저장되고 상세 조회가 가능하다")
    void createsAndReadsCargo() throws Exception {
        String response = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/cargos")
                        .header("X-User-Id", "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REQUESTED"))
                .andExpect(jsonPath("$.data.cargoId").isNumber())
                .andReturn().getResponse().getContentAsString();

        String cargoId = response.replaceAll(".*\"cargoId\":(\\d+).*", "$1");

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/cargos/" + cargoId)
                        .header("X-User-Id", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REQUESTED"))
                .andExpect(jsonPath("$.data.origin.sido").value("서울특별시"))
                .andExpect(jsonPath("$.data.destination.sigungu").value("해운대구"))
                .andExpect(jsonPath("$.data.desiredFare").value(500000))
                .andExpect(jsonPath("$.data.assignedDriver").doesNotExist());
    }

    @Test
    @DisplayName("없는 화물을 조회하면 404")
    void rejectsUnknownCargo() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/cargos/999999")
                        .header("X-User-Id", "100"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("희망 운임이 없으면 400")
    void rejectsMissingFare() throws Exception {
        String invalid = BODY.replace("\"desiredFare\": 500000,", "\"desiredFare\": null,");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/cargos")
                        .header("X-User-Id", "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest());
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew test --tests '*CargoCreateTest'`
Expected: FAIL — 컴파일 에러

- [ ] **Step 3: 엔티티와 Repository 작성**

`domain/cargo/entity/CargoStatus.java`:
```java
package com.hackathon.domain.cargo.entity;

public enum CargoStatus {
    REQUESTED, MATCHED, CANCELED
}
```

`domain/cargo/entity/Cargo.java`:
```java
package com.hackathon.domain.cargo.entity;

import com.hackathon.global.entity.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cargo extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long shipperId;

    private String originSido;
    private String originSigungu;
    private String destSido;
    private String destSigungu;

    private String cargoType;
    private BigDecimal weightTon;
    private String vehicleType;
    private String bodyType;

    private Integer desiredFare;
    private LocalDateTime loadingAt;
    private LocalDateTime unloadingAt;
    private Integer distanceKm;

    @Enumerated(EnumType.STRING)
    private CargoStatus status;

    public static Cargo create(Long shipperId,
                               String originSido, String originSigungu,
                               String destSido, String destSigungu,
                               String cargoType, BigDecimal weightTon,
                               String vehicleType, String bodyType,
                               Integer desiredFare,
                               LocalDateTime loadingAt, LocalDateTime unloadingAt,
                               Integer distanceKm) {
        Cargo cargo = new Cargo();
        cargo.shipperId = shipperId;
        cargo.originSido = originSido;
        cargo.originSigungu = originSigungu;
        cargo.destSido = destSido;
        cargo.destSigungu = destSigungu;
        cargo.cargoType = cargoType;
        cargo.weightTon = weightTon;
        cargo.vehicleType = vehicleType;
        cargo.bodyType = bodyType;
        cargo.desiredFare = desiredFare;
        cargo.loadingAt = loadingAt;
        cargo.unloadingAt = unloadingAt;
        cargo.distanceKm = distanceKm;
        cargo.status = CargoStatus.REQUESTED;
        return cargo;
    }

    public void changeFare(Integer desiredFare) {
        this.desiredFare = desiredFare;
    }

    public void changeSchedule(LocalDateTime loadingAt, LocalDateTime unloadingAt) {
        if (loadingAt != null) {
            this.loadingAt = loadingAt;
        }
        if (unloadingAt != null) {
            this.unloadingAt = unloadingAt;
        }
    }

    public void changeRoute(String originSido, String originSigungu,
                            String destSido, String destSigungu, Integer distanceKm) {
        this.originSido = originSido;
        this.originSigungu = originSigungu;
        this.destSido = destSido;
        this.destSigungu = destSigungu;
        if (distanceKm != null) {
            this.distanceKm = distanceKm;
        }
    }

    public void changeCargoSpec(String cargoType, BigDecimal weightTon,
                                String vehicleType, String bodyType) {
        if (cargoType != null) {
            this.cargoType = cargoType;
        }
        if (weightTon != null) {
            this.weightTon = weightTon;
        }
        if (vehicleType != null) {
            this.vehicleType = vehicleType;
        }
        if (bodyType != null) {
            this.bodyType = bodyType;
        }
    }

    public boolean isModifiable() {
        return status == CargoStatus.REQUESTED;
    }
}
```

`domain/cargo/repository/CargoRepository.java`:
```java
package com.hackathon.domain.cargo.repository;

import com.hackathon.domain.cargo.entity.Cargo;
import com.hackathon.domain.cargo.entity.CargoStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CargoRepository extends JpaRepository<Cargo, Long> {

    List<Cargo> findByStatus(CargoStatus status);
}
```

- [ ] **Step 3-1: Flyway 마이그레이션 작성 (`V3__create_cargo_table.sql`)**

`backend/src/main/resources/db/migration/V3__create_cargo_table.sql`:
```sql
CREATE TABLE cargo (
    id              BIGSERIAL PRIMARY KEY,
    shipper_id      BIGINT NOT NULL,
    origin_sido     VARCHAR(20) NOT NULL,
    origin_sigungu  VARCHAR(20),
    dest_sido       VARCHAR(20) NOT NULL,
    dest_sigungu    VARCHAR(20),
    cargo_type      VARCHAR(20),
    weight_ton      NUMERIC(6,1) NOT NULL,
    vehicle_type    VARCHAR(20),
    body_type       VARCHAR(20),
    desired_fare    INTEGER NOT NULL,
    loading_at      TIMESTAMP NOT NULL,
    unloading_at    TIMESTAMP,
    distance_km     INTEGER,
    status          VARCHAR(20) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);
```

> Task 3와 같은 이유로 `bootRun`에는 필요하지만 `./gradlew test`에는 필요 없다.

- [ ] **Step 5: DTO 작성**

`domain/cargo/dto/CargoCreateRequest.java`:
```java
package com.hackathon.domain.cargo.dto;

import com.hackathon.domain.driver.dto.RegionPoint;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CargoCreateRequest(
        @NotNull(message = "출발지를 입력해주세요.") RegionPoint origin,
        @NotNull(message = "도착지를 입력해주세요.") RegionPoint destination,
        @NotNull(message = "화물 종류를 입력해주세요.") String cargoType,
        @NotNull(message = "중량을 입력해주세요.") BigDecimal weightTon,
        String vehicleType,
        String bodyType,
        @NotNull(message = "희망 운임을 입력해주세요.") Integer desiredFare,
        @NotNull(message = "상차 일시를 입력해주세요.") LocalDateTime loadingAt,
        LocalDateTime unloadingAt,
        Integer distanceKm
) {
}
```

`domain/cargo/dto/CargoDetailResponse.java`:
```java
package com.hackathon.domain.cargo.dto;

import com.hackathon.domain.cargo.entity.Cargo;
import com.hackathon.domain.driver.dto.DriverResponse;
import com.hackathon.domain.driver.dto.RegionPoint;
import com.hackathon.domain.fare.dto.FareQuoteResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CargoDetailResponse(
        Long cargoId,
        String status,
        RegionPoint origin,
        RegionPoint destination,
        String cargoType,
        BigDecimal weightTon,
        String vehicleType,
        String bodyType,
        Integer desiredFare,
        LocalDateTime loadingAt,
        LocalDateTime unloadingAt,
        Integer distanceKm,
        FareQuoteResponse fare,
        DriverResponse assignedDriver
) {
    public static CargoDetailResponse of(Cargo cargo, FareQuoteResponse fare, DriverResponse driver) {
        return new CargoDetailResponse(
                cargo.getId(),
                cargo.getStatus().name(),
                new RegionPoint(cargo.getOriginSido(), cargo.getOriginSigungu()),
                new RegionPoint(cargo.getDestSido(), cargo.getDestSigungu()),
                cargo.getCargoType(),
                cargo.getWeightTon(),
                cargo.getVehicleType(),
                cargo.getBodyType(),
                cargo.getDesiredFare(),
                cargo.getLoadingAt(),
                cargo.getUnloadingAt(),
                cargo.getDistanceKm(),
                fare,
                driver
        );
    }
}
```

> `FareQuoteResponse`는 Task 10에서 만든다. 이 태스크에서는 아래 stub을 먼저 생성해 컴파일을 통과시키고, Task 10에서 필드를 채운다.

`domain/fare/dto/FareQuoteResponse.java` (stub):
```java
package com.hackathon.domain.fare.dto;

public record FareQuoteResponse(
        Integer averageFare,
        Integer sameDayThreshold,
        Integer distanceKm,
        String verdict,
        String message
) {
}
```

- [ ] **Step 6: Service, Controller 작성**

`domain/cargo/service/CargoService.java`:
```java
package com.hackathon.domain.cargo.service;

import com.hackathon.domain.cargo.dto.CargoCreateRequest;
import com.hackathon.domain.cargo.dto.CargoDetailResponse;
import com.hackathon.domain.cargo.entity.Cargo;
import com.hackathon.domain.cargo.repository.CargoRepository;
import com.hackathon.global.exception.BusinessException;
import com.hackathon.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CargoService {

    private final CargoRepository cargoRepository;

    @Transactional
    public Long create(Long shipperId, CargoCreateRequest request) {
        Cargo cargo = Cargo.create(
                shipperId,
                request.origin().sido(), request.origin().sigungu(),
                request.destination().sido(), request.destination().sigungu(),
                request.cargoType(), request.weightTon(),
                request.vehicleType(), request.bodyType(),
                request.desiredFare(),
                request.loadingAt(), request.unloadingAt(),
                request.distanceKm());
        return cargoRepository.save(cargo).getId();
    }

    @Transactional(readOnly = true)
    public CargoDetailResponse findDetail(Long cargoId) {
        Cargo cargo = getCargo(cargoId);
        // fare / assignedDriver는 Task 10, Task 8에서 채운다.
        return CargoDetailResponse.of(cargo, null, null);
    }

    @Transactional(readOnly = true)
    public Cargo getCargo(Long cargoId) {
        return cargoRepository.findById(cargoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CARGO_NOT_FOUND));
    }
}
```

`domain/cargo/controller/CargoController.java`:
```java
package com.hackathon.domain.cargo.controller;

import com.hackathon.domain.cargo.dto.CargoCreateRequest;
import com.hackathon.domain.cargo.dto.CargoDetailResponse;
import com.hackathon.domain.cargo.service.CargoService;
import com.hackathon.global.auth.LoginUser;
import com.hackathon.global.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cargos")
@RequiredArgsConstructor
public class CargoController {

    private final CargoService cargoService;

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@LoginUser Long shipperId,
                                                   @Valid @RequestBody CargoCreateRequest request) {
        Long cargoId = cargoService.create(shipperId, request);
        return ApiResponse.ok(Map.of("cargoId", cargoId, "status", "REQUESTED"));
    }

    @GetMapping("/{cargoId}")
    public ApiResponse<CargoDetailResponse> detail(@PathVariable Long cargoId) {
        return ApiResponse.ok(cargoService.findDetail(cargoId));
    }
}
```

- [ ] **Step 7: 기사 목록용 화물 seed 추가**

`DataInitializer.run()` 끝에 추가 (필요한 import: `CargoRepository`, `Cargo`, `LocalDateTime`):
```java
        LocalDateTime base = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0)
                .withSecond(0).withNano(0);

        cargoRepository.saveAll(List.of(
                // 기사(id=1)의 구간에 딱 맞는 화물 — BEST_MATCH 후보
                Cargo.create(100L, "경기도", "수원시", "부산광역시", "강서구",
                        "냉장", new BigDecimal("22.0"), "카고", "윙바디",
                        850_000, base.plusDays(1), base.plusDays(1).plusHours(9), 380),
                // 서울(전체) → 송파구, 최소수락운임 미달 — BELOW_AVERAGE 후보
                Cargo.create(101L, "서울특별시", "강남구", "서울특별시", "송파구",
                        "파렛트", new BigDecimal("5.0"), "탑차", null,
                        95_000, base.plusHours(10), base.plusHours(13), 45),
                // 구간 불일치 — 목록에서 제외되어야 함
                Cargo.create(102L, "광주광역시", "광산구", "전라북도", "전주시",
                        "일반화물", new BigDecimal("11.0"), "카고", null,
                        210_000, base.plusDays(2), base.plusDays(2).plusHours(3), 100),
                // 적재량 초과 — 25t 기사에게는 보이지만 5t 기사에게는 제외
                Cargo.create(103L, "경기도", "용인시", "부산광역시", "해운대구",
                        "일반화물", new BigDecimal("30.0"), "카고", null,
                        1_200_000, base.plusDays(2), base.plusDays(2).plusHours(8), 390)
        ));
```

`DataInitializer`에 `private final CargoRepository cargoRepository;` 필드를 추가하고, 화물 seed 앞에 `if (cargoRepository.count() > 0) return;` 가드를 넣는다.

- [ ] **Step 8: 테스트 통과 확인**

Run: `./gradlew test --tests '*CargoCreateTest'`
Expected: PASS (3 tests)

---

### Task 6: 화물 수정 (`PATCH /api/v1/cargos/{id}`)

**Files:**
- Create: `domain/cargo/dto/CargoUpdateRequest.java`
- Modify: `domain/cargo/service/CargoService.java` (`update` 추가)
- Modify: `domain/cargo/controller/CargoController.java` (PATCH 핸들러 추가)
- Test: `src/test/java/com/hackathon/domain/cargo/CargoUpdateTest.java`

**Interfaces:**
- Consumes: `Cargo`, `CargoService`, `CargoDetailResponse` (Task 5), `ErrorCode.CARGO_NOT_MODIFIABLE` (Task 1)
- Produces:
  - `CargoUpdateRequest(RegionPoint origin, RegionPoint destination, String cargoType, BigDecimal weightTon, String vehicleType, String bodyType, Integer desiredFare, LocalDateTime loadingAt, LocalDateTime unloadingAt, Integer distanceKm)` — 모든 필드 nullable, 보낸 것만 반영
  - `CargoService.update(Long cargoId, CargoUpdateRequest)` → `CargoDetailResponse`

- [ ] **Step 1: 실패 테스트 작성**

`src/test/java/com/hackathon/domain/cargo/CargoUpdateTest.java`:
```java
package com.hackathon.domain.cargo;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hackathon.domain.cargo.entity.Cargo;
import com.hackathon.domain.cargo.repository.CargoRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
class CargoUpdateTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    CargoRepository cargoRepository;

    private Long newCargo() {
        Cargo cargo = Cargo.create(200L, "서울특별시", "강남구", "부산광역시", "해운대구",
                "냉장", new BigDecimal("5.0"), "탑차", null,
                500_000, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(4), 325);
        return cargoRepository.save(cargo).getId();
    }

    @Test
    @DisplayName("운임만 보내면 운임만 바뀐다")
    void updatesFareOnly() throws Exception {
        Long id = newCargo();

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/cargos/" + id)
                        .header("X-User-Id", "200")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"desiredFare\": 620000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.desiredFare").value(620000))
                .andExpect(jsonPath("$.data.cargoType").value("냉장"))
                .andExpect(jsonPath("$.data.origin.sigungu").value("강남구"));
    }

    @Test
    @DisplayName("보내지 않은 필드는 그대로 유지된다")
    void keepsUnsentFields() throws Exception {
        Long id = newCargo();

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/cargos/" + id)
                        .header("X-User-Id", "200")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cargoType\": \"냉동\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cargoType").value("냉동"))
                .andExpect(jsonPath("$.data.desiredFare").value(500000))
                .andExpect(jsonPath("$.data.destination.sigungu").value("해운대구"));
    }
}
```

> `CARGO_NOT_MODIFIABLE`(배차 후 수정 거부) 검증은 화물을 MATCHED로 만드는 수단이 Task 8에서 생기므로, 해당 테스트는 Task 8에서 추가한다.

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew test --tests '*CargoUpdateTest'`
Expected: FAIL — 405 또는 404 (PATCH 핸들러 없음)

- [ ] **Step 3: 구현**

`domain/cargo/dto/CargoUpdateRequest.java`:
```java
package com.hackathon.domain.cargo.dto;

import com.hackathon.domain.driver.dto.RegionPoint;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 모든 필드가 nullable — 보낸 필드만 반영한다. */
public record CargoUpdateRequest(
        RegionPoint origin,
        RegionPoint destination,
        String cargoType,
        BigDecimal weightTon,
        String vehicleType,
        String bodyType,
        Integer desiredFare,
        LocalDateTime loadingAt,
        LocalDateTime unloadingAt,
        Integer distanceKm
) {
}
```

`CargoService`에 추가:
```java
    @Transactional
    public CargoDetailResponse update(Long cargoId, CargoUpdateRequest request) {
        Cargo cargo = getCargo(cargoId);
        if (!cargo.isModifiable()) {
            throw new BusinessException(ErrorCode.CARGO_NOT_MODIFIABLE);
        }

        if (request.desiredFare() != null) {
            cargo.changeFare(request.desiredFare());
        }
        if (request.origin() != null && request.destination() != null) {
            cargo.changeRoute(request.origin().sido(), request.origin().sigungu(),
                    request.destination().sido(), request.destination().sigungu(),
                    request.distanceKm());
        }
        cargo.changeSchedule(request.loadingAt(), request.unloadingAt());
        cargo.changeCargoSpec(request.cargoType(), request.weightTon(),
                request.vehicleType(), request.bodyType());

        return CargoDetailResponse.of(cargo, null, null);
    }
```

필요한 import 추가: `com.hackathon.domain.cargo.dto.CargoUpdateRequest`.

`CargoController`에 추가:
```java
    @PatchMapping("/{cargoId}")
    public ApiResponse<CargoDetailResponse> update(@PathVariable Long cargoId,
                                                   @RequestBody CargoUpdateRequest request) {
        return ApiResponse.ok(cargoService.update(cargoId, request));
    }
```

필요한 import 추가: `org.springframework.web.bind.annotation.PatchMapping`, `com.hackathon.domain.cargo.dto.CargoUpdateRequest`.

- [ ] **Step 4: 테스트 통과 확인**

Run: `./gradlew test --tests '*CargoUpdateTest'`
Expected: PASS (2 tests)

---

### Task 7: 매칭 스코어링 + 화물 목록 (`GET /api/v1/loads`)

이 서비스의 핵심 API. 배지 중 `BEST_MATCH`만 이 태스크에서 구현하고, `BELOW_AVERAGE`는 시세가 생기는 Task 11에서 추가한다.

**Files:**
- Create: `domain/matching/service/MatchScoreCalculator.java`
- Create: `domain/matching/dto/LoadResponse.java`
- Create: `domain/matching/service/LoadService.java`
- Create: `domain/matching/controller/LoadController.java`
- Test: `src/test/java/com/hackathon/domain/matching/MatchScoreCalculatorTest.java`
- Test: `src/test/java/com/hackathon/domain/matching/LoadControllerTest.java`

**Interfaces:**
- Consumes: `Cargo`, `CargoStatus`, `CargoRepository.findByStatus` (Task 5), `Driver`, `DriverRoutePreference`, `Direction`, repositories (Task 3)
- Produces:
  - `MatchScoreCalculator.isEligible(Cargo, Driver, List<DriverRoutePreference>, LocalDateTime now)` → `boolean`
  - `MatchScoreCalculator.calculateScore(Cargo, Driver, List<DriverRoutePreference>, LocalDateTime now)` → `int` (0~100)
  - `LoadResponse(Long cargoId, String origin, String destination, LocalDateTime loadingAt, LocalDateTime unloadingAt, Integer distanceKm, String vehicleType, BigDecimal weightTon, String bodyType, String cargoType, Integer fare, Integer matchScore, String badge, Integer regionAverageFare, Integer belowPercent)`
  - `LoadService.findAvailableLoads(Long driverId)` → `List<LoadResponse>`

**스코어 규칙** (설계 문서 4절 8번과 동일)

```
score = 구간일치(40) + 차량적합(25) + 시간여유(20) + 운임매력(15)
```

- 구간일치 = 출발 20 + 도착 20. 방향별로: 기사가 시군구까지 지정했으면 시군구 일치 20 / 시도만 일치 12, "전체"(`sigungu == null`)로 지정했으면 시도 일치 20, 불일치 0
- 차량적합: `capacityTon >= weightTon`이면 25, 미달이면 **목록 제외**
- 시간여유: 상차까지 24시간 이상 20, 12시간 이상 12, 그 미만 5
- 운임매력: `fare >= minAcceptFare`면 15, 미만이면 `floor(15 * fare / minAcceptFare)`
- 출발·도착 모두 불일치면 **목록 제외**
- 상차 시각이 이미 지난 화물은 **목록 제외** (설계 문서에 없던 추가 규칙 — 지난 화물이 목록에 남으면 데모에서 이상해 보인다)

- [ ] **Step 1: 스코어링 실패 테스트 작성**

`src/test/java/com/hackathon/domain/matching/MatchScoreCalculatorTest.java`:
```java
package com.hackathon.domain.matching;

import static org.assertj.core.api.Assertions.assertThat;

import com.hackathon.domain.cargo.entity.Cargo;
import com.hackathon.domain.driver.entity.Direction;
import com.hackathon.domain.driver.entity.Driver;
import com.hackathon.domain.driver.entity.DriverRoutePreference;
import com.hackathon.domain.matching.service.MatchScoreCalculator;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MatchScoreCalculatorTest {

    private final MatchScoreCalculator calculator = new MatchScoreCalculator();
    private final LocalDateTime now = LocalDateTime.of(2026, 8, 11, 9, 0);

    private Driver driver() {
        return Driver.create("박OO", "010", "34나 5678", "카고",
                new BigDecimal("25.0"), "윙바디", new BigDecimal("4.6"),
                87, 95, 150_000, "06:00", "20:00", "최근 운행");
    }

    private List<DriverRoutePreference> preferences() {
        return List.of(
                DriverRoutePreference.of(1L, Direction.ORIGIN, "경기도", "수원시"),
                DriverRoutePreference.of(1L, Direction.ORIGIN, "서울특별시", null),
                DriverRoutePreference.of(1L, Direction.DESTINATION, "부산광역시", null),
                DriverRoutePreference.of(1L, Direction.DESTINATION, "서울특별시", "송파구"));
    }

    private Cargo cargo(String oSido, String oSigungu, String dSido, String dSigungu,
                        String weightTon, int fare, LocalDateTime loadingAt) {
        return Cargo.create(1L, oSido, oSigungu, dSido, dSigungu,
                "일반화물", new BigDecimal(weightTon), "카고", null,
                fare, loadingAt, loadingAt.plusHours(4), 100);
    }

    @Test
    @DisplayName("설계 문서 검산 1: 수원→부산(전체), 36시간 뒤, 운임 충분 = 100점")
    void perfectMatch() {
        Cargo cargo = cargo("경기도", "수원시", "부산광역시", "강서구",
                "22.0", 850_000, now.plusHours(36));

        int score = calculator.calculateScore(cargo, driver(), preferences(), now);

        assertThat(score).isEqualTo(100); // 20 + 20 + 25 + 20 + 15
    }

    @Test
    @DisplayName("설계 문서 검산 2: 서울(전체)→송파구, 10시간 뒤, 운임 미달 = 79점")
    void belowMinAcceptFare() {
        Cargo cargo = cargo("서울특별시", "강남구", "서울특별시", "송파구",
                "5.0", 95_000, now.plusHours(10));

        int score = calculator.calculateScore(cargo, driver(), preferences(), now);

        // 20(서울 전체) + 20(송파구 정확) + 25 + 5(10시간) + 9(floor(15*95000/150000))
        assertThat(score).isEqualTo(79);
    }

    @Test
    @DisplayName("시도만 일치하면 방향당 12점")
    void sidoOnlyMatch() {
        Cargo cargo = cargo("경기도", "평택시", "부산광역시", "강서구",
                "10.0", 900_000, now.plusHours(36));

        int score = calculator.calculateScore(cargo, driver(), preferences(), now);

        assertThat(score).isEqualTo(92); // 12 + 20 + 25 + 20 + 15
    }

    @Test
    @DisplayName("적재량이 부족하면 목록에서 제외")
    void excludesOverweight() {
        Cargo cargo = cargo("경기도", "수원시", "부산광역시", "강서구",
                "30.0", 900_000, now.plusHours(36));

        assertThat(calculator.isEligible(cargo, driver(), preferences(), now)).isFalse();
    }

    @Test
    @DisplayName("출발·도착 모두 구간 밖이면 제외")
    void excludesUnmatchedRoute() {
        Cargo cargo = cargo("광주광역시", "광산구", "전라북도", "전주시",
                "10.0", 900_000, now.plusHours(36));

        assertThat(calculator.isEligible(cargo, driver(), preferences(), now)).isFalse();
    }

    @Test
    @DisplayName("상차 시각이 지난 화물은 제외")
    void excludesPastLoadingTime() {
        Cargo cargo = cargo("경기도", "수원시", "부산광역시", "강서구",
                "10.0", 900_000, now.minusHours(1));

        assertThat(calculator.isEligible(cargo, driver(), preferences(), now)).isFalse();
    }

    @Test
    @DisplayName("한쪽 방향만 맞으면 목록에는 남는다")
    void keepsPartialMatch() {
        Cargo cargo = cargo("경기도", "수원시", "제주특별자치도", "제주시",
                "10.0", 900_000, now.plusHours(36));

        assertThat(calculator.isEligible(cargo, driver(), preferences(), now)).isTrue();
        assertThat(calculator.calculateScore(cargo, driver(), preferences(), now))
                .isEqualTo(80); // 20 + 0 + 25 + 20 + 15
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew test --tests '*MatchScoreCalculatorTest'`
Expected: FAIL — 컴파일 에러 (`MatchScoreCalculator` 없음)

- [ ] **Step 3: `MatchScoreCalculator` 구현**

`domain/matching/service/MatchScoreCalculator.java`:
```java
package com.hackathon.domain.matching.service;

import com.hackathon.domain.cargo.entity.Cargo;
import com.hackathon.domain.driver.entity.Direction;
import com.hackathon.domain.driver.entity.Driver;
import com.hackathon.domain.driver.entity.DriverRoutePreference;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class MatchScoreCalculator {

    private static final int DIRECTION_EXACT = 20;
    private static final int DIRECTION_SIDO_ONLY = 12;
    private static final int VEHICLE_FIT = 25;
    private static final int FARE_MAX = 15;

    /** 목록에 노출할 화물인지 판정한다. */
    public boolean isEligible(Cargo cargo, Driver driver,
                              List<DriverRoutePreference> preferences, LocalDateTime now) {
        if (cargo.getLoadingAt() == null || !cargo.getLoadingAt().isAfter(now)) {
            return false;
        }
        if (driver.getCapacityTon().compareTo(cargo.getWeightTon()) < 0) {
            return false;
        }
        int origin = directionPoint(cargo.getOriginSido(), cargo.getOriginSigungu(),
                preferences, Direction.ORIGIN);
        int destination = directionPoint(cargo.getDestSido(), cargo.getDestSigungu(),
                preferences, Direction.DESTINATION);
        return origin + destination > 0;
    }

    /** 0~100 매칭 점수. isEligible이 true인 화물에만 호출한다. */
    public int calculateScore(Cargo cargo, Driver driver,
                              List<DriverRoutePreference> preferences, LocalDateTime now) {
        int route = directionPoint(cargo.getOriginSido(), cargo.getOriginSigungu(),
                preferences, Direction.ORIGIN)
                + directionPoint(cargo.getDestSido(), cargo.getDestSigungu(),
                preferences, Direction.DESTINATION);

        int vehicle = driver.getCapacityTon().compareTo(cargo.getWeightTon()) >= 0 ? VEHICLE_FIT : 0;

        return route + vehicle + timeSlackPoint(cargo.getLoadingAt(), now)
                + farePoint(cargo.getDesiredFare(), driver.getMinAcceptFare());
    }

    private int directionPoint(String sido, String sigungu,
                               List<DriverRoutePreference> preferences, Direction direction) {
        List<DriverRoutePreference> sameSido = preferences.stream()
                .filter(p -> p.getDirection() == direction)
                .filter(p -> Objects.equals(p.getSido(), sido))
                .toList();

        if (sameSido.isEmpty()) {
            return 0;
        }
        boolean exact = sameSido.stream()
                .anyMatch(p -> p.getSigungu() == null || Objects.equals(p.getSigungu(), sigungu));
        return exact ? DIRECTION_EXACT : DIRECTION_SIDO_ONLY;
    }

    private int timeSlackPoint(LocalDateTime loadingAt, LocalDateTime now) {
        long hours = Duration.between(now, loadingAt).toHours();
        if (hours >= 24) {
            return 20;
        }
        if (hours >= 12) {
            return 12;
        }
        return 5;
    }

    private int farePoint(Integer fare, Integer minAcceptFare) {
        if (minAcceptFare == null || minAcceptFare <= 0 || fare >= minAcceptFare) {
            return FARE_MAX;
        }
        return (int) Math.floor((double) FARE_MAX * fare / minAcceptFare);
    }
}
```

- [ ] **Step 4: 스코어링 테스트 통과 확인**

Run: `./gradlew test --tests '*MatchScoreCalculatorTest'`
Expected: PASS (7 tests)

- [ ] **Step 5: `/loads` 실패 테스트 작성**

`src/test/java/com/hackathon/domain/matching/LoadControllerTest.java`:
```java
package com.hackathon.domain.matching;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
class LoadControllerTest {

    @Autowired
    MockMvc mockMvc;

    /** 다른 테스트가 구간을 바꿔놨을 수 있으므로 매번 seed 구간으로 되돌린다. */
    @BeforeEach
    void resetPreferences() throws Exception {
        String body = """
                {
                  "origins": [
                    {"sido": "경기도", "sigungu": "수원시"},
                    {"sido": "경기도", "sigungu": "용인시"},
                    {"sido": "서울특별시", "sigungu": null}
                  ],
                  "destinations": [
                    {"sido": "부산광역시", "sigungu": null},
                    {"sido": "서울특별시", "sigungu": "송파구"}
                  ]
                }
                """;
        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/drivers/me/route-preferences")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @Test
    @DisplayName("구간에 맞는 화물만 점수순으로 반환하고 최상위에 BEST_MATCH를 붙인다")
    void returnsScoredLoads() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/loads")
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].badge").value("BEST_MATCH"))
                .andExpect(jsonPath("$.data[0].origin").value("경기 수원시"))
                .andExpect(jsonPath("$.data[0].destination").value("부산 강서구"))
                .andExpect(jsonPath("$.data[0].matchScore").value(100))
                // 광주→전주(구간 밖)는 목록에 없어야 한다
                .andExpect(jsonPath("$.data[?(@.origin == '광주 광산구')]").isEmpty());
    }

    @Test
    @DisplayName("점수는 내림차순으로 정렬된다")
    void sortedByScoreDesc() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/loads")
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String json = result.getResponse().getContentAsString();
                    java.util.regex.Matcher m =
                            java.util.regex.Pattern.compile("\"matchScore\":(\\d+)").matcher(json);
                    int previous = Integer.MAX_VALUE;
                    while (m.find()) {
                        int score = Integer.parseInt(m.group(1));
                        if (score > previous) {
                            throw new AssertionError("정렬이 깨졌습니다: " + json);
                        }
                        previous = score;
                    }
                });
    }

    @Test
    @DisplayName("구간을 바꾸면 결과가 비어 목록이 없다")
    void emptyWhenNoMatch() throws Exception {
        String body = """
                {
                  "origins": [{"sido": "제주특별자치도", "sigungu": null}],
                  "destinations": [{"sido": "제주특별자치도", "sigungu": null}]
                }
                """;
        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/drivers/me/route-preferences")
                .header("X-User-Id", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/loads")
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
```

- [ ] **Step 6: 테스트 실패 확인**

Run: `./gradlew test --tests '*LoadControllerTest'`
Expected: FAIL — 404 (`/api/v1/loads` 없음)

- [ ] **Step 7: DTO / Service / Controller 구현**

`domain/matching/dto/LoadResponse.java`:
```java
package com.hackathon.domain.matching.dto;

import com.hackathon.domain.cargo.entity.Cargo;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LoadResponse(
        Long cargoId,
        String origin,
        String destination,
        LocalDateTime loadingAt,
        LocalDateTime unloadingAt,
        Integer distanceKm,
        String vehicleType,
        BigDecimal weightTon,
        String bodyType,
        String cargoType,
        Integer fare,
        Integer matchScore,
        String badge,
        Integer regionAverageFare,
        Integer belowPercent
) {
    public static LoadResponse of(Cargo cargo, int matchScore) {
        return new LoadResponse(
                cargo.getId(),
                label(cargo.getOriginSido(), cargo.getOriginSigungu()),
                label(cargo.getDestSido(), cargo.getDestSigungu()),
                cargo.getLoadingAt(),
                cargo.getUnloadingAt(),
                cargo.getDistanceKm(),
                cargo.getVehicleType(),
                cargo.getWeightTon(),
                cargo.getBodyType(),
                cargo.getCargoType(),
                cargo.getDesiredFare(),
                matchScore,
                null, null, null
        );
    }

    public LoadResponse withBadge(String badge) {
        return new LoadResponse(cargoId, origin, destination, loadingAt, unloadingAt,
                distanceKm, vehicleType, weightTon, bodyType, cargoType, fare,
                matchScore, badge, regionAverageFare, belowPercent);
    }

    public LoadResponse withBelowAverage(Integer regionAverageFare, Integer belowPercent) {
        return new LoadResponse(cargoId, origin, destination, loadingAt, unloadingAt,
                distanceKm, vehicleType, weightTon, bodyType, cargoType, fare,
                matchScore, "BELOW_AVERAGE", regionAverageFare, belowPercent);
    }

    /** "서울특별시 강남구" → "서울 강남구" 처럼 화면 표기용으로 줄인다. */
    private static String label(String sido, String sigungu) {
        String shortSido = sido
                .replace("특별자치도", "")
                .replace("특별시", "")
                .replace("광역시", "")
                .replace("자치도", "")
                .replace("도", "");
        return sigungu == null ? shortSido : shortSido + " " + sigungu;
    }
}
```

> 주의: `label()`의 `.replace("도", "")`는 "충청남도"→"충청남"까지만 줄이면 되지만 "성동구" 같은 시군구에는 적용되지 않는다(시도 문자열에만 쓴다). "강원도"→"강원", "경기도"→"경기"로 동작한다.

`domain/matching/service/LoadService.java`:
```java
package com.hackathon.domain.matching.service;

import com.hackathon.domain.cargo.entity.Cargo;
import com.hackathon.domain.cargo.entity.CargoStatus;
import com.hackathon.domain.cargo.repository.CargoRepository;
import com.hackathon.domain.driver.entity.Driver;
import com.hackathon.domain.driver.entity.DriverRoutePreference;
import com.hackathon.domain.driver.repository.DriverRepository;
import com.hackathon.domain.driver.repository.DriverRoutePreferenceRepository;
import com.hackathon.domain.matching.dto.LoadResponse;
import com.hackathon.global.exception.BusinessException;
import com.hackathon.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoadService {

    private final CargoRepository cargoRepository;
    private final DriverRepository driverRepository;
    private final DriverRoutePreferenceRepository preferenceRepository;
    private final MatchScoreCalculator scoreCalculator;

    @Transactional(readOnly = true)
    public List<LoadResponse> findAvailableLoads(Long driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DRIVER_NOT_FOUND));
        List<DriverRoutePreference> preferences = preferenceRepository.findByDriverId(driverId);
        LocalDateTime now = LocalDateTime.now();

        List<LoadResponse> loads = new ArrayList<>();
        for (Cargo cargo : cargoRepository.findByStatus(CargoStatus.REQUESTED)) {
            if (!scoreCalculator.isEligible(cargo, driver, preferences, now)) {
                continue;
            }
            loads.add(LoadResponse.of(cargo,
                    scoreCalculator.calculateScore(cargo, driver, preferences, now)));
        }

        loads.sort(Comparator.comparingInt(LoadResponse::matchScore).reversed());

        if (!loads.isEmpty()) {
            loads.set(0, loads.get(0).withBadge("BEST_MATCH"));
        }
        return loads;
    }
}
```

`domain/matching/controller/LoadController.java`:
```java
package com.hackathon.domain.matching.controller;

import com.hackathon.domain.matching.dto.LoadResponse;
import com.hackathon.domain.matching.service.LoadService;
import com.hackathon.global.auth.LoginUser;
import com.hackathon.global.response.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/loads")
@RequiredArgsConstructor
public class LoadController {

    private final LoadService loadService;

    @GetMapping
    public ApiResponse<List<LoadResponse>> getLoads(@LoginUser Long driverId) {
        return ApiResponse.ok(loadService.findAvailableLoads(driverId));
    }
}
```

- [ ] **Step 8: 테스트 통과 확인**

Run: `./gradlew test --tests '*LoadControllerTest'`
Expected: PASS (3 tests)

---

### Task 8: 화물 수락 + 동시성 (`POST /api/v1/loads/{cargoId}/accept`)

**Files:**
- Create: `domain/matching/entity/Assignment.java`
- Create: `domain/matching/repository/AssignmentRepository.java`
- Create: `domain/matching/dto/AcceptResponse.java`
- Modify: `domain/cargo/repository/CargoRepository.java` (`updateStatusIf` 추가)
- Modify: `domain/matching/service/LoadService.java` (`accept` 추가)
- Modify: `domain/matching/controller/LoadController.java` (POST 핸들러 추가)
- Modify: `domain/cargo/service/CargoService.java` (`findDetail`에 배차 기사 채우기)
- Modify: `src/test/java/com/hackathon/domain/cargo/CargoUpdateTest.java` (배차 후 수정 거부 테스트 추가)
- Test: `src/test/java/com/hackathon/domain/matching/AcceptConcurrencyTest.java`

**Interfaces:**
- Consumes: `Cargo`, `CargoStatus`, `CargoRepository` (Task 5), `Driver`, `DriverResponse` (Task 3), `LoadService` (Task 7)
- Produces:
  - `Assignment.of(Long cargoId, Long driverId)` — `getId()`, `getCargoId()`, `getDriverId()`, `getAcceptedAt()`
  - `AssignmentRepository.findByCargoId(Long)` → `Optional<Assignment>`
  - `CargoRepository.updateStatusIf(Long id, CargoStatus from, CargoStatus to)` → `int` (영향 행 수)
  - `AcceptResponse(Long assignmentId, Long cargoId, String status)`
  - `LoadService.accept(Long driverId, Long cargoId)` → `AcceptResponse`

- [ ] **Step 1: 동시성 실패 테스트 작성**

`src/test/java/com/hackathon/domain/matching/AcceptConcurrencyTest.java`:
```java
package com.hackathon.domain.matching;

import static org.assertj.core.api.Assertions.assertThat;

import com.hackathon.domain.cargo.entity.Cargo;
import com.hackathon.domain.cargo.entity.CargoStatus;
import com.hackathon.domain.cargo.repository.CargoRepository;
import com.hackathon.domain.matching.repository.AssignmentRepository;
import com.hackathon.domain.matching.service.LoadService;
import com.hackathon.global.exception.BusinessException;
import com.hackathon.global.exception.ErrorCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AcceptConcurrencyTest {

    @Autowired
    LoadService loadService;

    @Autowired
    CargoRepository cargoRepository;

    @Autowired
    AssignmentRepository assignmentRepository;

    private Long newCargo() {
        return cargoRepository.save(Cargo.create(300L,
                "경기도", "수원시", "부산광역시", "강서구",
                "일반화물", new BigDecimal("3.0"), "카고", null,
                800_000, LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(2).plusHours(8), 380
        )).getId();
    }

    @Test
    @DisplayName("두 기사가 동시에 수락하면 한 명만 성공한다")
    void onlyOneDriverWins() throws Exception {
        Long cargoId = newCargo();

        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger conflict = new AtomicInteger();

        for (long driverId : new long[]{1L, 2L}) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    loadService.accept(driverId, cargoId);
                    success.incrementAndGet();
                } catch (BusinessException e) {
                    if (e.getErrorCode() == ErrorCode.CARGO_ALREADY_MATCHED) {
                        conflict.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await(5, TimeUnit.SECONDS);
        start.countDown();
        done.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(success.get()).isEqualTo(1);
        assertThat(conflict.get()).isEqualTo(1);
        assertThat(cargoRepository.findById(cargoId).orElseThrow().getStatus())
                .isEqualTo(CargoStatus.MATCHED);
        assertThat(assignmentRepository.findByCargoId(cargoId)).isPresent();
    }

    @Test
    @DisplayName("적재량이 부족한 기사는 수락할 수 없다")
    void rejectsOverweight() {
        Long cargoId = cargoRepository.save(Cargo.create(300L,
                "경기도", "수원시", "부산광역시", "강서구",
                "일반화물", new BigDecimal("20.0"), "카고", null,
                800_000, LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(2).plusHours(8), 380
        )).getId();

        // driverId=2는 5t 탑차
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> loadService.accept(2L, cargoId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VEHICLE_NOT_SUITABLE);
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `./gradlew test --tests '*AcceptConcurrencyTest'`
Expected: FAIL — 컴파일 에러 (`accept`, `Assignment` 없음)

- [ ] **Step 3: Assignment 엔티티와 Repository 작성**

`domain/matching/entity/Assignment.java`:
```java
package com.hackathon.domain.matching.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private Long cargoId;

    private Long driverId;

    private LocalDateTime acceptedAt;

    public static Assignment of(Long cargoId, Long driverId) {
        Assignment assignment = new Assignment();
        assignment.cargoId = cargoId;
        assignment.driverId = driverId;
        assignment.acceptedAt = LocalDateTime.now();
        return assignment;
    }
}
```

`domain/matching/repository/AssignmentRepository.java`:
```java
package com.hackathon.domain.matching.repository;

import com.hackathon.domain.matching.entity.Assignment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    Optional<Assignment> findByCargoId(Long cargoId);
}
```

`domain/matching/dto/AcceptResponse.java`:
```java
package com.hackathon.domain.matching.dto;

public record AcceptResponse(Long assignmentId, Long cargoId, String status) {
}
```

- [ ] **Step 3-1: Flyway 마이그레이션 작성 (`V4__create_assignment_table.sql`)**

`backend/src/main/resources/db/migration/V4__create_assignment_table.sql`:
```sql
CREATE TABLE assignment (
    id           BIGSERIAL PRIMARY KEY,
    cargo_id     BIGINT NOT NULL UNIQUE,
    driver_id    BIGINT NOT NULL,
    accepted_at  TIMESTAMP NOT NULL
);
```

> `cargo_id UNIQUE`가 동시 수락 방어의 마지막 안전판이다 — `updateStatusIf`의 조건부 UPDATE가 주 방어선이고, 이 제약은 애플리케이션 버그로 두 번 INSERT되는 것까지 막는다.

- [ ] **Step 5: 조건부 UPDATE 추가**

`CargoRepository`에 추가:
```java
    /**
     * status가 from일 때만 to로 바꾼다. 영향 행 수가 0이면 다른 기사가 먼저 가져간 것.
     * 별도의 락 없이 동시 수락을 막는다.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Cargo c SET c.status = :to WHERE c.id = :id AND c.status = :from")
    int updateStatusIf(@Param("id") Long id,
                       @Param("from") CargoStatus from,
                       @Param("to") CargoStatus to);
```

필요한 import 추가: `org.springframework.data.jpa.repository.Modifying`, `org.springframework.data.jpa.repository.Query`, `org.springframework.data.repository.query.Param`.

- [ ] **Step 6: `accept` 구현**

`LoadService`에 추가 (`AssignmentRepository` 필드도 주입):
```java
    @Transactional
    public AcceptResponse accept(Long driverId, Long cargoId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DRIVER_NOT_FOUND));
        Cargo cargo = cargoRepository.findById(cargoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CARGO_NOT_FOUND));

        if (driver.getCapacityTon().compareTo(cargo.getWeightTon()) < 0) {
            throw new BusinessException(ErrorCode.VEHICLE_NOT_SUITABLE);
        }

        int updated = cargoRepository.updateStatusIf(
                cargoId, CargoStatus.REQUESTED, CargoStatus.MATCHED);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.CARGO_ALREADY_MATCHED);
        }

        Assignment assignment = assignmentRepository.save(Assignment.of(cargoId, driverId));
        return new AcceptResponse(assignment.getId(), cargoId, CargoStatus.MATCHED.name());
    }
```

필요한 import 추가: `Assignment`, `AssignmentRepository`, `AcceptResponse`.

`LoadController`에 추가:
```java
    @PostMapping("/{cargoId}/accept")
    public ApiResponse<AcceptResponse> accept(@LoginUser Long driverId,
                                              @PathVariable Long cargoId) {
        return ApiResponse.ok(loadService.accept(driverId, cargoId));
    }
```

필요한 import 추가: `org.springframework.web.bind.annotation.PostMapping`, `org.springframework.web.bind.annotation.PathVariable`, `AcceptResponse`.

- [ ] **Step 7: 화주 조회에 배차된 기사 채우기**

`CargoService.findDetail`을 다음으로 교체 (`AssignmentRepository`, `DriverRepository`, `DriverRoutePreferenceRepository` 주입):
```java
    @Transactional(readOnly = true)
    public CargoDetailResponse findDetail(Long cargoId) {
        Cargo cargo = getCargo(cargoId);
        DriverResponse assignedDriver = assignmentRepository.findByCargoId(cargoId)
                .flatMap(a -> driverRepository.findById(a.getDriverId()))
                .map(d -> DriverResponse.from(d, preferenceRepository.findByDriverId(d.getId())))
                .orElse(null);
        // fare는 Task 11에서 채운다.
        return CargoDetailResponse.of(cargo, null, assignedDriver);
    }
```

그리고 `CargoUpdateTest`에 배차 후 수정 거부 테스트를 추가한다 (이제 `updateStatusIf`가 존재한다):

```java
    @Autowired
    LoadService loadService;

    @Test
    @DisplayName("배차 완료된 화물은 수정할 수 없다")
    void rejectsMatchedCargo() throws Exception {
        Long id = newCargo();
        loadService.accept(1L, id); // 25t 기사가 5t 화물을 수락

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/cargos/" + id)
                        .header("X-User-Id", "200")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"desiredFare\": 620000}"))
                .andExpect(status().isBadRequest());
    }
```

> `newCargo()`의 상차 시각이 미래여야 `accept`가 통과한다 — Task 6에서 이미 `LocalDateTime.now().plusDays(1)`로 잡아뒀다.

- [ ] **Step 8: 테스트 통과 확인**

Run: `./gradlew test --tests '*AcceptConcurrencyTest' --tests '*CargoUpdateTest'`
Expected: PASS (동시성 2 + 수정 3)

- [ ] **Step 9: 전체 흐름 수동 확인 (AI 없이 도는지)**

Run: `./gradlew bootRun` 후 다른 터미널에서
```bash
# 1. 기사 목록
curl -s -H 'X-User-Id: 1' localhost:8080/api/v1/loads | head -c 600; echo

# 2. 최상위 화물 수락 (cargoId는 위 응답에서 확인)
curl -s -X POST -H 'X-User-Id: 1' localhost:8080/api/v1/loads/1/accept; echo

# 3. 화주가 배차 확인
curl -s -H 'X-User-Id: 100' localhost:8080/api/v1/cargos/1 | head -c 600; echo
```
Expected: 3번 응답의 `status`가 `MATCHED`, `assignedDriver.name`이 `박OO`.

> **여기까지가 데모 안전판이다.** Claude API가 죽어도 기사 화면 전체와 화주의 등록·배차 확인은 동작한다.

---

### Task 9: Claude 클라이언트 + 자연어 파싱

**Files:**
- Create: `global/ai/ClaudeClient.java`
- Create: `domain/cargo/dto/ParseRequest.java`, `domain/cargo/dto/ParsedCargoResponse.java`
- Create: `domain/cargo/service/CargoParseService.java`
- Modify: `domain/cargo/controller/CargoController.java`
- Test: `src/test/java/com/hackathon/domain/cargo/CargoParseServiceTest.java`

**핵심 결정**

`claude-opus-5`를 **structured outputs**(`outputConfig(Class)`)로 호출한다. 스키마가 POJO에서 자동 생성되고 응답이 타입 안전하게 돌아오므로 JSON 문자열 파싱·재시도 루프·"JSON만 출력해라" 프롬프트가 전부 불필요하다. 첫 호출만 스키마 컴파일로 느리고 이후 24시간 캐시된다.

Claude가 채우는 레코드는 날짜를 **문자열로** 받고 서버에서 `LocalDateTime.parse`한다. 날짜 타입을 직접 스키마에 넣으면 Jackson 직렬화가 까다로워진다.

`RawParsed` 필드는 전부 nullable. 문장에 없는 항목은 null로 두라고 프롬프트에 명시하고, **출발지·도착지만 필수 검증**한다 (없으면 `AI_PARSING_FAILED`). 나머지는 프론트가 "고치기"로 채운다.

프롬프트에는 `LocalDate.now()`를 넣어 "내일" 같은 상대 날짜를 절대 시각으로 바꾸게 한다. 시·도는 정식 명칭으로 정규화("서울"→"서울특별시")하도록 규칙을 준다 — 매칭 스코어가 시도 문자열 일치로 동작하기 때문에 이게 어긋나면 목록이 비어 보인다.

**테스트:** 실제 Claude를 호출하므로 `@EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")`를 붙여 키 없는 환경에서는 자동으로 건너뛴다.

**응답 시간 주의:** Opus 5는 thinking이 기본 ON이고 effort 기본값이 `high`라 파싱 한 번에 수 초 걸릴 수 있다. 데모에서 느리면 수동 스키마 경로(`OutputConfig.builder().effort(...).format(JsonOutputFormat...)`)로 바꿔 effort를 낮춘다. 먼저 실측하고 판단한다.

- [ ] Step 1: `ClaudeClient.extract(systemPrompt, userMessage, Class<T>)` 작성 — `AnthropicOkHttpClient.fromEnv()`로 클라이언트 생성, 예외는 `AI_CALL_FAILED`로 감싼다
- [ ] Step 2: 파싱 실패 테스트 3건 작성 (정상 파싱 / 일부 항목 null / 구간 없으면 422)
- [ ] Step 3: 테스트 실패 확인 — `./gradlew test --tests '*CargoParseServiceTest'`
- [ ] Step 4: `CargoParseService.parse()` 구현 — 프롬프트 + `RawParsed` 레코드 + 구간 검증 + 날짜 변환
- [ ] Step 5: 컨트롤러에 parse 핸들러 연결
- [ ] Step 6: 테스트 통과 확인. 실패하면 프롬프트 규칙 문구를 조정하고 재실행
- [ ] Step 7: `curl`로 `"내일 서울→부산 냉장 5톤 예산 50만원"` 파싱 결과 눈으로 확인

---

### Task 10: 구간 시세 + 캐시

**Files:**
- Create: `domain/fare/entity/FareQuote.java`, `domain/fare/repository/FareQuoteRepository.java`
- Modify: `domain/fare/dto/FareQuoteResponse.java` (Task 5 stub 채우기)
- Create: `domain/fare/service/FareQuoteService.java`, `domain/fare/controller/FareController.java`
- Test: `src/test/java/com/hackathon/domain/fare/FareQuoteServiceTest.java`

**핵심 결정**

LLM은 호출마다 값이 흔들린다. `quoteKey = originSido|destSido|cargoType|weightTon`을 UNIQUE로 잡고 **DB 캐시 우선 조회**해서, 같은 구간이면 데모 내내 같은 숫자가 나오게 한다. 이게 "LLM 시세 추정"의 재현성 문제를 해결하는 핵심이다.

Claude에게 `averageFare`, `sameDayThreshold`, `distanceKm` 세 값을 한 번에 받는다. 프롬프트에 "임계는 평균의 80~90% 수준, 반드시 평균 이하"라고 명시하되, **모델이 규칙을 어길 경우를 코드로 방어**한다 (`Math.min(threshold, average)`). 프롬프트만 믿지 않는다.

`verdict`는 서버가 계산한다 — `desiredFare >= sameDayThreshold`면 `FAIR`, 아니면 `SLOW`. 화면 문구("지금 금액으로는 매칭이 하루 이상 걸릴 수 있어요 / 620,000원부터는...")도 서버에서 조립해 내려보낸다. `desiredFare`가 null이면 verdict/message 모두 null.

- [ ] Step 1: 캐시 동작 테스트 작성 — 같은 구간 2회 호출 시 **행이 1개만 늘고** 두 값이 동일한지 검증
- [ ] Step 2: verdict 분기 테스트 2건 (`SLOW` / `FAIR`) + 평균 ≥ 임계 불변식 테스트
- [ ] Step 3: 테스트 실패 확인
- [ ] Step 4: `FareQuote` 엔티티 (`quoteKey` UNIQUE) + Repository 작성
- [ ] Step 4-1: Flyway 마이그레이션 작성 (`V5__create_fare_quote_table.sql`)

```sql
CREATE TABLE fare_quote (
    id                  BIGSERIAL PRIMARY KEY,
    quote_key           VARCHAR(150) NOT NULL UNIQUE,
    average_fare        INTEGER NOT NULL,
    same_day_threshold  INTEGER NOT NULL,
    distance_km         INTEGER,
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP NOT NULL DEFAULT now()
);
```

- [ ] Step 6: `FareQuoteResponse.of(quote, desiredFare)` 팩토리로 verdict/message 조립
- [ ] Step 7: `FareQuoteService.quote()` 구현 — 캐시 조회 → miss면 Claude 호출 후 저장
- [ ] Step 8: 컨트롤러 연결, 테스트 통과 확인

---

### Task 11: 시세를 화물 조회와 목록 배지에 연결

**Files:**
- Modify: `domain/cargo/service/CargoService.java`
- Modify: `domain/matching/service/LoadService.java`
- Test: `src/test/java/com/hackathon/domain/matching/BelowAverageBadgeTest.java`

**핵심 결정**

Task 5/8에서 `null`로 비워둔 `CargoDetailResponse.fare`와, Task 7에서 미룬 `BELOW_AVERAGE` 배지를 이제 채운다.

배지 규칙: `fare < averageFare × 0.8`이면 `BELOW_AVERAGE`, `belowPercent = round((average - fare) / average × 100)`. **`BEST_MATCH`와 겹치면 `BELOW_AVERAGE`가 우선한다** — 기사에게 불리한 정보를 먼저 보여준다.

**AI 장애가 화면을 막지 않게 한다.** 시세 조회를 `try/catch`로 감싸고, 실패하면 배지 없이 목록을 그대로 내려보낸다. 화물 상세도 `fare: null`로 응답한다. 발표 당일 API가 죽어도 기사 화면과 배차 흐름은 살아 있어야 한다.

목록에서 화물마다 `quote()`를 부르지만 캐시가 있으면 DB만 읽으므로 Claude 호출은 **구간당 한 번**뿐이다.

- [ ] Step 1: `BELOW_AVERAGE` 배지 테스트 작성 — seed의 강남구→송파구 화물(95,000원)이 45km 구간 시세의 80% 미만이라 배지가 붙는지
- [ ] Step 2: 화물 상세에 `fare.verdict` / `fare.message`가 실리는지 테스트 작성
- [ ] Step 3: 테스트 실패 확인 (배지가 `BEST_MATCH`, `fare`가 null)
- [ ] Step 4: `LoadService`에 `FareQuoteService` 주입 후 배지 계산 로직 추가 (try/catch 포함)
- [ ] Step 5: `CargoService`에 `quoteOrNull(cargo)` 헬퍼 추가하고 `findDetail`/`update`에 연결
- [ ] Step 6: 테스트 통과 확인
- [ ] Step 7: 전체 테스트 실행 — `ANTHROPIC_API_KEY=<키> ./gradlew test`
- [ ] Step 8: `bootRun` 후 10개 엔드포인트 전수 `curl` 확인, Swagger UI에서도 10개가 보이는지 확인

---

## 이번 범위 밖

- 회원가입 · 로그인
- 기사 등록 화면 (seed 데이터 전제)
- 운송중 · 완료 상태 및 정산
- **숨기기 서버 저장 (TODO)** — 필요해지면 `HiddenLoad(driverId, cargoId)` 테이블과 `POST /loads/{id}/hide`를 추가하고 `LoadService.findAvailableLoads`에서 제외한다. 현재는 클라이언트 로컬 상태로 충분하다.
- 실시간 푸시 (폴링으로 대체)
