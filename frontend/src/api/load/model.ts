import type { components } from '@/api/schema.gen'

export type GetLoadListParams = {
  driverId: number
}
export type LoadResponse = components['schemas']['LoadResponse']
export type GetLoadListResponse = LoadResponse[]

export type AcceptLoadParams = {
  cargoId: number
  driverId: number
}
export type AcceptResponse = components['schemas']['AcceptResponse']
export type AcceptLoadResponse = AcceptResponse
