import { useState } from 'react'
import styled from '@emotion/styled'
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
  origin: '',
  destination: '',
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
  const [rawText, setRawText] = useState('')
  const [cargoId, setCargoId] = useState<number | undefined>(undefined)
  const [formError, setFormError] = useState<string | null>(null)
  const [showCreate, setShowCreate] = useState(false)
  const [cargoPage, setCargoPage] = useState(0)
  const [manualMode, setManualMode] = useState(false)
  const [manualCargo, setManualCargo] = useState(EMPTY_MANUAL_CARGO)
  const [selectedFromList, setSelectedFromList] = useState(false)

  const parseMutation = useCargoParseMutation()
  const createMutation = useCargoCreateMutation()
  const cargoDetailQuery = useCargoDetailQuery(cargoId)
  const myCargosQuery = useMyCargosQuery({
    shipperId: DEMO_SHIPPER_ID,
    pageable: { page: cargoPage, size: 10 },
  })

  const isSubmitting = parseMutation.isPending || createMutation.isPending

  const handleConvert = async () => {
    setFormError(null)

    if (!rawText.trim()) {
      setFormError('화물 요청 내용을 입력해 주세요.')
      return
    }

    try {
      const parsed = await parseMutation.mutateAsync({ requestText: rawText })
      const originSido = parsed.origin?.sido
      const destSido = parsed.destination?.sido
      const weightTon = parsed.weightTon
      const cargoType = toCargoType(parsed.cargoType)

      if (
        !originSido ||
        !destSido ||
        weightTon === undefined ||
        parsed.offeredFareKrw === undefined
      ) {
        const detail = parsed.missingFields?.length
          ? ` (누락: ${parsed.missingFields.join(', ')})`
          : ''
        setFormError(
          `원문에서 출발지·도착지·톤수·운임을 인식하지 못했어요. 조금 더 구체적으로 적어주세요.${detail}`,
        )
        return
      }

      const desiredFare = parsed.offeredFareKrw

      const created = await createMutation.mutateAsync({
        params: { shipperId: DEMO_SHIPPER_ID },
        body: {
          origin: { sido: originSido, sigungu: parsed.origin?.sigungu ?? '전체' },
          destination: { sido: destSido, sigungu: parsed.destination?.sigungu ?? '전체' },
          cargoType,
          cargoDescription: parsed.cargoDescription ?? rawText,
          weightTon,
          desiredFare,
          loadingAt: parsed.loadingDate
            ? `${parsed.loadingDate}T09:00:00`
            : new Date().toISOString(),
          unloadingAt: parsed.unloadingDate
            ? `${parsed.unloadingDate}T18:00:00`
            : new Date().toISOString(),
        },
      })

      const newCargoId = extractCargoId(created)
      if (newCargoId === undefined) {
        setFormError('화물 등록에 실패했어요. 다시 시도해 주세요.')
        return
      }

      setCargoId(newCargoId)
      setShowCreate(true)
      await myCargosQuery.refetch()
    } catch (error) {
      setFormError(error instanceof Error ? error.message : '요청을 처리하지 못했어요.')
    }
  }

  const handleManualSubmit = async () => {
    setFormError(null)
    if (
      !manualCargo.origin.trim() ||
      !manualCargo.destination.trim() ||
      !manualCargo.loadingAt ||
      !manualCargo.unloadingAt ||
      !manualCargo.weightTon ||
      !manualCargo.desiredFare
    ) {
      setFormError('필수 항목을 모두 입력해 주세요.')
      return
    }

    const [originSido, ...originDetail] = manualCargo.origin.trim().split(/\s+/)
    const [destinationSido, ...destinationDetail] = manualCargo.destination.trim().split(/\s+/)

    try {
      const created = await createMutation.mutateAsync({
        params: { shipperId: DEMO_SHIPPER_ID },
        body: {
          origin: { sido: originSido, sigungu: originDetail.join(' ') || '전체' },
          destination: {
            sido: destinationSido,
            sigungu: destinationDetail.join(' ') || '전체',
          },
          cargoType: manualCargo.cargoType,
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
      await myCargosQuery.refetch()
    } catch (error) {
      setFormError(error instanceof Error ? error.message : '화물 등록에 실패했어요.')
    }
  }

  const cargo = cargoDetailQuery.data

  return (
    <PageLayout>
      {!showCreate && !cargo ? (
        <MyCargoList
          cargos={myCargosQuery.data?.content ?? []}
          loading={myCargosQuery.isLoading}
          error={myCargosQuery.isError}
          onCreate={() => setShowCreate(true)}
          page={myCargosQuery.data?.page ?? cargoPage}
          totalPages={myCargosQuery.data?.totalPages ?? 0}
          onPageChange={setCargoPage}
          onSelect={(selectedCargoId) => {
            setSelectedFromList(true)
            setCargoId(selectedCargoId)
          }}
        />
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
              setManualMode(true)
            }}
          />
        </>
      ) : manualMode && !cargo ? (
        <>
          <Hero
            eyebrow="화물 직접 등록"
            title="화물 정보 입력"
            description="배차에 필요한 정보를 항목별로 입력해주세요."
          />
          <ManualCargoForm
            value={manualCargo}
            onChange={setManualCargo}
            onSubmit={handleManualSubmit}
            onCancel={() => setManualMode(false)}
            loading={createMutation.isPending}
            error={formError}
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
