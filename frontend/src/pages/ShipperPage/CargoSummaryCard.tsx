import styled from '@emotion/styled'
import { Card } from '@/components/Card'
import { Button } from '@/components/Button'
import { getCargoTypeLabel } from '@/utils/cargoType'
import { formatDateTime, formatFare } from '@/utils/format'
import type { CargoDetailResponse } from '@/api/cargo/model'

export interface CargoSummaryCardProps {
  cargo: CargoDetailResponse
  onEdit: () => void
  onRematch: () => void
  rematchLoading: boolean
}

const Head = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 22px;
`

const Title = styled.h2`
  margin: 0;
  font-size: 19px;
  font-weight: 800;
  color: ${(props) => props.theme.color.navy900};
`

const Status = styled.span`
  flex: none;
  padding: 9px 12px;
  border-radius: 10px;
  background: ${(props) => props.theme.color.blue100};
  color: ${(props) => props.theme.color.navy700};
  font-size: 14px;
  font-weight: 800;
`

const Route = styled.div`
  display: grid;
  grid-template-columns: 1fr 44px 1fr;
  align-items: center;
  gap: 10px;
  padding: 4px 0 20px;
`

const Place = styled.div`
  min-width: 0;
  text-align: center;
`

const PlaceLabel = styled.span`
  display: block;
  margin-bottom: 6px;
  color: ${(props) => props.theme.color.muted};
  font-size: 15px;
  font-weight: 700;
`

const City = styled.strong`
  display: block;
  color: ${(props) => props.theme.color.text};
  font-size: 22px;
  font-weight: 850;
  line-height: 1.25;
`

const Time = styled.time`
  display: block;
  margin-top: 7px;
  color: #24305f;
  font-size: 17px;
  font-weight: 800;
  line-height: 1.3;
  white-space: nowrap;
`

const Arrow = styled.div`
  text-align: center;
  color: ${(props) => props.theme.color.navy500};
  font-size: 34px;
  font-weight: 700;
  line-height: 1;
`

const Meta = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 14px;
  padding: 20px 0;
`

const CargoTypeText = styled.span`
  font-size: 18px;
  font-weight: 800;
  color: ${(props) => props.theme.color.text};
`

const Separator = styled.span`
  width: 1px;
  height: 24px;
  background: ${(props) => props.theme.color.border};
`

const FareText = styled.span`
  font-size: 18px;
  color: ${(props) => props.theme.color.text};

  strong {
    color: ${(props) => props.theme.color.navy900};
    font-size: 24px;
    font-weight: 850;
  }
`

const Actions = styled.div`
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  padding-top: 16px;
  border-top: 1px solid ${(props) => props.theme.color.border};
`

function placeName(sido: string | undefined, sigungu: string | undefined): string {
  if (!sido) return '-'
  return sigungu && sigungu !== '전체' ? `${sido} ${sigungu}` : sido
}

export function CargoSummaryCard({
  cargo,
  onEdit,
  onRematch,
  rematchLoading,
}: CargoSummaryCardProps) {
  return (
    <Card>
      <Head>
        <Title>화물 정보</Title>
        <Status>AI 변환</Status>
      </Head>

      <Route>
        <Place>
          <PlaceLabel>출발지</PlaceLabel>
          <City>{placeName(cargo.origin?.sido, cargo.origin?.sigungu)}</City>
          <Time dateTime={cargo.loadingAt}>{formatDateTime(cargo.loadingAt)}</Time>
        </Place>
        <Arrow aria-hidden="true">→</Arrow>
        <Place>
          <PlaceLabel>도착지</PlaceLabel>
          <City>{placeName(cargo.destination?.sido, cargo.destination?.sigungu)}</City>
          <Time dateTime={cargo.unloadingAt}>{formatDateTime(cargo.unloadingAt)}</Time>
        </Place>
      </Route>

      <Meta>
        <CargoTypeText>
          {getCargoTypeLabel(cargo.cargoType)} · {cargo.weightTon ?? '-'}톤
        </CargoTypeText>
        <Separator aria-hidden="true" />
        <FareText>
          운임 <strong>{formatFare(cargo.desiredFare)}</strong>
        </FareText>
      </Meta>

      <Actions>
        <Button type="button" variant="ghost" onClick={onRematch} disabled={rematchLoading}>
          {rematchLoading ? '재매칭 중...' : '재매칭'}
        </Button>
        <Button type="button" onClick={onEdit}>
          수정하기
        </Button>
      </Actions>
    </Card>
  )
}
