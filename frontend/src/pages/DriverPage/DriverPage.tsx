import { useEffect, useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import styled from '@emotion/styled'
import { AlertDialog } from '@/components/AlertDialog'
import { Hero } from '@/components/Hero'
import { PageLayout } from '@/components/PageLayout'
import { useDriverProfileQuery, useUpdateRoutePreferencesMutation } from '@/api/driver/service'
import { useRegionListQuery } from '@/api/region/service'
import {
  useAcceptLoadMutation,
  useCompleteLoadMutation,
  useCancelLoadMutation,
  useHideLoadMutation,
  useLoadListQuery,
} from '@/api/load/service'
import type { LoadFilter, NextLoadRecommendationResponse } from '@/api/load/model'
import { getCargoTypeLabel } from '@/utils/cargoType'
import { formatFare, formatShortDateTime } from '@/utils/format'
import { RouteFilterCard } from './RouteFilterCard'
import { LoadOfferList } from './LoadOfferList'
import { DriverProfileSummary } from './DriverProfileSummary'

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

const Recommendation = styled.div`
  display: grid;
  gap: 10px;
  text-align: left;

  strong {
    color: ${(props) => props.theme.color.text};
    font-size: 18px;
    line-height: 1.4;
  }

  p {
    margin: 0;
  }

  ul {
    margin: 0;
    padding-left: 20px;
  }
`

const ProfileLink = styled(Link)`
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 44px;
  margin-left: auto;
  padding: 10px 14px;
  border: 2px solid ${(props) => props.theme.color.navy300};
  border-radius: ${(props) => props.theme.radius.md};
  background: ${(props) => props.theme.color.blue100};
  color: ${(props) => props.theme.color.navy700};
  font-size: 15px;
  font-weight: 750;
  text-decoration: none;

  &:focus-visible {
    outline: 3px solid ${(props) => props.theme.color.focus};
    outline-offset: 2px;
  }
`

export function DriverPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [route, setRoute] = useState<RouteDraft>(EMPTY_ROUTE)
  const [acceptingCargoId, setAcceptingCargoId] = useState<number | undefined>(undefined)
  const [acceptError, setAcceptError] = useState<string | null>(null)
  const [acceptComplete, setAcceptComplete] = useState(false)
  const [nextRecommendation, setNextRecommendation] =
    useState<NextLoadRecommendationResponse | null>(null)
  const [completingCargoId, setCompletingCargoId] = useState<number | undefined>(undefined)
  const [cancelingCargoId, setCancelingCargoId] = useState<number | undefined>(undefined)
  const seededRef = useRef(false)
  const loadMoreObserverRef = useRef<IntersectionObserver | null>(null)

  const driverProfileQuery = useDriverProfileQuery({ driverId: DEMO_DRIVER_ID })
  const regionListQuery = useRegionListQuery()
  const updateRoutePreferencesMutation = useUpdateRoutePreferencesMutation()
  const filter = toLoadFilter(searchParams.get('filter'))
  const loadListQuery = useLoadListQuery({ driverId: DEMO_DRIVER_ID, filter })
  const acceptLoadMutation = useAcceptLoadMutation()
  const hideLoadMutation = useHideLoadMutation()
  const completeLoadMutation = useCompleteLoadMutation()
  const cancelLoadMutation = useCancelLoadMutation()

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
    try {
      await acceptLoadMutation.mutateAsync({ cargoId, driverId: DEMO_DRIVER_ID })
      setAcceptComplete(true)
      await loadListQuery.refetch()
      return true
    } catch (error) {
      setAcceptError(error instanceof Error ? error.message : '화물을 수락하지 못했어요.')
      return false
    } finally {
      setAcceptingCargoId(undefined)
    }
  }

  const handleHide = async (cargoId: number) => {
    setAcceptError(null)
    try {
      await hideLoadMutation.mutateAsync({ cargoId, driverId: DEMO_DRIVER_ID })
      await loadListQuery.refetch()
    } catch (error) {
      setAcceptError(error instanceof Error ? error.message : '화물을 숨기지 못했어요.')
    }
  }

  const handleComplete = async (cargoId: number) => {
    setAcceptError(null)
    setCompletingCargoId(cargoId)
    try {
      const response = await completeLoadMutation.mutateAsync({
        cargoId,
        driverId: DEMO_DRIVER_ID,
      })
      setNextRecommendation(response.nextLoadRecommendation ?? null)
      await loadListQuery.refetch()
    } catch (error) {
      setAcceptError(error instanceof Error ? error.message : '하차 완료 처리에 실패했어요.')
    } finally {
      setCompletingCargoId(undefined)
    }
  }

  const handleAcceptRecommendation = async () => {
    const cargoId = nextRecommendation?.cargoId
    if (cargoId === undefined) return
    const accepted = await handleAccept(cargoId)
    if (accepted) setNextRecommendation(null)
  }

  const handleCancelAcceptance = async (cargoId: number) => {
    setAcceptError(null)
    setCancelingCargoId(cargoId)
    try {
      await cancelLoadMutation.mutateAsync({ cargoId, driverId: DEMO_DRIVER_ID })
      await loadListQuery.refetch()
      return true
    } catch (error) {
      setAcceptError(error instanceof Error ? error.message : '화물 수락을 취소하지 못했어요.')
      return false
    } finally {
      setCancelingCargoId(undefined)
    }
  }

  const visibleLoads = (loadListQuery.data?.pages ?? [])
    .flatMap((page) => page.content ?? [])
    .filter((load) => load.cargoId !== undefined)

  const setLoadMoreRef = (node: HTMLDivElement | null) => {
    loadMoreObserverRef.current?.disconnect()
    if (!node || !loadListQuery.hasNextPage || typeof IntersectionObserver === 'undefined') return

    loadMoreObserverRef.current = new IntersectionObserver((entries) => {
      if (entries[0]?.isIntersecting && !loadListQuery.isFetchingNextPage) {
        loadListQuery.fetchNextPage()
      }
    })
    loadMoreObserverRef.current.observe(node)
  }

  const handleFilterChange = (nextFilter: LoadFilter) => {
    const nextParams = new URLSearchParams(searchParams)
    nextParams.set('filter', nextFilter)
    setSearchParams(nextParams)
  }

  return (
    <PageLayout headerRight={<ProfileLink to="/driver/profile">내 정보</ProfileLink>}>
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

      <DriverProfileSummary profile={driverProfileQuery.data} />

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
        hasNextPage={loadListQuery.hasNextPage}
        loadingMore={loadListQuery.isFetchingNextPage}
        loadMoreRef={setLoadMoreRef}
        onComplete={handleComplete}
        completingCargoId={completingCargoId}
        onCancelAcceptance={handleCancelAcceptance}
        cancelingCargoId={cancelingCargoId}
      />

      {acceptComplete && (
        <AlertDialog
          title="화물을 수락했어요"
          description="수락한 화물은 수락 필터에서 다시 확인할 수 있어요."
          confirmLabel="확인"
          onConfirm={() => setAcceptComplete(false)}
        />
      )}

      {nextRecommendation && (
        <AlertDialog
          title="다음 추천 화물이 있어요"
          description={
            <Recommendation>
              <strong>
                {nextRecommendation.origin ?? '-'} → {nextRecommendation.destination ?? '-'}
              </strong>
              <p>
                {formatShortDateTime(nextRecommendation.loadingAt)} 출발 ·{' '}
                {formatShortDateTime(nextRecommendation.unloadingAt)} 도착
              </p>
              <p>
                {getCargoTypeLabel(nextRecommendation.cargoType)} ·{' '}
                {nextRecommendation.weightTon ?? '-'}톤 · {formatFare(nextRecommendation.fare)}
              </p>
              {nextRecommendation.recommendationSummary && (
                <p>{nextRecommendation.recommendationSummary}</p>
              )}
              {(nextRecommendation.recommendationReasons?.length ?? 0) > 0 && (
                <ul>
                  {nextRecommendation.recommendationReasons?.map((reason) => (
                    <li key={reason}>{reason}</li>
                  ))}
                </ul>
              )}
            </Recommendation>
          }
          confirmLabel="수락"
          cancelLabel="닫기"
          onConfirm={handleAcceptRecommendation}
          onCancel={() => setNextRecommendation(null)}
          busy={acceptingCargoId === nextRecommendation.cargoId}
        />
      )}
    </PageLayout>
  )
}
