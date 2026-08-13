import { useMutation, useQuery } from '@tanstack/react-query'
import { shipperMutations, shipperQueries } from './queries'

export const useShipperProfileQuery = () => useQuery(shipperQueries.profile())

export const useUpdateShipperProfileMutation = () => useMutation(shipperMutations.updateProfile())
