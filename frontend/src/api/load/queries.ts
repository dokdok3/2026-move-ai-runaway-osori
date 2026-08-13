import { mutationOptions, queryOptions } from '@tanstack/react-query'
import { acceptLoad, getLoadList, hideLoad } from './api'
import type { AcceptLoadParams, GetLoadListParams, HideLoadParams } from './model'

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
    ] as const,
}

export const loadQueries = {
  list: (params: GetLoadListParams) =>
    queryOptions({
      queryKey: loadQueryKeys.list(params),
      queryFn: () => getLoadList(params),
    }),
}

export const loadMutations = {
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
}
