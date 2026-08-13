import { mutationOptions, queryOptions } from '@tanstack/react-query'
import { getDriverProfile, updateRoutePreferences } from './api'
import type {
  GetDriverProfileParams,
  UpdateRoutePreferencesParams,
  UpdateRoutePreferencesRequest,
} from './model'

export const driverQueryKeys = {
  all: ['driver'] as const,
  profile: (params: GetDriverProfileParams) => [...driverQueryKeys.all, 'profile', params] as const,
}

export const driverQueries = {
  profile: (params: GetDriverProfileParams) =>
    queryOptions({
      queryKey: driverQueryKeys.profile(params),
      queryFn: () => getDriverProfile(params),
    }),
}

export const driverMutations = {
  updateRoutePreferences: () =>
    mutationOptions({
      mutationKey: [...driverQueryKeys.all, 'updateRoutePreferences'],
      mutationFn: ({
        params,
        body,
      }: {
        params: UpdateRoutePreferencesParams
        body: UpdateRoutePreferencesRequest
      }) => updateRoutePreferences(params, body),
    }),
}
