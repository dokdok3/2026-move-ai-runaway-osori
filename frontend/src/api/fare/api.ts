import { apiClient } from '@/api/client'
import type { GetFareQuoteParams, GetFareQuoteResponse } from './model'

export const getFareQuote = async (params: GetFareQuoteParams): Promise<GetFareQuoteResponse> => {
  const result = await apiClient.GET('/api/v1/fares/quote', {
    params: { query: params },
  })
  if (!result.data) {
    throw new Error(`운임 시세를 불러오지 못했습니다 (status: ${result.response.status})`)
  }

  return result.data.data ?? {}
}
