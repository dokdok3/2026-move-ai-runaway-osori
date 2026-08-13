---
name: api-convention
description: 도메인별 API 호출 계층(api / queries / service) 작성 컨벤션. fetch 함수, TanStack Query queryOptions·mutationOptions, 소비 훅을 어떤 파일에 어떤 형태로 둘지 정한다.
---

# API 호출 컨벤션

도메인별로 API 관련 코드를 네 파일로 나눈다. 타입, 통신, 쿼리 설정, 소비 훅을 각각 분리해 책임을 명확히 한다.

```
src/api/<도메인>/
  model.ts     # 파라미터 / 응답 타입
  api.ts       # fetch 함수 (통신 구현)
  queries.ts   # queryKey 팩토리 + queryOptions / mutationOptions
  service.ts   # 컴포넌트가 쓰는 훅 (useQuery / useMutation 래핑)
```

의존 방향은 `service.ts → queries.ts → api.ts → model.ts` 한 방향이다. 컴포넌트는 `service.ts`의 훅만 import 하고, `api.ts`의 fetch 함수를 직접 부르지 않는다(DIP).

## OpenAPI 코드젠

백엔드 스펙(`openapi/schema.json`, `pnpm api:fetch`로 갱신)에서 `pnpm api:generate`로 `src/api/schema.gen.ts`(`paths`/`components` 타입)를 생성한다. 이 파일은 자동 생성물이라 직접 수정하지 않는다. 통신은 `src/api/client.ts`의 `apiClient`(openapi-fetch, `@/api/schema.gen`의 `paths`로 타입화됨)로만 한다 — 도메인 `api.ts`에서 `fetch()`를 직접 부르지 않는다.

## `model.ts` — 파라미터 / 응답 타입

파라미터·응답 타입은 도메인의 `model.ts` 한 곳에 정의하고 나머지 세 파일이 모두 이를 참조한다. 직접 선언하지 않고 `schema.gen.ts`의 `components['schemas']`에서 가져와 별칭을 붙인다 — 백엔드 스펙과 어긋나지 않게 하기 위함이다.

네이밍 규칙:

- **Get 호출**이면 prefix `Get`: `GetProductListParams`, `GetProductListResponse`
- **Query Params**로 보내면 suffix `Params`
- **Body**로 보내면 suffix `Request`
- **응답값**이면 suffix `Response`

백엔드는 모든 응답을 `ApiResponse<T>`(`{ success, data, message }`)로 감싼다. `*Response` 타입은 감싸지 않은 실제 데이터(`T`, 예: 배열·객체)를 가리키고, 껍질을 벗기는 일은 `api.ts`가 한다.

```ts
import type { components } from '@/api/schema.gen'

export type RegionResponse = components['schemas']['RegionResponse']
export type GetRegionsResponse = RegionResponse[]

export type GetProductListParams = components['schemas']['GetProductListParams']
```

## `api.ts` — 통신 함수

- 함수명은 `동사 + 목적어`: `getHome`, `getRegions`, `createProductItem`
- `async` 화살표 함수로 작성하고 named export 한다
- `apiClient.GET`/`POST`/... 호출 결과를 구조 분해하지 않고 변수로 받는다 (구조 분해하면 이후 narrowing이 끊긴다)
- 이 백엔드의 OpenAPI 스펙은 에러 응답(4xx/5xx)을 문서화하지 않아 `result.error`가 항상 `never`로 추론된다. 대신 **`result.data` 존재 여부**로 성공/실패를 판별한다
- 실패 시 사용자에게 보여줄 메시지와 `result.response.status`를 담아 `throw` 한다 (빈 catch·무시 금지)
- 성공 시 `result.data.data`로 `ApiResponse` 껍질을 벗겨 반환한다

```ts
import { apiClient } from '@/api/client'
import type { GetRegionsResponse } from './model'

export const getRegions = async (): Promise<GetRegionsResponse> => {
  const result = await apiClient.GET('/api/v1/regions')
  if (!result.data) {
    throw new Error(`지역 목록을 불러오지 못했습니다 (status: ${result.response.status})`)
  }

  return result.data.data ?? []
}
```

- 경로 파라미터가 있는 GET/PATCH 등은 `apiClient.GET('/api/v1/cargos/{cargoId}', { params: { path: { cargoId } } })`처럼 `params.path`/`params.query`로 넘긴다. 직접 URL 문자열을 조립하지 않는다.

## `queries.ts` — queryKey + query/mutation 옵션

### queryKey 팩토리

- `all`은 도메인 루트 키 **배열**이다 (함수 아님)
- 파생 키는 `[...도메인QueryKeys.all, '구분자', 인자]`로 확장한다
- `as const`로 튜플 타입을 좁힌다

```ts
export const productQueryKeys = {
  all: ['product'] as const,
  list: (query: GetProductListParams) => [...productQueryKeys.all, 'list', query] as const,
}
```

### query / mutation 옵션

- 조회는 `queryOptions`, 변경은 `mutationOptions`로 감싼다 (v5.80+)
- 목록처럼 파라미터가 바뀌며 재조회되는 쿼리는 `placeholderData: keepPreviousData`로 깜빡임을 막는다
- mutation 키 필드명은 `mutationKey`다

```ts
import { queryOptions, mutationOptions, keepPreviousData } from '@tanstack/react-query'

export const productQueries = {
  list: (query: GetProductListParams) =>
    queryOptions({
      queryKey: productQueryKeys.list(query),
      queryFn: () => getProductList(query),
      placeholderData: keepPreviousData,
    }),
}

export const productMutations = {
  create: () =>
    mutationOptions({
      mutationKey: [...productQueryKeys.all, 'create'],
      mutationFn: createProductItem,
    }),
}
```

## `service.ts` — 소비 훅

- 컴포넌트가 import 하는 유일한 계층
- 훅 이름은 `use + 행위 + Query / Mutation`: 조회는 `Query`, 변경은 `Mutation` suffix로 훅의 성격을 이름에 드러낸다 (`useHomeQuery`, `useProductListQuery`, `useProductCreateMutation`)
- `queries.ts`의 옵션을 `useQuery` / `useSuspenseQuery` / `useMutation`에 그대로 넘긴다
- 여기서 통신·쿼리 설정을 다시 만들지 않는다

같은 query 옵션을 `useQuery`로도 `useSuspenseQuery`로도 쓸 수 있다. Suspense 경계(`loading.tsx` / `<Suspense>`)에 맡길 화면은 `useSuspenseQuery`를, `data` undefined를 직접 다뤄야 하면 `useQuery`를 쓴다. Suspense 변형은 `use + Suspense + 행위 + Query`로 둔다.

```ts
import { useQuery, useSuspenseQuery, useMutation } from '@tanstack/react-query'

export const useProductListQuery = (params: GetProductListParams) =>
  useQuery(productQueries.list(params))

export const useSuspenseProductListQuery = (params: GetProductListParams) =>
  useSuspenseQuery(productQueries.list(params))

export const useProductCreateMutation = () => useMutation(productMutations.create())
```

## 체크리스트

- [ ] `api.ts`가 `fetch()`를 직접 부르지 않고 `apiClient`(openapi-fetch)를 쓰는가
- [ ] `apiClient` 호출 결과를 변수로 받고(구조 분해 X), `result.data` 존재 여부로 성공/실패를 판별하는가
- [ ] 실패 시 사용자 메시지 + `result.response.status`를 담아 throw 하는가
- [ ] 컴포넌트가 `service.ts` 훅만 쓰고 `api.ts`를 직접 부르지 않는가
- [ ] queryKey `all`을 배열로 두고 파생 키가 이를 확장하는가
- [ ] 파라미터·응답 타입이 `model.ts`에서 `schema.gen.ts`를 참조해 관리되는가
- [ ] 타입 네이밍이 규칙(Get / Params / Request / Response)을 따르는가
- [ ] `*Response` 타입이 `ApiResponse` 껍질을 벗긴 실제 데이터를 가리키는가
- [ ] mutation 필드명이 `mutationKey` / `mutationFn`인가
- [ ] `service.ts` 훅 이름이 `use + 행위 + Query / Mutation`을 따르는가
- [ ] `src/api/schema.gen.ts`를 직접 수정하지 않았는가 (스펙을 고치고 `pnpm api:generate`로 재생성)
