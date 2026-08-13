import styled from '@emotion/styled'
import { Card } from '@/components/Card'
import type { LoadFilter, LoadResponse } from '@/api/load/model'
import { LoadOfferCard } from './LoadOfferCard'

export interface LoadOfferListProps {
  loads: LoadResponse[]
  onRefresh: () => void
  onAccept: (cargoId: number) => Promise<boolean>
  onHide: (cargoId: number) => void
  acceptingCargoId: number | undefined
  filter: LoadFilter
  onFilterChange: (filter: LoadFilter) => void
  hasNextPage: boolean
  loadingMore: boolean
  loadMoreRef: (node: HTMLDivElement | null) => void
}

const TitleRow = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
`

const Title = styled.div`
  font-size: 19px;
  font-weight: 800;
  color: ${(props) => props.theme.color.navy900};
`

const FilterList = styled.div`
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
`

const FilterBadge = styled.button<{ active: boolean }>`
  min-height: 44px;
  padding: 10px 16px;
  border: 2px solid
    ${(props) => (props.active ? props.theme.color.navy700 : props.theme.color.borderStrong)};
  border-radius: 999px;
  background: ${(props) => (props.active ? props.theme.color.navy700 : props.theme.color.surface)};
  color: ${(props) => (props.active ? props.theme.color.white : props.theme.color.muted)};
  font-size: 15px;
  font-weight: 750;
  cursor: pointer;
  touch-action: manipulation;

  &:focus-visible {
    outline: 3px solid ${(props) => props.theme.color.focus};
    outline-offset: 2px;
  }

  @media (hover: hover) {
    &:hover {
      border-color: ${(props) => props.theme.color.navy700};
    }
  }
`

const RefreshButton = styled.button`
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border: 2px solid ${(props) => props.theme.color.borderStrong};
  border-radius: ${(props) => props.theme.radius.md};
  background: ${(props) => props.theme.color.surface};
  color: ${(props) => props.theme.color.navy700};
  cursor: pointer;

  &:focus-visible {
    outline: 3px solid ${(props) => props.theme.color.focus};
    outline-offset: 2px;
  }

  @media (hover: hover) {
    &:hover {
      border-color: ${(props) => props.theme.color.mutedLight};
    }
  }

  svg {
    width: 22px;
    height: 22px;
  }
`

const EmptyState = styled.p`
  margin: 0;
  padding: 8px 0;
  color: ${(props) => props.theme.color.muted};
  font-size: 15px;
  line-height: 1.6;
  text-align: center;
`

const LoadMoreStatus = styled.div`
  min-height: 48px;
  padding: 14px 0 4px;
  color: ${(props) => props.theme.color.muted};
  font-size: 14px;
  text-align: center;
`

export function LoadOfferList({
  loads,
  onRefresh,
  onAccept,
  onHide,
  acceptingCargoId,
  filter,
  onFilterChange,
  hasNextPage,
  loadingMore,
  loadMoreRef,
}: LoadOfferListProps) {
  const filters: Array<{ value: LoadFilter; label: string }> = [
    { value: 'ALL', label: '전체' },
    { value: 'ACCEPTED', label: '수락' },
    { value: 'HIDDEN', label: '숨기기' },
  ]

  return (
    <Card>
      <TitleRow>
        <Title>추천 화물</Title>
        <RefreshButton type="button" onClick={onRefresh} aria-label="새로고침">
          <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2.2"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <path d="M3 12a9 9 0 0 1 15.3-6.4L21 8" />
            <path d="M21 3v5h-5" />
            <path d="M21 12a9 9 0 0 1-15.3 6.4L3 16" />
            <path d="M3 21v-5h5" />
          </svg>
        </RefreshButton>
      </TitleRow>

      <FilterList aria-label="추천 화물 필터">
        {filters.map(({ value, label }) => (
          <FilterBadge
            key={value}
            type="button"
            active={filter === value}
            aria-pressed={filter === value}
            onClick={() => onFilterChange(value)}
          >
            {label}
          </FilterBadge>
        ))}
      </FilterList>

      {loads.length === 0 ? (
        <EmptyState>
          {filter === 'ALL'
            ? '조건에 맞는 화물이 아직 없어요. 활동 지역을 조정해 보세요.'
            : filter === 'ACCEPTED'
              ? '수락한 화물이 아직 없어요.'
              : '숨긴 화물이 아직 없어요.'}
        </EmptyState>
      ) : (
        <>
          {loads.map((load, index) => (
            <LoadOfferCard
              key={load.cargoId}
              load={load}
              isBest={index === 0}
              onAccept={() =>
                load.cargoId !== undefined ? onAccept(load.cargoId) : Promise.resolve(false)
              }
              onHide={() => load.cargoId !== undefined && onHide(load.cargoId)}
              accepting={acceptingCargoId === load.cargoId}
            />
          ))}
          <LoadMoreStatus ref={loadMoreRef} aria-live="polite">
            {loadingMore
              ? '화물을 더 불러오는 중이에요.'
              : hasNextPage
                ? ''
                : '모든 화물을 봤어요.'}
          </LoadMoreStatus>
        </>
      )}
    </Card>
  )
}
