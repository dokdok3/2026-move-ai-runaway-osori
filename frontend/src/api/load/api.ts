import { apiClient } from '@/api/client'
import type {
  AcceptLoadParams,
  AcceptLoadResponse,
  GetLoadListParams,
  GetLoadListResponse,
} from './model'

export const getLoadList = async (params: GetLoadListParams): Promise<GetLoadListResponse> => {
  const result = await apiClient.GET('/api/v1/loads', { params: { query: params } })
  if (!result.data) {
    throw new Error(`추천 화물을 불러오지 못했습니다 (status: ${result.response.status})`)
  }

  return result.data.data ?? []
}

export const acceptLoad = async (params: AcceptLoadParams): Promise<AcceptLoadResponse> => {
  const { cargoId, driverId } = params
  const result = await apiClient.POST('/api/v1/loads/{cargoId}/accept', {
    params: { path: { cargoId }, query: { driverId } },
  })
  if (!result.data) {
    throw new Error(`화물을 수락하지 못했습니다 (status: ${result.response.status})`)
  }

  return result.data.data ?? {}
}
