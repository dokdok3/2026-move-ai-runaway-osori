import { useState } from 'react'
import styled from '@emotion/styled'
import { AlertDialog } from '@/components/AlertDialog'
import { Badge } from '@/components/Badge'
import { Button } from '@/components/Button'
import { FareWarningBanner } from '@/components/FareWarningBanner'
import { getCargoTypeLabel } from '@/utils/cargoType'
import { formatFare, formatShortDateTime } from '@/utils/format'
import type { LoadResponse } from '@/api/load/model'
import type { LoadFilter } from '@/api/load/model'

export interface LoadOfferCardProps {
  load: LoadResponse
  isBest: boolean
  onAccept: () => Promise<boolean>
  onHide: () => void
  accepting: boolean
  filter: LoadFilter
  onComplete: () => void
  completing: boolean
}

const Wrapper = styled.div<{ isBest: boolean }>`
  padding: 18px;
  margin-bottom: 14px;
  border-radius: 12px;
  background: ${(props) => props.theme.color.white};
  border: ${(props) => (props.isBest ? '3px' : '1px')} solid
    ${(props) => (props.isBest ? props.theme.color.navy700 : props.theme.color.border)};
  box-shadow: none;

  @media (max-width: 380px) {
    padding: 16px;
  }
`

const Top = styled.div`
  display: flex;
  justify-content: flex-start;
  align-items: center;
  gap: 10px;
  margin-bottom: 18px;

  > span:last-child {
    margin-left: auto;
  }
`

const Route = styled.div`
  display: grid;
  grid-template-columns: 1fr 44px 1fr;
  align-items: center;
  gap: 10px;
  padding: 2px 0 14px;

  @media (max-width: 380px) {
    grid-template-columns: 1fr 32px 1fr;
    gap: 4px;
  }
`

const Place = styled.div`
  min-width: 0;
  text-align: center;
`

const PlaceLabel = styled.span`
  display: block;
  margin-bottom: 4px;
  color: ${(props) => props.theme.color.label};
  font-size: 14px;
  font-weight: 600;
`

const City = styled.strong`
  display: block;
  color: ${(props) => props.theme.color.text};
  font-size: 26px;
  font-weight: 850;
  line-height: 1.25;
  letter-spacing: -0.02em;

  @media (max-width: 380px) {
    font-size: 22px;
  }
`

const Time = styled.time`
  display: block;
  margin-top: 4px;
  color: ${(props) => props.theme.color.mutedLight};
  font-size: 14px;
  font-weight: 600;
  line-height: 1.3;
  white-space: nowrap;
`

const Arrow = styled.span`
  color: ${(props) => props.theme.color.navy500};
  font-size: 34px;
  font-weight: 700;
  line-height: 1;
  text-align: center;
`

const Sub = styled.div`
  color: ${(props) => props.theme.color.mutedLight};
  font-size: 14px;
  line-height: 1.5;
`

const Price = styled.div`
  font-size: 30px;
  font-weight: 850;
  letter-spacing: -0.03em;
  color: ${(props) => props.theme.color.text};

  @media (max-width: 380px) {
    font-size: 25px;
    text-align: right;
  }
`

const Meta = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;

  @media (max-width: 380px) {
    display: grid;
    gap: 8px;
  }
`

const Actions = styled.div`
  display: flex;
  gap: 8px;

  > * {
    flex: 1;
  }
`

export function LoadOfferCard({
  load,
  isBest,
  onAccept,
  onHide,
  accepting,
  filter,
  onComplete,
  completing,
}: LoadOfferCardProps) {
  const [confirmOpen, setConfirmOpen] = useState(false)
  const isLow = load.badge === 'LOW'
  const badgeLabel = isLow
    ? `운임 ${load.belowPercent ?? 0}% 낮음`
    : load.badge === 'FAIR'
      ? '적정 운임'
      : '시세 정보 없음'

  const handleAccept = async () => {
    const accepted = await onAccept()
    if (accepted) setConfirmOpen(false)
  }

  return (
    <Wrapper isBest={isBest}>
      <Top>
        <Badge tone={isLow ? 'warn' : load.badge === 'FAIR' ? 'ok' : 'neutral'}>{badgeLabel}</Badge>
      </Top>

      <Route>
        <Place>
          <PlaceLabel>출발지</PlaceLabel>
          <City>{load.origin}</City>
          <Time>{formatShortDateTime(load.loadingAt)}</Time>
        </Place>
        <Arrow aria-hidden="true">→</Arrow>
        <Place>
          <PlaceLabel>도착지</PlaceLabel>
          <City>{load.destination}</City>
          <Time>{formatShortDateTime(load.unloadingAt)}</Time>
        </Place>
      </Route>

      <Meta>
        <Sub>
          {load.bodyType} · {load.weightTon}톤 · {getCargoTypeLabel(load.cargoType)}
        </Sub>
        <Price>{formatFare(load.fare)}</Price>
      </Meta>

      {isLow && load.regionAverageFare !== undefined && (
        <FareWarningBanner
          message={`이 구간의 평균 운임은 ${formatFare(load.regionAverageFare)}예요`}
        />
      )}

      <Actions>
        <Button
          type="button"
          variant="ghost"
          onClick={onHide}
          disabled={accepting || completing || filter === 'ACCEPTED'}
        >
          숨기기
        </Button>
        {filter === 'ACCEPTED' ? (
          <Button type="button" onClick={onComplete} disabled={completing}>
            {completing ? '처리 중...' : '하차 완료'}
          </Button>
        ) : (
          <Button type="button" onClick={() => setConfirmOpen(true)} disabled={accepting}>
            {accepting ? '수락 중...' : '수락'}
          </Button>
        )}
      </Actions>

      {confirmOpen && (
        <AlertDialog
          title="이 화물을 수락할까요?"
          description={
            <>
              {load.origin}에서 {load.destination}까지 운행하는 화물이에요.
            </>
          }
          confirmLabel="수락하기"
          onConfirm={handleAccept}
          onCancel={() => setConfirmOpen(false)}
          busy={accepting}
        />
      )}
    </Wrapper>
  )
}
