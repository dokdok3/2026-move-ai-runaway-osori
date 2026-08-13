import styled from '@emotion/styled'
import { Card } from '@/components/Card'
import { Badge } from '@/components/Badge'
import { Button } from '@/components/Button'
import { formatFare } from '@/utils/format'
import type { DriverResponse } from '@/api/driver/model'

export interface MatchedDriverCardProps {
  driver: DriverResponse | undefined
  fareVerdict: string | undefined
}

const Title = styled.div`
  margin-bottom: 14px;
  font-size: 14px;
  font-weight: 700;
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

const EmptyState = styled.p`
  margin: 0;
  padding: 8px 0;
  color: ${(props) => props.theme.color.muted};
  font-size: 15px;
  line-height: 1.6;
  text-align: center;
`

function regionPointLabel(point: { sido?: string; sigungu?: string } | undefined): string {
  if (!point?.sido) return '-'
  return point.sigungu ? `${point.sido} ${point.sigungu}` : point.sido
}

export function MatchedDriverCard({ driver, fareVerdict }: MatchedDriverCardProps) {
  if (!driver) {
    return (
      <Card>
        <Title>매칭 기사</Title>
        <EmptyState>아직 매칭된 기사가 없어요. 잠시 후 다시 확인해 주세요.</EmptyState>
      </Card>
    )
  }

  const originLabel = regionPointLabel(driver.routePreferences?.origins?.[0])
  const destinationLabel = regionPointLabel(driver.routePreferences?.destinations?.[0])

  return (
    <Card>
      <Title>매칭 기사</Title>
      <DriverRow>
        <DriverLeft>
          <Avatar aria-hidden="true" />
          {driver.name} 기사 · {driver.bodyType} {driver.capacityTon}톤
        </DriverLeft>
        <Badge tone={fareVerdict === 'LOW' ? 'warn' : 'ok'}>
          {fareVerdict === 'LOW' ? '운임 낮음' : '적정 운임'}
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
