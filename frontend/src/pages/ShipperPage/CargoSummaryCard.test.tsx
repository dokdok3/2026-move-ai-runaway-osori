import { describe, expect, it } from 'vitest'
import { screen } from '@testing-library/react'
import { renderWithProviders } from '@/test/render'
import type { CargoDetailResponse } from '@/api/cargo/model'
import { CargoSummaryCard } from './CargoSummaryCard'

const cargo: CargoDetailResponse = {
  cargoId: 1,
  status: 'REQUESTED',
  origin: { sido: '서울특별시', sigungu: '송파구' },
  destination: { sido: '부산광역시', sigungu: '강서구' },
  cargoType: 'GENERAL',
  weightTon: 5,
  desiredFare: 500000,
  loadingAt: '2026-08-14T09:00:00',
  unloadingAt: '2026-08-14T18:00:00',
}

const renderCard = (aiParsed: boolean | undefined) =>
  renderWithProviders(
    <CargoSummaryCard
      cargo={{ ...cargo, aiParsed }}
      onEdit={() => undefined}
      onRematch={() => undefined}
      rematchLoading={false}
    />,
  )

describe('CargoSummaryCard', () => {
  it('AI 자동 변환으로 등록한 화물은 AI 변환 배지를 보여준다', () => {
    renderCard(true)

    expect(screen.getByText('AI 변환')).toBeInTheDocument()
  })

  it('직접 등록한 화물은 AI 변환 배지를 보여주지 않는다', () => {
    renderCard(false)

    expect(screen.queryByText('AI 변환')).not.toBeInTheDocument()
  })

  it('aiParsed 값이 없는 화물도 AI 변환 배지를 보여주지 않는다', () => {
    renderCard(undefined)

    expect(screen.queryByText('AI 변환')).not.toBeInTheDocument()
  })
})
