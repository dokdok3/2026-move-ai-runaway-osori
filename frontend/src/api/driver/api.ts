import { apiClient } from '@/api/client'
import { extractApiErrorMessage } from '@/api/errorMessage'
import type {
  GetDriverProfileParams,
  GetDriverProfileResponse,
  UpdateRoutePreferencesParams,
  UpdateRoutePreferencesRequest,
  UpdateRoutePreferencesResponse,
  UpdateDriverProfileParams,
  UpdateDriverProfileRequest,
  UpdateDriverProfileResponse,
} from './model'

export const updateDriverProfile = async (
  params: UpdateDriverProfileParams,
  body: UpdateDriverProfileRequest,
): Promise<UpdateDriverProfileResponse> => {
  const result = await apiClient.PUT('/api/v1/drivers/me', {
    params: { query: params },
    body,
  })
  if (!result.data) {
    throw new Error(
      extractApiErrorMessage(result.error) ??
        `기사 정보를 수정하지 못했습니다 (status: ${result.response.status})`,
    )
  }

  return result.data.data ?? {}
}

export const getDriverProfile = async (
  params: GetDriverProfileParams,
): Promise<GetDriverProfileResponse> => {
  const result = await apiClient.GET('/api/v1/drivers/me', { params: { query: params } })
  if (!result.data) {
    throw new Error(
      extractApiErrorMessage(result.error) ??
        `기사 프로필을 불러오지 못했습니다 (status: ${result.response.status})`,
    )
  }

  return result.data.data ?? {}
}

export const updateRoutePreferences = async (
  params: UpdateRoutePreferencesParams,
  body: UpdateRoutePreferencesRequest,
): Promise<UpdateRoutePreferencesResponse> => {
  const result = await apiClient.PUT('/api/v1/drivers/me/route-preferences', {
    params: { query: params },
    body,
  })
  if (!result.data) {
    throw new Error(
      extractApiErrorMessage(result.error) ??
        `다니는 구간을 저장하지 못했습니다 (status: ${result.response.status})`,
    )
  }

  return result.data.data ?? {}
}
