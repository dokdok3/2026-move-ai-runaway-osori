import type { components } from '@/api/schema.gen'

export type CreateCargoParams = {
  shipperId: number
}
export type CreateCargoRequest = components['schemas']['CargoCreateRequest']
export type CreateCargoResponse = Record<string, unknown>

export type CargoDetailResponse = components['schemas']['CargoDetailResponse']
export type GetCargoDetailResponse = CargoDetailResponse

export type GetMyCargosParams = {
  shipperId: number
}
export type ShipperCargoResponse = components['schemas']['ShipperCargoResponse']
export type GetMyCargosResponse = ShipperCargoResponse[]

export type UpdateCargoParams = {
  cargoId: number
  shipperId: number
}
export type UpdateCargoRequest = components['schemas']['CargoUpdateRequest']
export type UpdateCargoResponse = CargoDetailResponse

export type ParseCargoRequest = components['schemas']['ParseRequest']
export type ParseCargoResponse = components['schemas']['ParsedCargoResponse']
