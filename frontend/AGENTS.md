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
│   └── user/
│       ├── model.ts    (파라미터/응답 타입)
│       ├── api.ts      (fetch 함수)
│       ├── queries.ts  (query key factory + queryOptions/mutationOptions)
│       └── service.ts  (컴포넌트가 쓰는 훅)
├── theme/
│   └── theme.ts              (Emotion ThemeProvider용 디자인 토큰)
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

## TanStack Query — 도메인별 API 계층

컴포넌트에서 `useQuery`/`useMutation`을 직접 호출하지 않고, `fetch`도 직접 부르지 않는다. 도메인별로 `model.ts`/`api.ts`/`queries.ts`/`service.ts` 네 파일로 나눠 감싼다 (의존 방향: `service.ts → queries.ts → api.ts → model.ts`). 컴포넌트는 `service.ts`의 훅만 import한다.

파일 역할, query key factory 규칙, `queryOptions`/`mutationOptions` 사용법, 타입 네이밍은 `api-convention` 스킬(`.claude/skills/api-convention/SKILL.md`)을 따른다.

## 스타일링 (Emotion)

- `styled()` API를 기본으로 쓴다. `css` prop은 쓰지 않는다.
- 스타일 코드는 별도 파일로 분리하지 않고 컴포넌트 파일 안에 함께 둔다.
- 색상·spacing·폰트 같은 디자인 토큰은 하드코딩하지 않고 `src/theme/theme.ts`에 정의한 Emotion 테마를 `props.theme`로 참조한다. 새 토큰이 필요하면 컴포넌트 파일이 아니라 `theme.ts`에 추가한다.

## 테스트

- **단위 테스트**: Vitest + Testing Library. 파일은 대상 컴포넌트/모듈과 같은 폴더에 `*.test.ts(x)`로 둔다. `pnpm test`로 실행한다.
- **e2e 테스트**: Playwright, `e2e/*.spec.ts`. `pnpm test:e2e`로 실행한다.
- 두 테스트 러너의 대상 경로가 겹치지 않게 한다 (`vite.config.ts`의 `test.include`가 `src/**/*.{test,spec}.{ts,tsx}`로 제한되어 있음).

## 포맷팅 / 린트

- 포맷팅은 Prettier가 전담한다 (`pnpm format`으로 적용, `pnpm format:check`로 검사만). 세미콜론 없음, single quote, trailing comma.
- ESLint는 코드 품질/버그 규칙만 담당한다 (`eslint-config-prettier`로 스타일 규칙은 이미 꺼져 있음). `pnpm lint`로 검사한다.
- 커밋 전 `pnpm lint && pnpm format:check && pnpm test`를 통과시킨다.
