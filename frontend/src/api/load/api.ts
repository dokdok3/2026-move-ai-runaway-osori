import { apiClient } from '@/api/client'
import { extractApiErrorMessage } from '@/api/errorMessage'
import type {
  AcceptLoadParams,
  AcceptLoadResponse,
  GetLoadListParams,
  GetLoadListResponse,
  HideLoadParams,
  CompleteLoadParams,
  CompleteLoadResponse,
  CancelLoadParams,
  CancelLoadResponse,
} from './model'

export const cancelLoad = async (params: CancelLoadParams): Promise<CancelLoadResponse> => {
  const query = new URLSearchParams({ driverId: String(params.driverId) })
  const response = await fetch(`/api/v1/loads/${params.cargoId}/cancel?${query}`, {
    method: 'POST',
    headers: { 'X-User-Id': String(params.driverId) },
  })
  const result = (await response.json()) as {
    data?: CancelLoadResponse
    message?: string
  }

  if (!response.ok) {
    throw new Error(result.message ?? `수락을 취소하지 못했습니다 (status: ${response.status})`)
  }

  return result.data ?? {}
}

export const completeLoad = async (params: CompleteLoadParams): Promise<CompleteLoadResponse> => {
  const { cargoId, driverId } = params
  const result = await apiClient.POST('/api/v1/loads/{cargoId}/complete', {
    params: { path: { cargoId }, query: { driverId } },
  })
  if (!result.data) {
    throw new Error(
      extractApiErrorMessage(result.error) ??
        `하차 완료 처리에 실패했습니다 (status: ${result.response.status})`,
    )
  }

  return result.data.data ?? {}
}

export const getLoadList = async (params: GetLoadListParams): Promise<GetLoadListResponse> => {
  const result = await apiClient.GET('/api/v1/loads', { params: { query: params } })
  if (!result.data) {
    throw new Error(
      extractApiErrorMessage(result.error) ??
        `추천 화물을 불러오지 못했습니다 (status: ${result.response.status})`,
    )
  }

  const data = result.data.data
  if (Array.isArray(data)) {
    return { content: data, hasNext: false, size: data.length }
  }

  return data ?? { content: [], hasNext: false, size: 0 }
}

export const hideLoad = async (params: HideLoadParams): Promise<void> => {
  const { cargoId, driverId } = params
  const result = await apiClient.POST('/api/v1/loads/{cargoId}/hide', {
    params: { path: { cargoId }, query: { driverId } },
  })
  if (!result.data) {
    throw new Error(
      extractApiErrorMessage(result.error) ??
        `화물을 숨기지 못했습니다 (status: ${result.response.status})`,
    )
  }
}

export const acceptLoad = async (params: AcceptLoadParams): Promise<AcceptLoadResponse> => {
  const { cargoId, driverId } = params
  const result = await apiClient.POST('/api/v1/loads/{cargoId}/accept', {
    params: { path: { cargoId }, query: { driverId } },
  })
  if (!result.data) {
    throw new Error(
      extractApiErrorMessage(result.error) ??
        `화물을 수락하지 못했습니다 (status: ${result.response.status})`,
    )
  }

  return result.data.data ?? {}
}
