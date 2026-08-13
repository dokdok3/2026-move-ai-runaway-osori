import { apiClient } from '@/api/client'
import type {
  GetShipperProfileResponse,
  UpdateShipperProfileRequest,
  UpdateShipperProfileResponse,
} from './model'

export const getShipperProfile = async (): Promise<GetShipperProfileResponse> => {
  const result = await apiClient.GET('/api/v1/shippers/me')
  if (!result.data) {
    throw new Error(`화주 정보를 불러오지 못했습니다 (status: ${result.response.status})`)
  }

  return result.data.data ?? {}
}

export const updateShipperProfile = async (
  body: UpdateShipperProfileRequest,
): Promise<UpdateShipperProfileResponse> => {
  const result = await apiClient.PUT('/api/v1/shippers/me', { body })
  if (!result.data) {
    throw new Error(`화주 정보를 저장하지 못했습니다 (status: ${result.response.status})`)
  }

  return result.data.data ?? {}
}
