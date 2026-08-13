import { useMutation, useQuery, useSuspenseQuery } from '@tanstack/react-query'
import { loadMutations, loadQueries } from './queries'
import type { GetLoadListParams } from './model'

export const useLoadListQuery = (params: GetLoadListParams) => useQuery(loadQueries.list(params))

export const useSuspenseLoadListQuery = (params: GetLoadListParams) =>
  useSuspenseQuery(loadQueries.list(params))

export const useAcceptLoadMutation = () => useMutation(loadMutations.accept())
