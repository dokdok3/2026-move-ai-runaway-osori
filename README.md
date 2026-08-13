# 물류 해커톤

화주와 화물차 기사를 직접 연결하는 AI 기반 화물 매칭 서비스입니다.

## 서비스 소개

카카오톡, 전화 메모, 엑셀 등으로 전달되는 비정형 화물 요청을 AI가
출발지, 도착지, 화물 종류, 중량, 운임, 일정 등의 구조화된 데이터로
변환합니다.

구조화된 요청과 기사의 활동 지역, 이동 거리, 차량 조건, 희망 운임을
기반으로 적합한 화물과 기사를 추천합니다. 제시 운임이 해당 구간의
평균보다 지나치게 낮으면 경고하여 다단계 주선 과정의 정보 비대칭을
줄이는 것이 목표입니다.

### 핵심 기능

- 비정형 화물 요청 AI 파싱 및 수정
- 화물 조건과 기사 활동 지역을 반영한 양방향 랭킹 추천
- 거리, 차량 조건, 운임을 이용한 최적 기사 매칭
- 구간 평균 운임 비교 및 비정상 저가 운임 경고
- 화주용 요청·매칭 화면과 기사용 추천 화물 화면 제공

## 기술 스택

- Backend: Java 25, Spring Boot 3.5, Gradle, JPA, Flyway, PostgreSQL/PostGIS, Redis, Swagger
- Frontend: Node.js 22, pnpm 9, React 19, TypeScript 5.9, Vite 7, Emotion 11, TanStack Query 5
- Test: JUnit, Storybook 10, Playwright 1.57

## 디렉터리

```text
backend/       Spring Boot API
frontend/      React 웹 애플리케이션
compose.yaml   PostgreSQL/PostGIS 및 Redis
```

## 로컬 실행

먼저 PostgreSQL과 Redis를 실행합니다.

```bash
docker compose up -d
```

백엔드 실행:

```bash
cd backend
./gradlew bootRun
```

프론트엔드 실행:

```bash
corepack enable
corepack prepare pnpm@9.15.9 --activate
cd frontend
pnpm install
pnpm dev
```

## 접속 주소

- Frontend: http://localhost:5173
- Backend: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- Health check: http://localhost:8080/actuator/health

## 테스트

```bash
cd backend && ./gradlew test
cd frontend && pnpm lint && pnpm build
```

DB 스키마 변경은 `backend/src/main/resources/db/migration`에 Flyway SQL 파일로 추가합니다.
