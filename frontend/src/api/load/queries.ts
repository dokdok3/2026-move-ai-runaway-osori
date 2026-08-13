import { infiniteQueryOptions, mutationOptions } from '@tanstack/react-query'
import { acceptLoad, cancelLoad, completeLoad, getLoadList, hideLoad } from './api'
import type {
  AcceptLoadParams,
  CompleteLoadParams,
  CancelLoadParams,
  GetLoadListParams,
  HideLoadParams,
} from './model'

export const loadQueryKeys = {
  all: ['load'] as const,
  list: (params: GetLoadListParams) =>
    [
      ...loadQueryKeys.all,
      'list',
      params.driverId,
      params.preferenceText ?? '',
      params.refresh ?? false,
      params.filter ?? 'ALL',
      params.cursor ?? '',
    ] as const,
}

export const loadQueries = {
  list: (params: GetLoadListParams) =>
    infiniteQueryOptions({
      queryKey: loadQueryKeys.list(params),
      queryFn: ({ pageParam }) => getLoadList({ ...params, cursor: pageParam }),
      initialPageParam: undefined as string | undefined,
      getNextPageParam: (lastPage) => (lastPage.hasNext ? lastPage.nextCursor : undefined),
    }),
}

export const loadMutations = {
  cancel: () =>
    mutationOptions({
      mutationKey: [...loadQueryKeys.all, 'cancel'],
      mutationFn: (params: CancelLoadParams) => cancelLoad(params),
    }),
  accept: () =>
    mutationOptions({
      mutationKey: [...loadQueryKeys.all, 'accept'],
      mutationFn: (params: AcceptLoadParams) => acceptLoad(params),
    }),
  hide: () =>
    mutationOptions({
      mutationKey: [...loadQueryKeys.all, 'hide'],
      mutationFn: (params: HideLoadParams) => hideLoad(params),
    }),
  complete: () =>
    mutationOptions({
      mutationKey: [...loadQueryKeys.all, 'complete'],
      mutationFn: (params: CompleteLoadParams) => completeLoad(params),
    }),
}
