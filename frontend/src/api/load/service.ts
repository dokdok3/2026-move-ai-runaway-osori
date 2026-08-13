import { useInfiniteQuery, useMutation, useSuspenseInfiniteQuery } from '@tanstack/react-query'
import { loadMutations, loadQueries } from './queries'
import type { GetLoadListParams } from './model'

export const useLoadListQuery = (params: GetLoadListParams) =>
  useInfiniteQuery(loadQueries.list(params))

export const useSuspenseLoadListQuery = (params: GetLoadListParams) =>
  useSuspenseInfiniteQuery(loadQueries.list(params))

export const useAcceptLoadMutation = () => useMutation(loadMutations.accept())

export const useHideLoadMutation = () => useMutation(loadMutations.hide())
