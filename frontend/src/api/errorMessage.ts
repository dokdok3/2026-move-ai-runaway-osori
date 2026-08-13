/**
 * openapi-fetch는 4xx/5xx 응답 바디를 result.error에 파싱해 담아주지만, 이 백엔드의
 * OpenAPI 스펙이 에러 응답을 문서화하지 않아 타입은 항상 never다. 백엔드가 실제로
 * `{ success, data, message }` 형태로 원인을 내려주므로, 런타임에 타입 가드로 좁혀 꺼낸다.
 */
export function extractApiErrorMessage(error: unknown): string | undefined {
  if (typeof error !== 'object' || error === null || !('message' in error)) {
    return undefined
  }

  const message = (error as { message?: unknown }).message
  return typeof message === 'string' && message.length > 0 ? message : undefined
}
