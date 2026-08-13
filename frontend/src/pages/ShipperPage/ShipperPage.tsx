import { useEffect, useState } from 'react'
import styled from '@emotion/styled'
import { useQueryClient } from '@tanstack/react-query'
import { useNavigate, useParams } from 'react-router-dom'
import { Hero } from '@/components/Hero'
import { Button } from '@/components/Button'
import { PageLayout } from '@/components/PageLayout'
import { FareWarningBanner } from '@/components/FareWarningBanner'
import {
  useCargoCreateMutation,
  useCargoDetailQuery,
  useMyCargosQuery,
  useCargoParseMutation,
} from '@/api/cargo/service'
import type { CreateCargoResponse } from '@/api/cargo/model'
import { cargoQueryKeys } from '@/api/cargo/queries'
import { useRegionListQuery } from '@/api/region/service'
import { toCargoType } from '@/utils/cargoType'
import { CargoRequestForm } from './CargoRequestForm'
import { CargoSummaryCard } from './CargoSummaryCard'
import { MatchedDriverCard } from './MatchedDriverCard'
import { MyCargoList } from './MyCargoList'
import { ManualCargoForm } from './ManualCargoForm'
import type { ManualCargoDraft } from './ManualCargoForm'

/** 이 화면은 화주 로그인이 없는 데모라 화주 id를 고정값으로 둔다. */
const DEMO_SHIPPER_ID = 1
const DetailHead = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: 4px 0 12px;
`

const DetailTitle = styled.h1`
  margin: 0;
  color: ${(props) => props.theme.color.text};
  font-size: 19px;
  font-weight: 800;
`
const EMPTY_MANUAL_CARGO: ManualCargoDraft = {
  originSido: '',
  originSigungu: '',
  destinationSido: '',
  destinationSigungu: '',
  loadingAt: '',
  unloadingAt: '',
  cargoType: 'GENERAL',
  weightTon: '',
  desiredFare: '',
}

function extractCargoId(response: CreateCargoResponse): number | undefined {
  const value = response['cargoId']
  return typeof value === 'number' ? value : undefined
}

export function ShipperPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { cargoId: cargoIdParam } = useParams()
  const routeCargoId = cargoIdParam ? Number(cargoIdParam) : undefined
  const [rawText, setRawText] = useState('')
  const [cargoId, setCargoId] = useState<number | undefined>(() =>
    Number.isFinite(routeCargoId) ? routeCargoId : undefined,
  )
  const [formError, setFormError] = useState<string | null>(null)
  const [showCreate, setShowCreate] = useState(false)
  const [cargoPage, setCargoPage] = useState(0)
  const [manualMode, setManualMode] = useState(false)
  const [manualCargo, setManualCargo] = useState(EMPTY_MANUAL_CARGO)
  const [selectedFromList, setSelectedFromList] = useState(false)

  const parseMutation = useCargoParseMutation()
  const createMutation = useCargoCreateMutation()
  const regionListQuery = useRegionListQuery()
  const cargoDetailQuery = useCargoDetailQuery(cargoId)
  const myCargosQuery = useMyCargosQuery({
    shipperId: DEMO_SHIPPER_ID,
    pageable: { page: 0, size: 1000 },
  })

  const isSubmitting = parseMutation.isPending || createMutation.isPending

  useEffect(() => {
    if (!cargoDetailQuery.isError) return

    setCargoId(undefined)
    setSelectedFromList(false)
    setShowCreate(false)
    setManualMode(false)
    navigate('/shipper', { replace: true })
    void queryClient.invalidateQueries({ queryKey: cargoQueryKeys.myCargosAll() })
  }, [cargoDetailQuery.isError, navigate, queryClient])

  const handleConvert = async () => {
    setFormError(null)

    if (!rawText.trim()) {
      setFormError('화물 요청 내용을 입력해 주세요.')
      return
    }

    try {
      const parsed = await parseMutation.mutateAsync({ requestText: rawText })
      const originSido = parsed.origin?.sido
      const originSigungu = parsed.origin?.sigungu
      const destSido = parsed.destination?.sido
      const destSigungu = parsed.destination?.sigungu
      const cargoType = toCargoType(parsed.cargoType)
      const weightTon =
        typeof parsed.weightTon === 'number' &&
        Number.isFinite(parsed.weightTon) &&
        parsed.weightTon > 0
          ? parsed.weightTon
          : undefined
      const desiredFare =
        typeof parsed.offeredFareKrw === 'number' &&
        Number.isFinite(parsed.offeredFareKrw) &&
        parsed.offeredFareKrw > 0
          ? parsed.offeredFareKrw
          : undefined

      // 인식 못한 항목은 빈 값으로 두고 등록 폼에서 에러 표시 + 등록 비활성으로 안내한다.
      setManualCargo({
        originSido: originSido ?? '',
        originSigungu: originSigungu ?? '',
        destinationSido: destSido ?? '',
        destinationSigungu: destSigungu ?? '',
        loadingAt: parsed.loadingDate ? `${parsed.loadingDate}T09:00` : '',
        unloadingAt: parsed.unloadingDate ? `${parsed.unloadingDate}T18:00` : '',
        cargoType: cargoType ?? '',
        weightTon: weightTon === undefined ? '' : String(weightTon),
        desiredFare: desiredFare === undefined ? '' : String(desiredFare),
      })
      setManualMode(true)

      if (
        !originSido ||
        !originSigungu ||
        !destSido ||
        !destSigungu ||
        !cargoType ||
        weightTon === undefined ||
        desiredFare === undefined ||
        !parsed.loadingDate ||
        !parsed.unloadingDate
      ) {
        const detail = parsed.missingFields?.length
          ? ` (누락: ${parsed.missingFields.join(', ')})`
          : ''
        setFormError(`원문에서 인식하지 못한 항목이 있어요. 직접 채워주세요.${detail}`)
      }
    } catch (error) {
      // 변환에 실패해도 빈 등록 폼으로 넘겨서 직접 입력할 수 있게 한다.
      setManualCargo(EMPTY_MANUAL_CARGO)
      setManualMode(true)
      setFormError(
        error instanceof Error
          ? `${error.message} 직접 입력해주세요.`
          : '요청을 처리하지 못했어요. 직접 입력해주세요.',
      )
    }
  }

  const handleManualSubmit = async () => {
    setFormError(null)
    if (
      !manualCargo.originSido ||
      !manualCargo.originSigungu ||
      !manualCargo.destinationSido ||
      !manualCargo.destinationSigungu ||
      !manualCargo.loadingAt ||
      !manualCargo.unloadingAt ||
      !manualCargo.weightTon ||
      !manualCargo.desiredFare
    ) {
      setFormError('필수 항목을 모두 입력해 주세요.')
      return
    }

    const cargoType = toCargoType(manualCargo.cargoType)
    if (!cargoType) return

    try {
      const created = await createMutation.mutateAsync({
        params: { shipperId: DEMO_SHIPPER_ID },
        body: {
          aiParsed: parseMutation.isSuccess,
          origin: { sido: manualCargo.originSido, sigungu: manualCargo.originSigungu },
          destination: {
            sido: manualCargo.destinationSido,
            sigungu: manualCargo.destinationSigungu,
          },
          cargoType,
          weightTon: Number(manualCargo.weightTon),
          desiredFare: Number(manualCargo.desiredFare),
          loadingAt: manualCargo.loadingAt,
          unloadingAt: manualCargo.unloadingAt,
        },
      })
      const newCargoId = extractCargoId(created)
      if (newCargoId === undefined) throw new Error('화물 등록에 실패했어요.')
      setCargoId(newCargoId)
      setManualMode(false)
      setShowCreate(false)
      navigate(`/shipper/cargos/${newCargoId}`, { replace: true })
      void queryClient.invalidateQueries({ queryKey: cargoQueryKeys.myCargosAll() })
    } catch {
      // 등록 API 오류는 입력 폼 하단에 노출하지 않는다.
    }
  }

  const cargo = cargoDetailQuery.data

  return (
    <PageLayout>
      {cargoId !== undefined && !cargo ? null : !showCreate && !cargo ? (
        <>
          <Hero
            eyebrow="✦ 내 화물 한눈에 보기"
            title={
              <>
                등록한 화물,
                <br />
                진행 상황 확인하세요
              </>
            }
            description="배차부터 운송 완료까지 상태별로 모아서 보여드려요."
          />

          <MyCargoList
            cargos={myCargosQuery.data?.content ?? []}
            loading={myCargosQuery.isLoading}
            error={myCargosQuery.isError}
            onCreate={() => setShowCreate(true)}
            page={cargoPage}
            onPageChange={setCargoPage}
            onSelect={(selectedCargoId) => {
              setSelectedFromList(true)
              setCargoId(selectedCargoId)
              navigate(`/shipper/cargos/${selectedCargoId}`)
            }}
          />
        </>
      ) : showCreate && !cargo && !manualMode ? (
        <>
          <Hero
            eyebrow="✦ AI로 빠른 매칭"
            title={
              <>
                화물 등록하고
                <br />
                3초 만에 기사 찾기
              </>
            }
            description="카톡 요청을 그대로 붙여넣으면 AI가 정리부터 매칭까지 끝내요."
          />

          <CargoRequestForm
            value={rawText}
            onChange={setRawText}
            onSubmit={handleConvert}
            loading={isSubmitting}
            error={formError}
            onManualEntry={() => {
              setFormError(null)
              parseMutation.reset()
              setManualCargo(EMPTY_MANUAL_CARGO)
              setManualMode(true)
            }}
          />
        </>
      ) : manualMode && !cargo ? (
        <>
          <Hero
            eyebrow={parseMutation.isSuccess ? '✦ AI가 정리한 내용' : '화물 직접 등록'}
            title="화물 정보 입력"
            description={
              parseMutation.isSuccess
                ? '원문에서 뽑아낸 정보예요. 맞는지 확인하고 등록해주세요.'
                : '배차에 필요한 정보를 항목별로 입력해주세요.'
            }
          />
          <ManualCargoForm
            value={manualCargo}
            onChange={setManualCargo}
            onSubmit={handleManualSubmit}
            onCancel={() => {
              // 요청 입력 화면으로 돌아갈 때 이전 원문·변환 결과를 남기지 않는다.
              setManualMode(false)
              setManualCargo(EMPTY_MANUAL_CARGO)
              setRawText('')
              setFormError(null)
              parseMutation.reset()
            }}
            loading={createMutation.isPending}
            error={formError}
            showErrors={parseMutation.isSuccess || parseMutation.isError}
            regions={regionListQuery.data ?? []}
          />
        </>
      ) : null}

      {cargo && (
        <>
          {selectedFromList && (
            <DetailHead>
              <DetailTitle>
                {cargo.status === 'COMPLETED'
                  ? '운송 상세'
                  : cargo.status === 'MATCHED'
                    ? '배차 상세'
                    : '화물 상세'}
              </DetailTitle>
              <Button
                type="button"
                variant="ghost"
                onClick={() => {
                  setCargoId(undefined)
                  setSelectedFromList(false)
                  navigate('/shipper')
                }}
              >
                ← 목록
              </Button>
            </DetailHead>
          )}
          <CargoSummaryCard
            cargo={cargo}
            onEdit={() => {
              setCargoId(undefined)
              setShowCreate(true)
            }}
            onRematch={() => cargoDetailQuery.refetch()}
            rematchLoading={cargoDetailQuery.isFetching}
            showActions={!selectedFromList}
          />

          {cargo.fare?.verdict === 'LOW' && cargo.fare.message && (
            <FareWarningBanner message={cargo.fare.message} />
          )}

          <MatchedDriverCard
            driver={cargo.assignedDriver}
            fareVerdict={cargo.fare?.verdict}
            cargoStatus={cargo.status}
          />
        </>
      )}
    </PageLayout>
  )
}
