import { apiClient } from '@/api/client'
import type { GetRegionsResponse } from './model'

export const getRegions = async (): Promise<GetRegionsResponse> => {
  const result = await apiClient.GET('/api/v1/regions')
  if (!result.data) {
    throw new Error(`지역 목록을 불러오지 못했습니다 (status: ${result.response.status})`)
  }

  return result.data.data ?? []
}
