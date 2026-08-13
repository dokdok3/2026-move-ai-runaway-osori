import { useState } from 'react'
import { Hero } from '@/components/Hero'
import { HeaderBackLink } from '@/components/AppHeader'
import { PageLayout } from '@/components/PageLayout'
import { FareWarningBanner } from '@/components/FareWarningBanner'
import {
  useCargoCreateMutation,
  useCargoDetailQuery,
  useCargoParseMutation,
} from '@/api/cargo/service'
import { useFareQuoteFetcher } from '@/api/fare/service'
import type { CreateCargoResponse } from '@/api/cargo/model'
import { toCargoType } from '@/utils/cargoType'
import { CargoRequestForm } from './CargoRequestForm'
import { CargoSummaryCard } from './CargoSummaryCard'
import { MatchedDriverCard } from './MatchedDriverCard'

/** 이 화면은 화주 로그인이 없는 데모라 화주 id를 고정값으로 둔다. */
const DEMO_SHIPPER_ID = 1

function extractCargoId(response: CreateCargoResponse): number | undefined {
  const value = response['cargoId']
  return typeof value === 'number' ? value : undefined
}

export function ShipperPage() {
  const [rawText, setRawText] = useState('')
  const [cargoId, setCargoId] = useState<number | undefined>(undefined)
  const [formError, setFormError] = useState<string | null>(null)

  const parseMutation = useCargoParseMutation()
  const createMutation = useCargoCreateMutation()
  const fetchFareQuote = useFareQuoteFetcher()
  const cargoDetailQuery = useCargoDetailQuery(cargoId)

  const isSubmitting = parseMutation.isPending || createMutation.isPending

  const handleConvert = async () => {
    setFormError(null)

    if (!rawText.trim()) {
      setFormError('화물 요청 내용을 입력해 주세요.')
      return
    }

    try {
      const parsed = await parseMutation.mutateAsync({ rawText })
      const originSido = parsed.origin?.sido
      const destSido = parsed.destination?.sido
      const weightTon = parsed.weightTon
      const cargoType = toCargoType(parsed.cargoType)

      if (!originSido || !destSido || weightTon === undefined) {
        setFormError(
          '원문에서 출발지·도착지·톤수를 인식하지 못했어요. 조금 더 구체적으로 적어주세요.',
        )
        return
      }

      let desiredFare = parsed.desiredFare
      if (desiredFare === undefined) {
        const quote = await fetchFareQuote({ originSido, destSido, cargoType, weightTon })
        desiredFare = quote.averageFare
      }

      if (desiredFare === undefined) {
        setFormError('구간 시세를 찾지 못해 운임을 추정하지 못했어요. 예산을 함께 적어주세요.')
        return
      }

      const created = await createMutation.mutateAsync({
        params: { shipperId: DEMO_SHIPPER_ID },
        body: {
          origin: { sido: originSido, sigungu: parsed.origin?.sigungu },
          destination: { sido: destSido, sigungu: parsed.destination?.sigungu },
          cargoType,
          cargoDescription: parsed.cargoDescription ?? rawText,
          weightTon,
          vehicleType: parsed.vehicleType,
          desiredFare,
          loadingAt: parsed.loadingAt ?? new Date().toISOString(),
          unloadingAt: parsed.unloadingAt,
        },
      })

      const newCargoId = extractCargoId(created)
      if (newCargoId === undefined) {
        setFormError('화물 등록에 실패했어요. 다시 시도해 주세요.')
        return
      }

      setCargoId(newCargoId)
    } catch (error) {
      setFormError(error instanceof Error ? error.message : '요청을 처리하지 못했어요.')
    }
  }

  const cargo = cargoDetailQuery.data

  return (
    <PageLayout headerRight={<HeaderBackLink />}>
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
      />

      {cargo && (
        <>
          <CargoSummaryCard
            cargo={cargo}
            onEdit={() => setCargoId(undefined)}
            onRematch={() => cargoDetailQuery.refetch()}
            rematchLoading={cargoDetailQuery.isFetching}
          />

          {cargo.fare?.verdict === 'LOW' && cargo.fare.message && (
            <FareWarningBanner message={cargo.fare.message} />
          )}

          <MatchedDriverCard driver={cargo.assignedDriver} fareVerdict={cargo.fare?.verdict} />
        </>
      )}
    </PageLayout>
  )
}
