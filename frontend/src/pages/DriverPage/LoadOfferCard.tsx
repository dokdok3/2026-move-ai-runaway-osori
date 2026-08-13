import styled from '@emotion/styled'
import { Badge } from '@/components/Badge'
import { Button } from '@/components/Button'
import { FareWarningBanner } from '@/components/FareWarningBanner'
import { getCargoTypeLabel } from '@/utils/cargoType'
import { formatFare, formatShortDateTime } from '@/utils/format'
import type { LoadResponse } from '@/api/load/model'

export interface LoadOfferCardProps {
  load: LoadResponse
  isBest: boolean
  onAccept: () => void
  onHide: () => void
  accepting: boolean
}

const Wrapper = styled.div<{ isBest: boolean }>`
  padding: 16px;
  margin-bottom: 14px;
  border-radius: 12px;
  background: ${(props) => props.theme.color.white};
  border: 1px solid
    ${(props) => (props.isBest ? props.theme.color.navy700 : props.theme.color.border)};
  box-shadow: ${(props) => (props.isBest ? '0 10px 28px rgba(40, 85, 217, 0.1)' : 'none')};
`

const BestLabel = styled.div`
  display: inline-flex;
  align-items: center;
  margin-bottom: 12px;
  padding: 7px 9px;
  border-radius: 7px;
  background: ${(props) => props.theme.color.navy700};
  color: ${(props) => props.theme.color.white};
  font-size: 14px;
  font-weight: 700;
`

const Top = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 12px;
`

const Route = styled.div`
  font-size: 15px;
  font-weight: 700;
  color: ${(props) => props.theme.color.navy900};
`

const TimeRow = styled.div`
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 7px;
  margin: 10px 0;
  font-size: 15px;
  color: #465365;
`

const TagChip = styled.span<{ tone: 'pickup' | 'dropoff' }>`
  padding: 5px 8px;
  border-radius: 7px;
  font-size: 13px;
  font-weight: 700;
  color: ${(props) => (props.tone === 'pickup' ? '#9a4a44' : '#33578c')};
  background: ${(props) => (props.tone === 'pickup' ? '#fbebea' : props.theme.color.blue100)};
`

const Sub = styled.div`
  margin-bottom: 10px;
  font-size: 15px;
  color: #586474;
`

const Price = styled.div`
  margin-bottom: 14px;
  font-size: 23px;
  font-weight: 800;
  color: ${(props) => props.theme.color.text};
`

const Actions = styled.div`
  display: flex;
  gap: 8px;

  > * {
    flex: 1;
  }
`

export function LoadOfferCard({ load, isBest, onAccept, onHide, accepting }: LoadOfferCardProps) {
  const isLow = load.badge === 'LOW'
  const badgeLabel = isLow
    ? `운임 ${load.belowPercent ?? 0}% 낮음`
    : load.badge === 'FAIR'
      ? '적정 운임'
      : '시세 정보 없음'

  return (
    <Wrapper isBest={isBest}>
      {isBest && <BestLabel>✦ 최적 매칭</BestLabel>}

      <Top>
        <Route>
          {load.origin} → {load.destination}
        </Route>
        <Badge tone={isLow ? 'warn' : load.badge === 'FAIR' ? 'ok' : 'neutral'}>{badgeLabel}</Badge>
      </Top>

      <TimeRow>
        <TagChip tone="pickup">상차</TagChip>
        {formatShortDateTime(load.loadingAt)}
        <TagChip tone="dropoff">하차</TagChip>
        {formatShortDateTime(load.unloadingAt)}
      </TimeRow>

      <Sub>
        {load.bodyType} · {load.weightTon}톤 · {getCargoTypeLabel(load.cargoType)}
      </Sub>

      <Price>{formatFare(load.fare)}</Price>

      {isLow && load.regionAverageFare !== undefined && (
        <FareWarningBanner
          message={`이 구간의 평균 운임은 ${formatFare(load.regionAverageFare)}예요`}
        />
      )}

      <Actions>
        <Button type="button" variant="ghost" onClick={onHide} disabled={accepting}>
          숨기기
        </Button>
        <Button type="button" onClick={onAccept} disabled={accepting}>
          {accepting ? '수락 중...' : '수락'}
        </Button>
      </Actions>
    </Wrapper>
  )
}
