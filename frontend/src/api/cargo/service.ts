import { useMutation, useQuery, useSuspenseQuery } from '@tanstack/react-query'
import { cargoMutations, cargoQueries } from './queries'
import type { GetMyCargosParams } from './model'

export const useMyCargosQuery = (params: GetMyCargosParams) =>
  useQuery(cargoQueries.myCargos(params))

/** cargoId가 아직 없을 때(예: 화물 생성 전)는 조회를 미루기 위해 undefined를 허용한다. */
export const useCargoDetailQuery = (cargoId: number | undefined) =>
  useQuery({ ...cargoQueries.detail(cargoId ?? 0), enabled: cargoId !== undefined })

export const useSuspenseCargoDetailQuery = (cargoId: number) =>
  useSuspenseQuery(cargoQueries.detail(cargoId))

export const useCargoCreateMutation = () => useMutation(cargoMutations.create())

export const useCargoUpdateMutation = () => useMutation(cargoMutations.update())

export const useCargoParseMutation = () => useMutation(cargoMutations.parse())
