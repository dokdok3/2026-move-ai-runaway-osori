import { useMutation, useQuery, useSuspenseQuery } from '@tanstack/react-query'
import { cargoMutations, cargoQueries } from './queries'

export const useCargoDetailQuery = (cargoId: number) => useQuery(cargoQueries.detail(cargoId))

export const useSuspenseCargoDetailQuery = (cargoId: number) =>
  useSuspenseQuery(cargoQueries.detail(cargoId))

export const useCargoCreateMutation = () => useMutation(cargoMutations.create())

export const useCargoUpdateMutation = () => useMutation(cargoMutations.update())

export const useCargoParseMutation = () => useMutation(cargoMutations.parse())
