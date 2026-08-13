# Project Guide

## 프로젝트 구조

```text
./
├── backend/          Java 25 · Spring Boot 3.5 · Gradle
├── frontend/         Node 22 · pnpm 9 · React 19 · TypeScript 5.9 · Vite 7
├── docs/             기획·API·배포·데이터 문서
├── scripts/          Mock 데이터 생성과 API 검증 스크립트
├── compose.yaml      로컬 PostgreSQL/PostGIS · Redis
└── compose.prod.yaml EC2 blue/green 운영 구성
```

`docs/`와 `scripts/`는 관련 문서 또는 스크립트를 처음 추가할 때 생성한다.

## 로컬 실행

저장소 루트에서 환경 변수 파일을 만들고 인프라를 실행한다.

```bash
cp .env.example .env
docker compose up -d postgres redis
```

백엔드를 실행한다.

```bash
cd backend
./gradlew bootRun
```

별도 터미널에서 프론트엔드를 실행한다.

```bash
cd frontend
corepack enable
corepack prepare pnpm@9.15.9 --activate
pnpm install --frozen-lockfile
pnpm dev
```

## 브랜치 전략

```text
main <- develop <- feature
```

- `main`: 배포 가능한 안정 버전
- `develop`: 기능 통합 브랜치
- `feature/*`: 기능별 작업 브랜치

## 커밋 컨벤션

- 커밋 제목과 본문은 한글로 작성한다.
- 제목은 `<type> : <변경 사항 요약>` 형식을 사용한다.
- 본문에는 핵심 변경 내용을 글머리표로 2~3줄 요약한다.
- 서로 다른 성격의 변경은 가능한 한 별도 커밋으로 분리한다.

사용 가능한 `type`은 다음과 같다.

- `feat`: 새로운 기능 추가
- `bugfix`: 버그 수정
- `refactor`: 기능 변경 없는 코드 구조 개선
- `docs`: 문서 추가 또는 수정
- `test`: 테스트 추가 또는 수정
- `chore`: 설정, 의존성, 기타 유지보수
- `ci`: CI/CD 구성 변경
- `style`: 포맷, 스타일 등 동작에 영향 없는 변경
- `perf`: 성능 개선

예시는 다음과 같다.

```text
feat : 화주 페이지 구성

- 화주 조회 기능 추가
- 화주 생성 기능 추가
- 화주 삭제 기능 추가
```

## CI/CD

- PR과 `develop`의 애플리케이션 변경은 CI에서 백엔드 테스트와 프론트엔드 lint/build를 수행한다.
- `main`의 애플리케이션·배포 구성 변경은 배포 워크플로에서 동일한 검증을 수행한 뒤 GHCR 이미지를
  만들고 EC2 self-hosted runner에서 blue/green 방식으로 배포한다. 문서 전용 변경은 배포하지 않는다.
- AWS 액세스 키를 저장소나 GitHub Actions에 저장하지 않는다.
- 배포 구성과 복구 절차는 `docs/deployment.md`를 따른다.
