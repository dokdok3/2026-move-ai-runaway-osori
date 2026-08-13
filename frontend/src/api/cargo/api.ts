import { apiClient } from '@/api/client'
import { extractApiErrorMessage } from '@/api/errorMessage'
import type {
  CreateCargoParams,
  CreateCargoRequest,
  CreateCargoResponse,
  GetCargoDetailResponse,
  GetMyCargosParams,
  GetMyCargosResponse,
  ParseCargoRequest,
  ParseCargoResponse,
  UpdateCargoRequest,
  UpdateCargoParams,
  UpdateCargoResponse,
} from './model'

export const getMyCargos = async (params: GetMyCargosParams): Promise<GetMyCargosResponse> => {
  const result = await apiClient.GET('/api/v1/cargos/me', { params: { query: params } })
  if (!result.data) {
    throw new Error(
      extractApiErrorMessage(result.error) ??
        `내 화물 목록을 불러오지 못했습니다 (status: ${result.response.status})`,
    )
  }

  return result.data.data ?? []
}

export const createCargo = async (
  params: CreateCargoParams,
  body: CreateCargoRequest,
): Promise<CreateCargoResponse> => {
  const result = await apiClient.POST('/api/v1/cargos', {
    params: { query: params },
    body,
  })
  if (!result.data) {
    throw new Error(
      extractApiErrorMessage(result.error) ??
        `화물 등록에 실패했습니다 (status: ${result.response.status})`,
    )
  }

  return result.data.data ?? {}
}

export const getCargoDetail = async (cargoId: number): Promise<GetCargoDetailResponse> => {
  const result = await apiClient.GET('/api/v1/cargos/{cargoId}', {
    params: { path: { cargoId } },
  })
  if (!result.data) {
    throw new Error(
      extractApiErrorMessage(result.error) ??
        `화물 정보를 불러오지 못했습니다 (status: ${result.response.status})`,
    )
  }

  return result.data.data ?? {}
}

export const updateCargo = async (
  params: UpdateCargoParams,
  body: UpdateCargoRequest,
): Promise<UpdateCargoResponse> => {
  const { cargoId, shipperId } = params
  const result = await apiClient.PATCH('/api/v1/cargos/{cargoId}', {
    params: { path: { cargoId }, query: { shipperId } },
    body,
  })
  if (!result.data) {
    throw new Error(
      extractApiErrorMessage(result.error) ??
        `화물 수정에 실패했습니다 (status: ${result.response.status})`,
    )
  }

  return result.data.data ?? {}
}

export const parseCargo = async (body: ParseCargoRequest): Promise<ParseCargoResponse> => {
  const result = await apiClient.POST('/api/v1/cargos/parse', { body })
  if (!result.data) {
    throw new Error(
      extractApiErrorMessage(result.error) ??
        `화물 문장을 파싱하지 못했습니다 (status: ${result.response.status})`,
    )
  }

  return result.data.data ?? {}
}
