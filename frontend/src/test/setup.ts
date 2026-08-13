import '@testing-library/jest-dom/vitest'
import { afterAll, afterEach, beforeAll } from 'vitest'
import { server } from '@/mocks/server'

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
afterEach(() => server.resetHandlers())
afterAll(() => server.close())

/**
 * apiClient는 브라우저 동일 오리진 프록시를 전제로 baseUrl '/'을 쓴다.
 * Node의 fetch(undici)는 브라우저와 달리 상대 URL을 resolve하지 못하므로, MSW가 fetch를
 * 패치한 뒤(server.listen 이후) 한 번 더 감싸서 절대 URL로 보정해야 MSW 매칭 이전에 터지지 않는다.
 */
beforeAll(() => {
  const patchedFetch = globalThis.fetch
  globalThis.fetch = ((input: RequestInfo | URL, init?: RequestInit) => {
    if (typeof input === 'string' && input.startsWith('/')) {
      return patchedFetch(`http://localhost${input}`, init)
    }
    return patchedFetch(input, init)
  }) as typeof fetch
})
