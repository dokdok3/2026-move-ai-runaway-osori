import { useState } from 'react'
import styled from '@emotion/styled'

import { Badge } from '@/components/Badge'
import { Button } from '@/components/Button'
import type { ShipperCargoResponse } from '@/api/cargo/model'
import { getCargoTypeLabel } from '@/utils/cargoType'
import { formatFare, formatShortDateTime } from '@/utils/format'

export interface MyCargoListProps {
  cargos: ShipperCargoResponse[]
  loading: boolean
  error: boolean
  onCreate: () => void
  page: number
  totalPages: number
  onPageChange: (page: number) => void
  onSelect: (cargoId: number) => void
}

type CargoFilter = 'ALL' | 'ACTIVE' | 'DONE'

const Tabs = styled.nav`
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
`

const Tab = styled.button<{ active: boolean }>`
  flex: 1;
  min-height: 52px;
  padding: 12px 10px;
  border: 2px solid
    ${(props) => (props.active ? props.theme.color.navy700 : props.theme.color.borderStrong)};
  border-radius: ${(props) => props.theme.radius.md};
  background: ${(props) => (props.active ? props.theme.color.navy700 : props.theme.color.surface)};
  color: ${(props) => (props.active ? props.theme.color.white : '#354052')};
  font-size: 17px;
  font-weight: 750;
  cursor: pointer;

  &:focus-visible {
    outline: 3px solid ${(props) => props.theme.color.focus};
    outline-offset: 2px;
  }
`

const Head = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: 4px 0 12px;
`

const Title = styled.h1`
  margin: 0;
  color: ${(props) => props.theme.color.text};
  font-size: 19px;
  font-weight: 800;
`

const CreateButton = styled(Button)`
  min-height: 44px;
  padding: 10px 14px;
  border-color: ${(props) => props.theme.color.navy300};
  background: ${(props) => props.theme.color.blue100};
  font-size: 15px;
`

const List = styled.ul`
  display: grid;
  gap: 10px;
  margin: 0;
  padding: 0;
  list-style: none;
`

const Item = styled.button`
  width: 100%;
  padding: 16px;
  border: 1px solid ${(props) => props.theme.color.border};
  border-radius: ${(props) => props.theme.radius.md};
  background: ${(props) => props.theme.color.surface};
  color: inherit;
  font-family: inherit;
  text-align: left;
  cursor: pointer;
  touch-action: manipulation;

  &:focus-visible {
    outline: 3px solid ${(props) => props.theme.color.focus};
    outline-offset: 2px;
  }

  @media (hover: hover) {
    &:hover {
      border-color: ${(props) => props.theme.color.mutedLight};
    }
  }
`

const ItemHead = styled.div`
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
`

const DateText = styled.span`
  margin-left: auto;
  color: ${(props) => props.theme.color.mutedLight};
  font-size: 14px;
  white-space: nowrap;
`

const Route = styled.div`
  color: ${(props) => props.theme.color.text};
  font-size: 20px;
  font-weight: 800;
  line-height: 1.3;
  letter-spacing: -0.02em;

  span {
    margin: 0 4px;
    color: ${(props) => props.theme.color.navy500};
  }
`

const Bottom = styled.div`
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
  margin-top: 8px;
`

const Meta = styled.span`
  color: ${(props) => props.theme.color.mutedLight};
  font-size: 14px;
  line-height: 1.5;
`

const Fare = styled.strong`
  color: ${(props) => props.theme.color.text};
  font-size: 20px;
  font-weight: 850;
  letter-spacing: -0.02em;
  white-space: nowrap;
`

const Message = styled.p`
  margin: 0;
  padding: 28px 16px;
  border: 1px solid ${(props) => props.theme.color.border};
  border-radius: ${(props) => props.theme.radius.md};
  background: ${(props) => props.theme.color.surface};
  color: ${(props) => props.theme.color.muted};
  font-size: 15px;
  line-height: 1.6;
  text-align: center;
`

const Pagination = styled.nav`
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 16px;
`

const PageButton = styled.button<{ active?: boolean }>`
  min-width: 48px;
  min-height: 48px;
  padding: 8px;
  border: 2px solid
    ${(props) => (props.active ? props.theme.color.navy700 : props.theme.color.borderStrong)};
  border-radius: ${(props) => props.theme.radius.md};
  background: ${(props) => (props.active ? props.theme.color.navy700 : props.theme.color.surface)};
  color: ${(props) => (props.active ? props.theme.color.white : props.theme.color.navy700)};
  font-size: 16px;
  font-weight: 750;
  cursor: pointer;

  &:disabled {
    opacity: 0.4;
    cursor: not-allowed;
  }

  &:focus-visible {
    outline: 3px solid ${(props) => props.theme.color.focus};
    outline-offset: 2px;
  }
`

function placeName(point: ShipperCargoResponse['origin']): string {
  if (!point?.sido) return '-'
  return point.sigungu && point.sigungu !== '전체' ? `${point.sido} ${point.sigungu}` : point.sido
}

function isDone(status: string | undefined): boolean {
  return status === 'COMPLETED' || status === 'DELIVERED'
}

function statusLabel(status: string | undefined): string {
  if (isDone(status)) return '운송 완료'
  if (status === 'MATCHED') return '배차 확정'
  if (status === 'CANCELLED') return '취소'
  return '기사 찾는 중'
}

function registeredDate(createdAt: string | undefined): string {
  if (!createdAt) return ''
  return `${formatShortDateTime(createdAt).slice(0, 5)} 등록`
}

export function MyCargoList({
  cargos,
  loading,
  error,
  onCreate,
  page,
  totalPages,
  onPageChange,
  onSelect,
}: MyCargoListProps) {
  const [filter, setFilter] = useState<CargoFilter>('ALL')
  const doneCount = cargos.filter((cargo) => isDone(cargo.status)).length
  const activeCount = cargos.length - doneCount
  const visibleCargos = cargos.filter((cargo) => {
    if (filter === 'ACTIVE') return !isDone(cargo.status)
    if (filter === 'DONE') return isDone(cargo.status)
    return true
  })

  return (
    <>
      <Tabs aria-label="내 화물 상태 필터">
        <Tab type="button" active={filter === 'ALL'} onClick={() => setFilter('ALL')}>
          전체 {cargos.length}
        </Tab>
        <Tab type="button" active={filter === 'ACTIVE'} onClick={() => setFilter('ACTIVE')}>
          진행 중 {activeCount}
        </Tab>
        <Tab type="button" active={filter === 'DONE'} onClick={() => setFilter('DONE')}>
          완료 {doneCount}
        </Tab>
      </Tabs>

      <Head>
        <Title>내가 등록한 화물</Title>
        <CreateButton type="button" variant="ghost" onClick={onCreate}>
          ＋ 새 화물
        </CreateButton>
      </Head>

      {loading ? (
        <Message>화물 목록을 불러오는 중이에요.</Message>
      ) : error ? (
        <Message role="alert">내 화물 목록을 불러오지 못했어요.</Message>
      ) : visibleCargos.length === 0 ? (
        <Message>{filter === 'ALL' ? '등록한 화물이 아직 없어요.' : '해당 화물이 없어요.'}</Message>
      ) : (
        <>
          <List>
            {visibleCargos.map((cargo) => (
              <li key={cargo.cargoId}>
                <Item
                  type="button"
                  aria-label={`${placeName(cargo.origin)}에서 ${placeName(cargo.destination)} 화물 상세 보기`}
                  onClick={() => cargo.cargoId !== undefined && onSelect(cargo.cargoId)}
                >
                  <ItemHead>
                    <Badge tone={cargo.status === 'MATCHED' ? 'ok' : 'neutral'}>
                      {statusLabel(cargo.status)}
                    </Badge>
                    <DateText>{registeredDate(cargo.createdAt)}</DateText>
                  </ItemHead>
                  <Route>
                    {placeName(cargo.origin)} <span aria-hidden="true">→</span>{' '}
                    {placeName(cargo.destination)}
                  </Route>
                  <Bottom>
                    <Meta>
                      {formatShortDateTime(cargo.loadingAt)} 상차 ·{' '}
                      {getCargoTypeLabel(cargo.cargoType)} {cargo.weightTon ?? '-'}톤
                    </Meta>
                    <Fare>{formatFare(cargo.desiredFare)}</Fare>
                  </Bottom>
                </Item>
              </li>
            ))}
          </List>
          {totalPages > 1 && (
            <Pagination aria-label="내 화물 페이지">
              <PageButton
                type="button"
                disabled={page === 0}
                onClick={() => onPageChange(page - 1)}
              >
                이전
              </PageButton>
              {Array.from({ length: totalPages }, (_, index) => (
                <PageButton
                  key={index}
                  type="button"
                  active={page === index}
                  aria-current={page === index ? 'page' : undefined}
                  aria-label={`${index + 1}페이지`}
                  onClick={() => onPageChange(index)}
                >
                  {index + 1}
                </PageButton>
              ))}
              <PageButton
                type="button"
                disabled={page >= totalPages - 1}
                onClick={() => onPageChange(page + 1)}
              >
                다음
              </PageButton>
            </Pagination>
          )}
        </>
      )}
    </>
  )
}
