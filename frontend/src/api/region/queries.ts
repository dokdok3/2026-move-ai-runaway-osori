import { queryOptions } from '@tanstack/react-query'
import { getRegions } from './api'

export const regionQueryKeys = {
  all: ['region'] as const,
  list: () => [...regionQueryKeys.all, 'list'] as const,
}

export const regionQueries = {
  list: () =>
    queryOptions({
      queryKey: regionQueryKeys.list(),
      queryFn: getRegions,
    }),
}
