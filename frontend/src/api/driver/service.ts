import { useMutation, useQuery, useSuspenseQuery } from '@tanstack/react-query'
import { driverMutations, driverQueries } from './queries'
import type { GetDriverProfileParams } from './model'

export const useDriverProfileQuery = (params: GetDriverProfileParams) =>
  useQuery(driverQueries.profile(params))

export const useSuspenseDriverProfileQuery = (params: GetDriverProfileParams) =>
  useSuspenseQuery(driverQueries.profile(params))

export const useUpdateRoutePreferencesMutation = () =>
  useMutation(driverMutations.updateRoutePreferences())

export const useUpdateDriverProfileMutation = () => useMutation(driverMutations.updateProfile())
