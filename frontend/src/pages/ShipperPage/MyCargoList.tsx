import styled from '@emotion/styled'

import { Badge } from '@/components/Badge'
import { Card } from '@/components/Card'
import type { ShipperCargoResponse } from '@/api/cargo/model'
import { getCargoTypeLabel } from '@/utils/cargoType'
import { formatFare, formatShortDateTime } from '@/utils/format'

export interface MyCargoListProps {
  cargos: ShipperCargoResponse[]
  loading: boolean
  error: boolean
}

const Head = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
`

const Title = styled.h2`
  margin: 0;
  color: ${(props) => props.theme.color.text};
  font-size: 19px;
  font-weight: 800;
`

const Count = styled.span`
  color: ${(props) => props.theme.color.muted};
  font-size: 14px;
  font-weight: 700;
`

const List = styled.div`
  display: grid;
  gap: 10px;
`

const Item = styled.article`
  padding: 16px;
  border: 1px solid ${(props) => props.theme.color.border};
  border-radius: ${(props) => props.theme.radius.md};
`

const ItemHead = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
`

const Route = styled.strong`
  color: ${(props) => props.theme.color.text};
  font-size: 18px;
  line-height: 1.4;
`

const Meta = styled.div`
  margin-top: 9px;
  color: ${(props) => props.theme.color.muted};
  font-size: 14px;
  line-height: 1.5;
`

const Fare = styled.div`
  margin-top: 8px;
  color: ${(props) => props.theme.color.text};
  font-size: 20px;
  font-weight: 800;
  text-align: right;
`

const Message = styled.p`
  margin: 0;
  padding: 8px 0;
  color: ${(props) => props.theme.color.muted};
  font-size: 15px;
  line-height: 1.6;
  text-align: center;
`

function placeName(point: ShipperCargoResponse['origin']): string {
  if (!point?.sido) return '-'
  return point.sigungu && point.sigungu !== '전체' ? `${point.sido} ${point.sigungu}` : point.sido
}

function statusLabel(status: string | undefined): string {
  if (status === 'MATCHED') return '배차 완료'
  if (status === 'CANCELLED') return '취소'
  return '배차 대기'
}

export function MyCargoList({ cargos, loading, error }: MyCargoListProps) {
  return (
    <Card>
      <Head>
        <Title>내 화물</Title>
        <Count>총 {cargos.length}건</Count>
      </Head>

      {loading ? (
        <Message>화물 목록을 불러오는 중이에요.</Message>
      ) : error ? (
        <Message role="alert">내 화물 목록을 불러오지 못했어요.</Message>
      ) : cargos.length === 0 ? (
        <Message>등록한 화물이 아직 없어요.</Message>
      ) : (
        <List>
          {cargos.map((cargo) => (
            <Item key={cargo.cargoId}>
              <ItemHead>
                <Route>
                  {placeName(cargo.origin)} → {placeName(cargo.destination)}
                </Route>
                <Badge tone={cargo.status === 'MATCHED' ? 'ok' : 'neutral'}>
                  {statusLabel(cargo.status)}
                </Badge>
              </ItemHead>
              <Meta>
                {formatShortDateTime(cargo.loadingAt)} · {getCargoTypeLabel(cargo.cargoType)} ·{' '}
                {cargo.weightTon ?? '-'}톤
              </Meta>
              <Fare>{formatFare(cargo.desiredFare)}</Fare>
            </Item>
          ))}
        </List>
      )}
    </Card>
  )
}
