import { useQuery, useQueryClient, useSuspenseQuery } from '@tanstack/react-query'
import { fareQueries } from './queries'
import type { GetFareQuoteParams } from './model'

export const useFareQuoteQuery = (params: GetFareQuoteParams) => useQuery(fareQueries.quote(params))

export const useSuspenseFareQuoteQuery = (params: GetFareQuoteParams) =>
  useSuspenseQuery(fareQueries.quote(params))

/** 파싱 결과에 희망 운임이 없을 때, 평균 시세를 즉석에서 한 번만 조회해 기본값으로 쓰기 위한 명령형 fetcher. */
export const useFareQuoteFetcher = () => {
  const queryClient = useQueryClient()
  return (params: GetFareQuoteParams) => queryClient.fetchQuery(fareQueries.quote(params))
}
