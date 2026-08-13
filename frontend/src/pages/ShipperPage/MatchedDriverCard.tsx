import styled from '@emotion/styled'
import { Card } from '@/components/Card'
import { Badge } from '@/components/Badge'
import { Button } from '@/components/Button'
import { formatFare } from '@/utils/format'
import type { DriverResponse } from '@/api/driver/model'

export interface MatchedDriverCardProps {
  driver: DriverResponse | undefined
  fareVerdict: string | undefined
  cargoStatus?: string
}

const Title = styled.div`
  margin-bottom: 14px;
  font-size: 19px;
  font-weight: 800;
  color: ${(props) => props.theme.color.navy900};
`

const DriverRow = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 15px;
  border: 1px solid ${(props) => props.theme.color.border};
  border-radius: ${(props) => props.theme.radius.md};
  font-size: 16px;
  font-weight: 600;
`

const DriverLeft = styled.div`
  display: flex;
  align-items: center;
  gap: 12px;
`

const Avatar = styled.div`
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  border-radius: 50%;
  background: ${(props) => props.theme.color.blue100};
  border: 1px solid ${(props) => props.theme.color.border};
`

const DataGrid = styled.dl`
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.35fr);
  align-items: baseline;
  column-gap: 18px;
  margin: 14px 0 0;
`

const Dt = styled.dt`
  padding: 10px 0;
  font-size: 15px;
  color: #586474;
`

const Dd = styled.dd`
  margin: 0;
  padding: 10px 0;
  text-align: right;
  font-size: 16px;
  font-weight: 700;
  color: ${(props) => props.theme.color.text};
`

const CallButtonWrapper = styled.div`
  margin-top: 16px;
`

const MatchingState = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 22px 12px 10px;
  text-align: center;

  span {
    display: grid;
    place-items: center;
    width: 64px;
    height: 64px;
    margin-bottom: 16px;
    border-radius: 50%;
    background: ${(props) => props.theme.color.blue100};
    font-size: 32px;
  }

  strong {
    color: ${(props) => props.theme.color.text};
    font-size: 19px;
    font-weight: 800;
  }

  p {
    max-width: 300px;
    margin: 7px 0 0;
    color: ${(props) => props.theme.color.muted};
    font-size: 14px;
    line-height: 1.55;
  }
`

function regionPointLabel(point: { sido?: string; sigungu?: string } | undefined): string {
  if (!point?.sido) return '-'
  return point.sigungu ? `${point.sido} ${point.sigungu}` : point.sido
}

export function MatchedDriverCard({ driver, fareVerdict, cargoStatus }: MatchedDriverCardProps) {
  if (!driver) {
    return (
      <Card>
        <Title>기사님 정보</Title>
        <MatchingState role="status">
          <span aria-hidden="true">🚚</span>
          <strong>조건에 맞는 기사님을 찾고 있어요</strong>
          <p>배차가 확정되면 기사님 정보가 여기에 표시됩니다.</p>
        </MatchingState>
      </Card>
    )
  }

  const originLabel = regionPointLabel(driver.routePreferences?.origins?.[0])
  const destinationLabel = regionPointLabel(driver.routePreferences?.destinations?.[0])

  return (
    <Card>
      <Title>{cargoStatus === 'COMPLETED' ? '운송 기사님 정보' : '확정된 기사님 정보'}</Title>
      <DriverRow>
        <DriverLeft>
          <Avatar aria-hidden="true" />
          {driver.name} 기사 · {driver.bodyType} {driver.capacityTon}톤
        </DriverLeft>
        <Badge tone={cargoStatus === 'COMPLETED' ? 'neutral' : 'ok'}>
          {cargoStatus === 'COMPLETED'
            ? '운송 완료'
            : fareVerdict === 'LOW'
              ? '운임 낮음'
              : fareVerdict === 'FAIR'
                ? '적정 운임'
                : '시세 정보 없음'}
        </Badge>
      </DriverRow>

      <DataGrid>
        <Dt>차량번호</Dt>
        <Dd>{driver.plateNumber ?? '-'}</Dd>
        <Dt>운행지역</Dt>
        <Dd>
          {originLabel} → {destinationLabel}
        </Dd>
        <Dt>평점</Dt>
        <Dd>{driver.rating ?? '-'}점</Dd>
        <Dt>완료율</Dt>
        <Dd>{driver.completionRate ?? '-'}%</Dd>
        <Dt>총 운행</Dt>
        <Dd>{driver.totalTrips ?? '-'}건</Dd>
        <Dt>희망 최소 단가</Dt>
        <Dd>{formatFare(driver.minAcceptFare)}</Dd>
      </DataGrid>

      <CallButtonWrapper>
        <Button type="button" fullWidth disabled title="데모 데이터에 연락처 정보가 없어요">
          전화하기
        </Button>
      </CallButtonWrapper>
    </Card>
  )
}
