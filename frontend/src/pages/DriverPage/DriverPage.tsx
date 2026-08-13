import { useEffect, useRef, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import styled from '@emotion/styled'
import { Hero } from '@/components/Hero'
import { PageLayout } from '@/components/PageLayout'
import { useDriverProfileQuery, useUpdateRoutePreferencesMutation } from '@/api/driver/service'
import { useRegionListQuery } from '@/api/region/service'
import { useAcceptLoadMutation, useLoadListQuery } from '@/api/load/service'
import type { LoadFilter, LoadResponse } from '@/api/load/model'
import { RouteFilterCard } from './RouteFilterCard'
import { LoadOfferList } from './LoadOfferList'

/** 이 화면은 기사 로그인이 없는 데모라 기사 id를 고정값으로 둔다. */
const DEMO_DRIVER_ID = 1

interface RouteDraft {
  originSido: string
  originSigungu: string
  destSido: string
  destSigungu: string
}

const EMPTY_ROUTE: RouteDraft = {
  originSido: '',
  originSigungu: '',
  destSido: '',
  destSigungu: '',
}

const LOAD_FILTERS: LoadFilter[] = ['ALL', 'ACCEPTED', 'HIDDEN']

function toLoadFilter(value: string | null): LoadFilter {
  return LOAD_FILTERS.includes(value as LoadFilter) ? (value as LoadFilter) : 'ALL'
}

const ErrorText = styled.p`
  margin: 0 0 14px;
  color: #b3261e;
  font-size: 14px;
  line-height: 1.5;
`

export function DriverPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [route, setRoute] = useState<RouteDraft>(EMPTY_ROUTE)
  const [hiddenCargoIds, setHiddenCargoIds] = useState<Set<number>>(new Set())
  const [acceptedCargoIds, setAcceptedCargoIds] = useState<Set<number>>(new Set())
  const [actionedLoads, setActionedLoads] = useState<Map<number, LoadResponse>>(new Map())
  const [acceptingCargoId, setAcceptingCargoId] = useState<number | undefined>(undefined)
  const [acceptError, setAcceptError] = useState<string | null>(null)
  const seededRef = useRef(false)

  const driverProfileQuery = useDriverProfileQuery({ driverId: DEMO_DRIVER_ID })
  const regionListQuery = useRegionListQuery()
  const updateRoutePreferencesMutation = useUpdateRoutePreferencesMutation()
  const filter = toLoadFilter(searchParams.get('filter'))
  const loadListQuery = useLoadListQuery({ driverId: DEMO_DRIVER_ID, filter })
  const acceptLoadMutation = useAcceptLoadMutation()

  useEffect(() => {
    if (seededRef.current) return
    const profile = driverProfileQuery.data
    if (!profile) return

    seededRef.current = true
    const origin = profile.routePreferences?.origins?.[0]
    const destination = profile.routePreferences?.destinations?.[0]
    setRoute({
      originSido: origin?.sido ?? '',
      originSigungu: origin?.sigungu ?? '',
      destSido: destination?.sido ?? '',
      destSigungu: destination?.sigungu ?? '',
    })
  }, [driverProfileQuery.data])

  const saveRoute = (next: RouteDraft) => {
    setRoute(next)
    updateRoutePreferencesMutation.mutate(
      {
        params: { driverId: DEMO_DRIVER_ID },
        body: {
          origins: [
            { sido: next.originSido || undefined, sigungu: next.originSigungu || undefined },
          ],
          destinations: [
            { sido: next.destSido || undefined, sigungu: next.destSigungu || undefined },
          ],
        },
      },
      { onSuccess: () => loadListQuery.refetch() },
    )
  }

  const handleAccept = async (cargoId: number) => {
    setAcceptError(null)
    setAcceptingCargoId(cargoId)
    const targetLoad = (loadListQuery.data ?? []).find((load) => load.cargoId === cargoId)
    try {
      await acceptLoadMutation.mutateAsync({ cargoId, driverId: DEMO_DRIVER_ID })
      setAcceptedCargoIds((prev) => new Set(prev).add(cargoId))
      if (targetLoad) setActionedLoads((prev) => new Map(prev).set(cargoId, targetLoad))
      await loadListQuery.refetch()
    } catch (error) {
      setAcceptError(error instanceof Error ? error.message : '화물을 수락하지 못했어요.')
    } finally {
      setAcceptingCargoId(undefined)
    }
  }

  const handleHide = (cargoId: number) => {
    const targetLoad = (loadListQuery.data ?? []).find((load) => load.cargoId === cargoId)
    setHiddenCargoIds((prev) => new Set(prev).add(cargoId))
    if (targetLoad) setActionedLoads((prev) => new Map(prev).set(cargoId, targetLoad))
  }

  const mergedLoads = [...(loadListQuery.data ?? [])]
  actionedLoads.forEach((load, cargoId) => {
    if (!mergedLoads.some((item) => item.cargoId === cargoId)) mergedLoads.push(load)
  })

  const visibleLoads = mergedLoads.filter((load) => {
    if (load.cargoId === undefined) return false
    if (filter === 'ACCEPTED') {
      return acceptedCargoIds.size === 0 || acceptedCargoIds.has(load.cargoId)
    }
    if (filter === 'HIDDEN') return hiddenCargoIds.size === 0 || hiddenCargoIds.has(load.cargoId)
    return !hiddenCargoIds.has(load.cargoId)
  })

  const handleFilterChange = (nextFilter: LoadFilter) => {
    const nextParams = new URLSearchParams(searchParams)
    nextParams.set('filter', nextFilter)
    setSearchParams(nextParams)
  }

  return (
    <PageLayout>
      <Hero
        eyebrow="✦ 내 지역 화물만 골라보기"
        title={
          <>
            오늘 뜬 화물,
            <br />
            바로 확인하세요
          </>
        }
        description="활동 지역과 조건에 맞는 화물만 추천해서 놓치지 않게 알려드려요."
      />

      <RouteFilterCard
        regions={regionListQuery.data ?? []}
        originSido={route.originSido}
        originSigungu={route.originSigungu}
        destSido={route.destSido}
        destSigungu={route.destSigungu}
        onOriginSidoChange={(sido) => saveRoute({ ...route, originSido: sido, originSigungu: '' })}
        onOriginSigunguChange={(sigungu) => saveRoute({ ...route, originSigungu: sigungu })}
        onDestSidoChange={(sido) => saveRoute({ ...route, destSido: sido, destSigungu: '' })}
        onDestSigunguChange={(sigungu) => saveRoute({ ...route, destSigungu: sigungu })}
      />

      {acceptError && <ErrorText role="alert">{acceptError}</ErrorText>}

      <LoadOfferList
        loads={visibleLoads}
        onRefresh={() => loadListQuery.refetch()}
        onAccept={handleAccept}
        onHide={handleHide}
        acceptingCargoId={acceptingCargoId}
        filter={filter}
        onFilterChange={handleFilterChange}
      />
    </PageLayout>
  )
}
