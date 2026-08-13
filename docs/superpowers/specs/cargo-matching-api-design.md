# 화물 매칭 플랫폼 — 백엔드 API 설계

- 작성일: 2026-08-11
- 프로젝트: 집나간 벌꿀오소리 (hackathon) · 카카오모빌리티 해커톤
- 범위: 백엔드 API 설계 (구현 계획은 별도 문서)

## 1. 개요

화주가 운송할 화물을 등록하면, 기사가 조건에 맞는 화물 목록을 보고 직접 수락하는 매칭 서비스다.

회원가입·로그인·회원정보 수정 API는 이 설계에 포함하지 않는다. 모든 사용자는 이미 가입되어 있다고 가정하고, 요청 헤더의 `X-User-Id`로 식별한다.

### 화면 흐름 (hifi 프로토타입 기준)

**화주**
1. 카톡·전화로 받은 문장을 그대로 붙여넣기 → 항목별로 정리
2. 정리된 화물 정보 확인 및 "고치기" → 구간 시세와 비교한 운임 코칭 → 등록
3. 배차된 기사 카드 확인 → 전화 걸기

**기사**
1. "다니는 구간" 설정 (출발·도착 시도/시군구, 복수 선택)
2. 조건에 맞는 화물 목록을 매칭 점수순으로 확인
3. 수락 또는 숨기기

### AI 적용 지점

| 지점 | 방식 | 위치 |
|---|---|---|
| 자연어 → 구조화 파싱 | Claude API | `POST /cargos/parse` |
| 구간 운임 시세 추정 | Claude API + DB 캐시 | `GET /fares/quote` |
| 화물↔기사 매칭 점수 | 룰 기반 스코어링 | `GET /loads` 내부 |

매칭 점수를 룰 기반으로 둔 이유: 응답이 빨라 목록 API에 적합하고, "왜 이 화물이 위에 있는가"를 심사 때 바로 설명할 수 있다. AI는 파싱과 시세 두 곳으로 충분하다.

## 2. 도메인 모델

### 패키지 구조

```
com.hackathon/
├── domain/
│   ├── cargo/          # 화물 요청 — 등록, 조회, 수정
│   ├── driver/         # 기사 프로필, 다니는 구간
│   ├── matching/       # 스코어링, 수락
│   ├── fare/           # 구간 시세 (LLM + 캐시)
│   └── region/         # 시도·시군구
└── global/
    ├── ai/             # Claude 클라이언트
    ├── exception/      # BusinessException, ErrorCode, GlobalExceptionHandler
    ├── response/       # ApiResponse<T>
    ├── config/         # CORS, Swagger, Security(permitAll), ArgumentResolver
    └── util/
```

도메인 단위로 묶고, 각 도메인 아래에 `controller / service / repository / dto / entity`를 둔다.

### 엔티티

**Cargo** — 화물 요청

| 필드 | 타입 | 비고 |
|---|---|---|
| id | Long | |
| shipperId | Long | 화주 (FK 없이 ID만) |
| originSido / originSigungu | String | |
| destSido / destSigungu | String | |
| cargoType | String | 냉장, 일반화물, 파렛트 등 |
| weightTon | BigDecimal | |
| vehicleType | String | 카고, 탑차, 윙바디 |
| desiredFare | Integer | 희망 운임 (원) |
| loadingAt / unloadingAt | LocalDateTime | 상차 / 하차 |
| distanceKm | Integer | 등록 시 계산·저장 |
| status | CargoStatus | REQUESTED / MATCHED / CANCELED |

**Driver** — 기사 프로필 (seed 데이터 전제, 등록 화면 없음)

| 필드 | 비고 |
|---|---|
| id, name, phoneNumber, plateNumber | |
| vehicleType, capacityTon, bodyType | 차종·최대적재량·적재함 |
| rating, totalTrips, completionRate | 평점 4.8 / 132건 / 98% |
| minAcceptFare | 최소 수락 운임 |
| contactableFrom / contactableTo | 연락 가능 시간 |
| recentTripSummary | "서울→대구 · 2일 전" |

**DriverRoutePreference** — 다니는 구간

| 필드 | 비고 |
|---|---|
| driverId | |
| direction | ORIGIN / DESTINATION |
| sido | |
| sigungu | **null이면 해당 시도 "전체"** |

별도 테이블로 분리한 이유: 프로토타입에서 출발지에 `경기도 / 서울특별시 / 충청남도` + `수원시 / 용인시 / 전체`처럼 복수 선택이 가능하다. 컬럼으로는 표현할 수 없다.

**FareQuote** — 구간 시세 캐시

| 필드 | 비고 |
|---|---|
| quoteKey | 구간+화물조건 해시 (UNIQUE) |
| averageFare | 구간 평균 운임 |
| sameDayThreshold | 당일 매칭 임계 운임 |
| distanceKm | |
| createdAt | |

LLM은 호출마다 값이 흔들린다. 같은 조건이면 캐시에서 같은 값을 돌려줘서 **데모 중 재현성**을 확보한다. `quoteKey`는 `originSido|destSido|cargoType|weightTon` 조합으로 만든다.

**Assignment** — 배차

| 필드 |
|---|
| cargoId (UNIQUE), driverId, acceptedAt |

### 상태 전이

```
REQUESTED ──기사 수락──> MATCHED
    │
    └──화주 취소──> CANCELED
```

운송중·완료 상태는 화면에 없으므로 두지 않는다. 기사 카드의 "완료율 98% · 132건"은 `Driver`의 누적 컬럼(seed 값)에서 읽는다.

### 설계 결정

- **"숨기기"는 서버에 저장하지 않는다.** 프로토타입에서 "다시 불러오기"를 누르면 숨긴 카드가 되돌아온다(`backup.innerHTML` 복원). 클라이언트 로컬 상태로 충분하다.
  - **TODO**: 숨김이 세션 간 유지되어야 한다면 `HiddenLoad(driverId, cargoId)` 테이블과 `POST /loads/{id}/hide`를 추가한다. 이번 범위에서는 제외한다.
- **화주는 폴링으로 배차를 확인한다.** WebSocket을 붙일 규모가 아니고, 프로토타입의 "다시 찾기" 버튼이 곧 재조회다.

## 3. 공통 규약

### 인증

`X-User-Id` 헤더로 사용자를 식별한다. `HandlerMethodArgumentResolver`가 `@LoginUser Long userId`로 주입하므로, Service 계층은 `HttpServletRequest`를 받지 않는다.

```java
@GetMapping("/loads")
public ApiResponse<List<LoadResponse>> getLoads(@LoginUser Long driverId) { ... }
```

Security는 해커톤 초기 설정대로 `permitAll()`로 열어둔다.

### 응답 래퍼

모든 응답은 `ApiResponse<T>`로 감싼다.

```json
{ "success": true, "data": { ... }, "message": null }
{ "success": false, "data": null, "message": "이미 배차된 화물입니다." }
```

### 에러 코드

| 코드 | HTTP | 상황 |
|---|---|---|
| `CARGO_NOT_FOUND` | 404 | 화물 없음 |
| `CARGO_ALREADY_MATCHED` | 409 | 두 기사가 동시에 수락 |
| `CARGO_NOT_MODIFIABLE` | 400 | 배차 완료 후 수정 시도 |
| `VEHICLE_NOT_SUITABLE` | 400 | 적재량 미달 기사가 수락 |
| `AI_PARSING_FAILED` | 422 | 문장에서 출발지·도착지를 못 읽음 |
| `DRIVER_NOT_FOUND` | 404 | 기사 없음 |

## 4. API 명세

총 10개.

| # | 메서드 | 경로 | 대상 | 화면 |
|---|---|---|---|---|
| 1 | POST | `/api/v1/cargos/parse` | 화주 | 01 |
| 2 | GET | `/api/v1/fares/quote` | 화주 | 02 |
| 3 | POST | `/api/v1/cargos` | 화주 | 02 |
| 4 | GET | `/api/v1/cargos/{id}` | 화주 | 03 |
| 5 | PATCH | `/api/v1/cargos/{id}` | 화주 | 02 |
| 6 | GET | `/api/v1/drivers/me` | 기사 | 구간 설정 |
| 7 | PUT | `/api/v1/drivers/me/route-preferences` | 기사 | 구간 설정 |
| 8 | GET | `/api/v1/loads` | 기사 | 화물 목록 |
| 9 | POST | `/api/v1/loads/{cargoId}/accept` | 기사 | 화물 목록 |
| 10 | GET | `/api/v1/regions` | 공통 | 드롭다운 |

---

### 1. 자연어 파싱 — `POST /api/v1/cargos/parse`

받은 문장을 항목별로 나눈다. **저장하지 않는다.** 화면 02를 채우는 용도다.

```json
// Request
{ "rawText": "내일 서울→부산 냉장 5톤 예산 50만원" }

// Response
{
  "origin": { "sido": "서울특별시", "sigungu": null },
  "destination": { "sido": "부산광역시", "sigungu": null },
  "cargoType": "냉장",
  "weightTon": 5.0,
  "vehicleType": null,
  "desiredFare": 500000,
  "loadingAt": "2026-08-12T14:00:00",
  "unloadingAt": "2026-08-12T18:00:00"
}
```

- 읽지 못한 항목은 `null`로 내려보내고, 프론트가 "고치기"로 채우게 한다.
- "내일" 같은 상대 날짜는 서버의 오늘 날짜를 프롬프트에 함께 넣어 절대 시각으로 변환한다.
- 출발지·도착지를 못 읽으면 `AI_PARSING_FAILED` (422). 나머지 항목은 null 허용.

### 2. 구간 시세 — `GET /api/v1/fares/quote`

```
GET /api/v1/fares/quote
  ?originSido=서울특별시&destSido=부산광역시&cargoType=냉장&weightTon=5&desiredFare=500000
```

```json
{
  "averageFare": 720000,
  "sameDayThreshold": 620000,
  "distanceKm": 325,
  "verdict": "SLOW",
  "message": "지금 금액으로는 매칭이 하루 이상 걸릴 수 있어요."
}
```

- `verdict`: `FAIR`(희망운임 ≥ sameDayThreshold) / `SLOW`(미만)
- `quoteKey`로 `FareQuote`를 먼저 조회하고, 없을 때만 Claude를 호출한 뒤 저장한다.
- 파싱(#1)과 합치지 않고 분리한다. "운임 올리고 다시 찾기"에서 시세를 단독으로 다시 조회해야 하기 때문이다.

### 3. 화물 등록 — `POST /api/v1/cargos`

"고치기"까지 끝난 최종본을 저장한다.

```json
// Request
{
  "origin": { "sido": "서울특별시", "sigungu": "강남구" },
  "destination": { "sido": "부산광역시", "sigungu": "해운대구" },
  "cargoType": "냉장", "weightTon": 5.0, "vehicleType": "탑차",
  "desiredFare": 500000,
  "loadingAt": "2026-08-12T14:00:00",
  "unloadingAt": "2026-08-12T18:00:00"
}

// Response
{ "cargoId": 12, "status": "REQUESTED" }
```

### 4. 화물 상세 — `GET /api/v1/cargos/{id}`

화면 03이 폴링하는 엔드포인트. `status`가 `MATCHED`면 `assignedDriver`가 채워진다.

```json
{
  "cargoId": 12,
  "status": "MATCHED",
  "origin": { "sido": "서울특별시", "sigungu": "강남구" },
  "destination": { "sido": "부산광역시", "sigungu": "해운대구" },
  "cargoType": "냉장", "weightTon": 5.0, "desiredFare": 500000,
  "loadingAt": "2026-08-12T14:00:00", "unloadingAt": "2026-08-12T18:00:00",
  "fare": { "averageFare": 720000, "sameDayThreshold": 620000, "verdict": "SLOW",
            "message": "지금 금액으로는 매칭이 하루 이상 걸릴 수 있어요." },
  "assignedDriver": {
    "name": "김OO", "plateNumber": "12가 3456", "vehicle": "냉장탑차 5t",
    "rating": 4.8, "totalTrips": 132, "completionRate": 98,
    "serviceAreas": ["수도권", "영남권"],
    "preferredDestinations": ["부산", "경남권"],
    "recentTrip": "서울→대구 · 2일 전",
    "contactableHours": "06:00-20:00",
    "minAcceptFare": 450000,
    "phoneNumber": "010-0000-0000",
    "fareBadge": "적정 운임"
  }
}
```

`REQUESTED` 상태면 `assignedDriver`는 `null`이다.

### 5. 화물 수정 — `PATCH /api/v1/cargos/{id}`

"운임 올리고 다시 찾기"를 포함한 범용 수정. 보낸 필드만 반영한다.

```json
// Request
{ "desiredFare": 620000 }

// Response — #4와 동일한 형태 (갱신된 시세 판정 포함)
{ "cargoId": 12, "status": "REQUESTED", "desiredFare": 620000,
  "fare": { "averageFare": 720000, "sameDayThreshold": 620000, "verdict": "FAIR" }, ... }
```

- `status`가 `MATCHED`면 `CARGO_NOT_MODIFIABLE` (400).
- 구간이나 화물 종류가 바뀌면 시세를 다시 조회한다.

### 6. 기사 프로필 — `GET /api/v1/drivers/me`

```json
{
  "driverId": 3, "name": "박OO", "plateNumber": "34나 5678",
  "vehicleType": "카고", "capacityTon": 25.0, "bodyType": "윙바디",
  "rating": 4.6, "totalTrips": 87, "completionRate": 95,
  "minAcceptFare": 150000,
  "routePreferences": {
    "origins": [ { "sido": "경기도", "sigungu": "수원시" },
                 { "sido": "서울특별시", "sigungu": null } ],
    "destinations": [ { "sido": "부산광역시", "sigungu": null } ]
  }
}
```

### 7. 다니는 구간 저장 — `PUT /api/v1/drivers/me/route-preferences`

전체 교체 방식.

```json
{
  "origins": [ { "sido": "경기도", "sigungu": "수원시" },
               { "sido": "경기도", "sigungu": "용인시" },
               { "sido": "서울특별시", "sigungu": null } ],
  "destinations": [ { "sido": "부산광역시", "sigungu": null },
                    { "sido": "서울특별시", "sigungu": "송파구" } ]
}
```

`sigungu: null`은 해당 시도 "전체"를 뜻한다.

### 8. 잡을 수 있는 화물 — `GET /api/v1/loads`

이 서비스의 핵심 API. 기사에게 저장된 구간을 기준으로 `REQUESTED` 화물을 골라 점수순으로 내린다.

아래 예시는 **6번의 기사**(카고 25t 윙바디, `minAcceptFare` 150,000원, 출발 `경기 수원시 / 경기 용인시 / 서울 전체`, 도착 `부산 전체 / 서울 송파구`)를 기준으로 한다.

```json
[
  { "cargoId": 21,
    "origin": "경기 수원시", "destination": "부산 강서구",
    "loadingAt": "2026-08-13T09:00:00", "unloadingAt": "2026-08-13T18:00:00",
    "distanceKm": 380,
    "vehicleType": "카고", "weightTon": 22, "bodyType": "윙바디", "cargoType": "냉장",
    "fare": 850000,
    "matchScore": 100,
    "badge": "BEST_MATCH" },

  { "cargoId": 22,
    "origin": "서울 강남구", "destination": "서울 송파구",
    "loadingAt": "2026-08-12T16:30:00", "unloadingAt": "2026-08-12T19:00:00",
    "distanceKm": 45,
    "vehicleType": "탑차", "weightTon": 5, "bodyType": null, "cargoType": "파렛트",
    "fare": 95000,
    "matchScore": 79,
    "badge": "BELOW_AVERAGE",
    "regionAverageFare": 150000,
    "belowPercent": 37 }
]
```

**스코어링**

```
score = 구간일치(40) + 차량적합(25) + 시간여유(20) + 운임매력(15)
```

| 항목 | 배점 규칙 |
|---|---|
| 구간일치 | **출발 20 + 도착 20.** 각 방향마다: 기사가 시군구까지 지정했으면 시군구 일치 20 / 시도만 일치 12, 기사가 "전체"(`sigungu = null`)로 지정했으면 시도 일치 20. 불일치 0 |
| 차량적합 | `capacityTon >= weightTon`이면 25, **미달이면 목록에서 제외** |
| 시간여유 | 상차 시각까지 24시간 이상 20, 12시간 이상 12, 그 미만 5 |
| 운임매력 | `fare >= minAcceptFare`면 15, 미만이면 `15 × (fare / minAcceptFare)`로 감점 |

출발·도착 모두 불일치인 화물은 목록에서 제외한다.

예시 검산:
- `cargoId 21` = 출발 20(수원 정확) + 도착 20(부산 전체) + 25 + 20(36시간 뒤) + 15(850,000 ≥ 150,000) = **100**
- `cargoId 22` = 출발 20(서울 전체) + 도착 20(송파구 정확) + 25 + 5(10시간 뒤) + 9(95,000/150,000) = **79**

**배지**

- `BEST_MATCH`: 점수 최상위 1건
- `BELOW_AVERAGE`: `fare < averageFare × 0.8`. 이때 `regionAverageFare`와 `belowPercent`를 함께 내린다.
- 두 조건이 겹치면 `BELOW_AVERAGE`가 우선한다 (기사에게 불리한 정보를 먼저 보여준다)

빈 배열이면 프론트가 "조건에 맞는 화물이 없습니다"를 띄운다.

### 9. 화물 수락 — `POST /api/v1/loads/{cargoId}/accept`

```json
{ "assignmentId": 7, "cargoId": 21, "status": "MATCHED" }
```

**동시성 처리**: 두 기사가 같은 화물을 동시에 수락할 수 있다. 조건부 UPDATE의 영향 행 수로 판정한다.

```sql
UPDATE cargo SET status = 'MATCHED' WHERE id = ? AND status = 'REQUESTED'
```

영향 행 수가 0이면 이미 다른 기사가 가져간 것이므로 `CARGO_ALREADY_MATCHED` (409). 별도 락 설정 없이 처리된다.

적재량이 부족한 기사가 수락하면 `VEHICLE_NOT_SUITABLE` (400).

### 10. 지역 목록 — `GET /api/v1/regions`

드롭다운용 시도·시군구 트리. 정적 데이터를 그대로 내린다.

```json
[
  { "sido": "서울특별시", "sigungus": ["강남구", "송파구", "..."] },
  { "sido": "경기도", "sigungus": ["수원시", "용인시", "..."] }
]
```

## 5. 구현 순서

CLAUDE.md의 "Entity 먼저" 원칙을 따른다.

1. 프로젝트 셋업 — Gradle, `ApiResponse`, `ErrorCode`, `GlobalExceptionHandler`, `BaseTimeEntity`
2. 엔티티 5개 + Repository
3. seed 데이터 — 기사, 지역, 화물 목록
4. 기사 API (#6, #7, #10) — AI 없이 동작 확인
5. 화물 등록·조회 API (#3, #4, #5)
6. 매칭 API (#8, #9) — 스코어링, 동시성
7. Claude 연동 (#1, #2) — 파싱, 시세 캐시

4~6번을 먼저 끝내면 AI 없이도 전체 흐름이 도는 상태가 된다. 데모 안전판이 된다.

## 6. 이번 범위 밖

- 회원가입·로그인 (가입 완료 가정)
- 기사 등록 화면 (seed 데이터 전제)
- 운송중·완료 상태 및 정산
- 숨기기 서버 저장 (TODO, 위 2절 참고)
- 실시간 푸시 (폴링으로 대체)
