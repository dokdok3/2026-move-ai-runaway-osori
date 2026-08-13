import type { components } from '@/api/schema.gen'

export type LoadFilter = 'ALL' | 'ACCEPTED' | 'HIDDEN'

export type GetLoadListParams = {
  driverId: number
  preferenceText?: string
  refresh?: boolean
  filter?: LoadFilter
  cursor?: string
}
export type LoadResponse = components['schemas']['LoadResponse']
export type GetLoadListResponse = components['schemas']['CursorPageResponseLoadResponse']

export type AcceptLoadParams = {
  cargoId: number
  driverId: number
}
export type AcceptResponse = components['schemas']['AcceptResponse']
export type AcceptLoadResponse = AcceptResponse

export type HideLoadParams = {
  cargoId: number
  driverId: number
}

export type CompleteLoadParams = {
  cargoId: number
  driverId: number
}
export type CompleteLoadResponse = components['schemas']['CompleteResponse']
