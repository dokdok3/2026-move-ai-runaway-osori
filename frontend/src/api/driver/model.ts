import type { components } from '@/api/schema.gen'

export type GetDriverProfileParams = {
  driverId: number
}
export type DriverResponse = components['schemas']['DriverResponse']
export type GetDriverProfileResponse = DriverResponse

export type UpdateRoutePreferencesParams = {
  driverId: number
}
export type UpdateRoutePreferencesRequest = components['schemas']['RoutePreferenceRequest']
export type UpdateRoutePreferencesResponse = DriverResponse
