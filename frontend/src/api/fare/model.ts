import type { components } from '@/api/schema.gen'

export type CargoType = components['schemas']['CargoCreateRequest']['cargoType']

export type GetFareQuoteParams = {
  originSido: string
  destSido: string
  cargoType: CargoType
  weightTon: number
  desiredFare?: number
}

export type FareQuoteResponse = components['schemas']['FareQuoteResponse']
export type GetFareQuoteResponse = FareQuoteResponse
