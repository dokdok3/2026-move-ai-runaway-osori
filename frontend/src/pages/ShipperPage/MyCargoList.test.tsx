import { expect, it, vi } from 'vitest'
import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'

import { renderWithProviders } from '@/test/render'

import { MyCargoList } from './MyCargoList'

function cargoFixtures(count: number) {
  return Array.from({ length: count }, (_, index) => ({
    cargoId: index + 1,
    status: 'PENDING',
    origin: { sido: '서울특별시' },
    destination: { sido: '부산광역시' },
    desiredFare: 500000,
  }))
}

function renderList(cargoCount: number, page: number, onPageChange = vi.fn()) {
  renderWithProviders(
    <MyCargoList
      cargos={cargoFixtures(cargoCount)}
      loading={false}
      error={false}
      onCreate={vi.fn()}
      page={page}
      onPageChange={onPageChange}
      onSelect={vi.fn()}
    />,
  )
}

it('페이지 인디케이터에서 다음 페이지를 요청한다', async () => {
  const user = userEvent.setup()
  const onPageChange = vi.fn()

  renderList(11, 0, onPageChange)

  expect(screen.getByRole('button', { name: '1페이지' })).toHaveAttribute('aria-current', 'page')
  await user.click(screen.getByRole('button', { name: '다음' }))
  expect(onPageChange).toHaveBeenCalledWith(1)
})

/** 이전/다음을 뺀 페이지 칸을 왼쪽부터 읽는다. 생략 기호는 '…'로 들어온다. */
function pageSlotLabels() {
  return screen
    .getAllByRole('navigation', { name: '내 화물 페이지' })[0]
    .querySelectorAll('button[aria-label], span')
}

it('가운데 페이지에서는 1 … 4 5 6 … 10 으로 7칸을 채운다', () => {
  // 화물 100건 = 10페이지, 현재 5페이지(index 4).
  renderList(100, 4)

  expect(screen.getAllByRole('button', { name: /페이지$/ })).toHaveLength(5)
  expect(pageSlotLabels()).toHaveLength(7)
  for (const label of ['1페이지', '4페이지', '5페이지', '6페이지', '10페이지']) {
    expect(screen.getByRole('button', { name: label })).toBeInTheDocument()
  }
  expect(screen.queryByRole('button', { name: '3페이지' })).not.toBeInTheDocument()
  expect(screen.queryByRole('button', { name: '7페이지' })).not.toBeInTheDocument()
})

it('첫 페이지에서는 1~5 … 10 으로 7칸을 채운다', () => {
  renderList(100, 0)

  expect(screen.getAllByRole('button', { name: /페이지$/ })).toHaveLength(6)
  expect(pageSlotLabels()).toHaveLength(7)
  expect(screen.getByRole('button', { name: '5페이지' })).toBeInTheDocument()
  expect(screen.getByRole('button', { name: '10페이지' })).toBeInTheDocument()
  expect(screen.queryByRole('button', { name: '6페이지' })).not.toBeInTheDocument()
})

it('마지막 페이지에서는 1 … 6~10 으로 7칸을 채운다', () => {
  renderList(100, 9)

  expect(screen.getAllByRole('button', { name: /페이지$/ })).toHaveLength(6)
  expect(pageSlotLabels()).toHaveLength(7)
  expect(screen.getByRole('button', { name: '1페이지' })).toBeInTheDocument()
  expect(screen.getByRole('button', { name: '6페이지' })).toBeInTheDocument()
  expect(screen.queryByRole('button', { name: '5페이지' })).not.toBeInTheDocument()
})

it('페이지가 7개 이하면 전부 보여준다', () => {
  renderList(50, 0)

  expect(screen.getAllByRole('button', { name: /페이지$/ })).toHaveLength(5)
  expect(pageSlotLabels()).toHaveLength(5)
})
