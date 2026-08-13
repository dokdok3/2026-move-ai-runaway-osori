# Frontend Code Conventions

Claude와 Codex가 이 프로젝트의 프론트엔드 코드를 작성·리뷰할 때 따르는 규칙이다. `CLAUDE.md`는 이 파일의 심볼릭 링크다.

## 폴더/파일 구조

컴포넌트는 폴더 단위로 응집시킨다.

```text
src/
├── components/
│   └── Badge/
│       ├── Badge.tsx
│       ├── Badge.stories.tsx
│       ├── Badge.test.tsx   (필요 시)
│       └── index.ts         (export { Badge } from './Badge')
├── api/
│   ├── schema.gen.ts   (OpenAPI 스펙에서 생성한 타입, 직접 수정 금지)
│   ├── client.ts        (openapi-fetch 클라이언트)
│   └── user/
│       ├── model.ts    (파라미터/응답 타입, schema.gen.ts 참조)
│       ├── api.ts      (apiClient 호출)
│       ├── queries.ts  (query key factory + queryOptions/mutationOptions)
│       └── service.ts  (컴포넌트가 쓰는 훅)
├── theme/
│   └── theme.ts              (Emotion ThemeProvider용 디자인 토큰)
├── mocks/
│   ├── browser.ts            (MSW 워커 설정)
│   ├── handlers/              (도메인별 MSW 핸들러)
│   └── data/                  (목데이터 CSV + 파싱/변환)
└── test/
    └── setup.ts               (Vitest + Testing Library 설정)
```

- 파일명: `PascalCase.tsx` — 컴포넌트명과 동일하게 짓는다.
- export: **named export**를 기본으로 한다. default export는 쓰지 않는다 (`react-refresh/only-export-components` 룰과도 궁합이 맞음).
- 폴더 안 `index.ts`에서 재export해서 상위에서는 `@/components/Badge`로 import한다.

## import 규칙

절대경로 alias `@/*` (`src/*`)를 쓴다. 상대경로는 같은 폴더 내부(`./Badge.styles`)에만 허용한다.

import 순서는 그룹 사이에 빈 줄로 구분한다.

```ts
// 1. 외부 라이브러리
import { useState } from 'react'
import styled from '@emotion/styled'

// 2. 내부 절대경로 모듈 (@/)
import { Badge } from '@/components/Badge'
import { useUserQuery } from '@/api/user/service'

// 3. 같은 폴더 상대경로
import type { CardProps } from './Card.types'
```

## TypeScript

- 객체 타입은 `interface`를 기본으로 한다. 함수 시그니처나 유니언처럼 `interface`로 표현할 수 없는 경우에만 `type`을 쓴다.
  - 예외: `api/<도메인>/model.ts`의 API 파라미터·응답 타입(`Get*Params`, `*Request`, `*Response`)은 `type`을 쓴다. 자세한 네이밍 규칙은 `api-convention` 스킬을 따른다.
- `any`는 금지한다. 타입을 모르는 값은 `unknown`으로 받고 타입 가드로 좁혀서 사용한다.
- Props 타입은 컴포넌트 파일 안에 `{ComponentName}Props`로 선언한다.

## 컴포넌트

```tsx
// src/components/Badge/Badge.tsx
import { Chip } from './Badge.styles'

interface BadgeProps {
  label: string
}

export function Badge({ label }: BadgeProps) {
  return <Chip>{label}</Chip>
}
```

- 함수 선언(`function ComponentName()`) 형태로 작성한다. 화살표 함수 컴포넌트는 쓰지 않는다.
- 컴포넌트 하나당 파일 하나. 컴포넌트 전용 하위 컴포넌트가 필요하면 같은 폴더에 별도 파일로 분리한다.

## 상태 관리

- **서버 상태**는 TanStack Query로만 관리한다. `useEffect` + `fetch` 조합으로 서버 데이터를 가져오지 않는다.
- **클라이언트/로컬 상태**는 `useState`/`useReducer` + Context로 시작한다. 전역 상태 라이브러리(Zustand 등)는 실제로 필요해지기 전까지 도입하지 않는다.

## API 연동 (OpenAPI + TanStack Query)

백엔드 스펙은 `openapi/schema.json`에 스냅샷으로 저장하고, `pnpm api:fetch`로 갱신, `pnpm api:generate`로 `src/api/schema.gen.ts` 타입을 재생성한다. 통신은 `src/api/client.ts`의 `apiClient`(openapi-fetch)로만 하고 `fetch()`를 직접 부르지 않는다.

로컬 개발 시 `/api/*` 요청은 `vite.config.ts`의 dev 프록시가 `http://localhost:8080`(로컬 백엔드)으로 전달한다. 프로덕션은 Nginx가 같은 방식으로 라우팅해 프론트와 백엔드가 동일 오리진처럼 동작하므로, base URL을 환경별로 분기하지 않는다.

컴포넌트에서 `useQuery`/`useMutation`을 직접 호출하지 않는다. 도메인별로 `model.ts`/`api.ts`/`queries.ts`/`service.ts` 네 파일로 나눠 감싼다 (의존 방향: `service.ts → queries.ts → api.ts → model.ts`). 컴포넌트는 `service.ts`의 훅만 import한다.

파일 역할, query key factory 규칙, `queryOptions`/`mutationOptions` 사용법, 타입 네이밍, `ApiResponse` 언래핑은 `api-convention` 스킬(`.claude/skills/api-convention/SKILL.md`)을 따른다.

## MSW 목업

백엔드가 아직 없는 엔드포인트도 화면 개발이 가능하도록, `.env.local`에 `VITE_API_MOCKING=enabled`를 넣으면 `pnpm dev`가 실제 백엔드 대신 MSW(`src/mocks/`)로 응답한다 (`.env.local.example` 참고). 기본값은 꺼짐이며, 프로덕션 빌드에는 이 값이 없어 MSW 관련 코드가 번들에서 완전히 트리쉐이킹된다.

- 핸들러는 `openapi-msw`의 `createOpenApiHttp<paths>()`로 만들어 실제 OpenAPI 스펙과 경로·파라미터가 어긋나면 타입 에러가 난다. 단, 이 백엔드 스펙은 응답 content-type을 `*/*`로 내보내서 `openapi-msw`의 타입화된 `.json()` 헬퍼가 쓸모없어진다 — 응답 바디는 `msw`의 `HttpResponse.json(...)`을 직접 쓴다.
- 목데이터는 `src/mocks/data/*.csv`(기사 36명, 화물 500건, 구간별 시세 24건 — [dokdok/docs/mock-data.md](https://github.com/dokdok3/dokdok/blob/main/docs/mock-data.md)와 동일한 시드)를 `src/mocks/data/dataset.ts`가 파싱한다. `freights.csv`는 `pnpm mocks:fetch`로 원본을 재수집할 수 있고, `drivers.csv`/`fares.csv`는 문서에 인라인으로만 있어 수동으로 옮겨야 한다.
- `src/mocks/data/transform.ts`가 CSV 행을 실제 API 응답 DTO(`DriverResponse`, `LoadResponse`, `CargoDetailResponse`, `FareQuoteResponse`)로 변환한다. 매칭 점수·운임 판정(`LOW`/`FAIR`/`UNKNOWN`)은 mock-data.md의 "필수 데모 시나리오" 표와 일치하도록 만든 근사 로직이다 — 실제 백엔드 알고리즘이 아니다.
- 화물 생성·수정·수락은 `src/mocks/data/store.ts`의 메모리 Map에 반영돼 같은 세션 안에서는 상태가 유지된다. 새로고침하면 초기화된다.

## 스타일링 (Emotion)

- `styled()` API를 기본으로 쓴다. `css` prop은 쓰지 않는다.
- 스타일 코드는 별도 파일로 분리하지 않고 컴포넌트 파일 안에 함께 둔다.
- 색상·spacing·폰트 같은 디자인 토큰은 하드코딩하지 않고 `src/theme/theme.ts`에 정의한 Emotion 테마를 `props.theme`로 참조한다. 새 토큰이 필요하면 컴포넌트 파일이 아니라 `theme.ts`에 추가한다.

## 테스트

- 이 프로젝트는 **단위 테스트만** 작성한다: Vitest + Testing Library. 파일은 대상 컴포넌트/모듈과 같은 폴더에 `*.test.ts(x)`로 둔다. `pnpm test`로 실행한다.
- Playwright e2e 설정(`playwright.config.ts`, `e2e/`, `pnpm test:e2e`)은 초기 스캐폴딩으로 남아있지만 이 프로젝트의 테스트 컨벤션은 아니다 — 새 e2e 테스트를 추가하지 않는다.
- Vitest의 `test.include`(`vite.config.ts`)는 `src/**/*.{test,spec}.{ts,tsx}`로 제한되어 있어 `e2e/` 아래 Playwright 스펙과 겹치지 않는다.

## 포맷팅 / 린트

- 포맷팅은 Prettier가 전담한다 (`pnpm format`으로 적용, `pnpm format:check`로 검사만). 세미콜론 없음, single quote, trailing comma.
- ESLint는 코드 품질/버그 규칙만 담당한다 (`eslint-config-prettier`로 스타일 규칙은 이미 꺼져 있음). `pnpm lint`로 검사한다.
- 커밋 전 `pnpm lint && pnpm format:check && pnpm test`를 통과시킨다.
