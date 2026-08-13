import { mutationOptions, queryOptions } from '@tanstack/react-query'
import { getShipperProfile, updateShipperProfile } from './api'
import type { UpdateShipperProfileRequest } from './model'

export const shipperQueryKeys = {
  all: ['shipper'] as const,
  profile: () => [...shipperQueryKeys.all, 'profile'] as const,
}

export const shipperQueries = {
  profile: () =>
    queryOptions({
      queryKey: shipperQueryKeys.profile(),
      queryFn: getShipperProfile,
    }),
}

export const shipperMutations = {
  updateProfile: () =>
    mutationOptions({
      mutationKey: [...shipperQueryKeys.all, 'updateProfile'],
      mutationFn: (body: UpdateShipperProfileRequest) => updateShipperProfile(body),
    }),
}
