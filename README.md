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

- 비정형 화물 요청 AI 파싱, 직접 입력 및 등록 전 수정
- 화주 화물의 진행 중·완료 상태별 목록과 상세 조회
- 화물 조건과 기사 활동 지역을 반영한 추천 화물 조회
- 기사 활동 지역 및 차량·운행 조건 프로필 수정
- 추천 화물 숨기기, 수락, 수락 취소 및 하차 완료
- 하차 완료 후 다음 연결 화물 추천
- 구간 평균 운임 비교 및 비정상 저가 운임 경고
- 화주 목록 페이지네이션과 기사 추천 목록 무한 스크롤

## 서비스 데모

- [서비스 데모 영상 보기](https://www.youtube.com/watch?v=ds4PwnvFsMI)

## 기술 스택

- Backend: Java 25, Spring Boot 3.5, Gradle, JPA, Flyway, PostgreSQL/PostGIS, Redis, Swagger
- Frontend: Node.js 22, pnpm 9, React 19, TypeScript 5.9, Vite 7, Emotion 11, TanStack Query 5
- Test: JUnit, Vitest 4, Testing Library, Storybook 10, Playwright 1.57

## 디렉터리

```text
backend/          Spring Boot API
frontend/         React 웹 애플리케이션과 정적 퍼블리싱 문서
docs/             배포 및 예외 처리 문서
deploy/           Nginx, 배포 상태, APM 정적 파일
scripts/          배포·운영 및 데이터 관련 스크립트
compose.yaml      로컬 PostgreSQL/PostGIS 및 Redis
compose.prod.yaml EC2 blue/green 운영 구성
```

## 로컬 실행

저장소 루트에서 환경 변수 파일을 만들고 PostgreSQL과 Redis를 실행합니다.

```bash
cp .env.example .env
docker compose up -d postgres redis
```

백엔드 실행:

```bash
cd backend
./gradlew bootRun
```

프론트엔드 실행:

```bash
cd frontend
corepack enable
corepack prepare pnpm@9.15.9 --activate
pnpm install --frozen-lockfile
pnpm dev
```

개발 서버의 `/api` 요청은 현재 `frontend/vite.config.ts`에 설정된 백엔드로 프록시됩니다.

## 화면 경로

- `/`: 화주·기사 역할 선택
- `/shipper`: 화주 화물 목록 및 등록
- `/shipper/cargos/:cargoId`: 화주 화물 상세
- `/driver`: 기사 활동 지역 및 추천 화물
- `/driver/profile`: 기사 정보 수정

## 접속 주소

- 운영 서비스: https://43.202.205.211/
- Frontend: http://localhost:5173
- Backend: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- Health check: http://localhost:8080/actuator/health

## 테스트

```bash
cd backend
./gradlew test

cd ../frontend
pnpm lint
pnpm test
pnpm build
pnpm build-storybook
```

Playwright E2E 테스트는 `cd frontend && pnpm test:e2e`로 별도 실행합니다.

DB 스키마 변경은 `backend/src/main/resources/db/migration`에 Flyway SQL 파일로 추가합니다.
시도·시군구 목록의 기준일, 좌표 산출 방식과 운영 반영 절차는
[`docs/region-data.md`](docs/region-data.md)를 참고합니다.

## 배포

PR과 `develop` 브랜치의 애플리케이션 변경은 CI에서 백엔드 테스트, 프론트엔드 lint·build,
Storybook build를 수행합니다. `main` 브랜치의 애플리케이션·배포 구성 변경은 배포 워크플로에서
변경된 애플리케이션을 검증하고 GHCR 이미지를 만든 뒤, EC2 self-hosted runner에서 blue/green으로
교체합니다. 최초 EC2 및 GitHub 설정과 복구 절차는 `docs/deployment.md`를 참고합니다.
