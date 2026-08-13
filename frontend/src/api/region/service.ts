import { useQuery, useSuspenseQuery } from '@tanstack/react-query'
import { regionQueries } from './queries'

export const useRegionListQuery = () => useQuery(regionQueries.list())

export const useSuspenseRegionListQuery = () => useSuspenseQuery(regionQueries.list())
