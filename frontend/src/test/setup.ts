import '@testing-library/jest-dom/vitest'
import { afterAll, afterEach } from 'vitest'
import { server } from '@/mocks/server'

/**
 * apiClient는 브라우저 동일 오리진 프록시를 전제로 baseUrl '/'을 쓰고, openapi-fetch는 내부에서
 * `new Request('/api/...', init)`을 직접 호출한다. 브라우저의 Request는 현재 문서 위치를 기준으로
 * 상대 URL을 해석하지만, Node(undici)의 Request는 그런 기준점이 없어 상대 URL을 거부한다.
 * 테스트에서만 절대 URL로 보정해 실제 앱 코드는 그대로 두고 이 간극을 메운다.
 */
const NodeRequest = globalThis.Request
globalThis.Request = class extends NodeRequest {
  constructor(input: RequestInfo | URL, init?: RequestInit) {
    if (typeof input === 'string' && input.startsWith('/')) {
      super(`http://localhost:3000${input}`, init)
    } else {
      super(input, init)
    }
  }
} as typeof Request

/**
 * apiClient(src/api/client.ts)는 모듈 로드 시점에 createClient()를 호출하면서
 * 그 순간의 globalThis.fetch를 캡처해 고정한다. beforeAll 안에서 listen()을 하면
 * 이미 테스트 파일과 apiClient가 다 로드된 뒤라 패치 전 fetch가 그대로 굳어버리므로,
 * setupFiles가 테스트 파일을 import하기 전인 최상위 스코프에서 바로 실행해야 한다.
 */
server.listen({ onUnhandledRequest: 'error' })
afterEach(() => server.resetHandlers())
afterAll(() => server.close())
