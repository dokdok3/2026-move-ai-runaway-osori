import { queryOptions } from '@tanstack/react-query'
import { getFareQuote } from './api'
import type { GetFareQuoteParams } from './model'

export const fareQueryKeys = {
  all: ['fare'] as const,
  quotes: () => [...fareQueryKeys.all, 'quote'] as const,
  quote: (params: GetFareQuoteParams) => [...fareQueryKeys.quotes(), params] as const,
}

export const fareQueries = {
  quote: (params: GetFareQuoteParams) =>
    queryOptions({
      queryKey: fareQueryKeys.quote(params),
      queryFn: () => getFareQuote(params),
    }),
}
