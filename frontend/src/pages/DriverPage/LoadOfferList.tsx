import styled from '@emotion/styled'
import { Card } from '@/components/Card'
import type { LoadResponse } from '@/api/load/model'
import { LoadOfferCard } from './LoadOfferCard'

export interface LoadOfferListProps {
  loads: LoadResponse[]
  onRefresh: () => void
  onAccept: (cargoId: number) => void
  onHide: (cargoId: number) => void
  acceptingCargoId: number | undefined
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

export function LoadOfferList({
  loads,
  onRefresh,
  onAccept,
  onHide,
  acceptingCargoId,
}: LoadOfferListProps) {
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

      {loads.length === 0 ? (
        <EmptyState>조건에 맞는 화물이 아직 없어요. 활동 지역을 조정해 보세요.</EmptyState>
      ) : (
        loads.map((load, index) => (
          <LoadOfferCard
            key={load.cargoId}
            load={load}
            isBest={index === 0}
            onAccept={() => load.cargoId !== undefined && onAccept(load.cargoId)}
            onHide={() => load.cargoId !== undefined && onHide(load.cargoId)}
            accepting={acceptingCargoId === load.cargoId}
          />
        ))
      )}
    </Card>
  )
}
