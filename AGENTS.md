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

## CI/CD

- PR과 `develop`, `main` 푸시에서는 백엔드 테스트와 프론트엔드 lint/build를 수행한다.
- `main` 푸시는 GHCR 이미지를 만든 뒤 EC2 self-hosted runner에서 blue/green 방식으로 배포한다.
- AWS 액세스 키를 저장소나 GitHub Actions에 저장하지 않는다.
- 배포 구성과 복구 절차는 `docs/deployment.md`를 따른다.
