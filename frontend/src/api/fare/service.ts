import { useQuery, useSuspenseQuery } from '@tanstack/react-query'
import { fareQueries } from './queries'
import type { GetFareQuoteParams } from './model'

export const useFareQuoteQuery = (params: GetFareQuoteParams) => useQuery(fareQueries.quote(params))

export const useSuspenseFareQuoteQuery = (params: GetFareQuoteParams) =>
  useSuspenseQuery(fareQueries.quote(params))
