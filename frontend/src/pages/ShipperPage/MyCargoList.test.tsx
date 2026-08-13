import { expect, it, vi } from 'vitest'
import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'

import { renderWithProviders } from '@/test/render'

import { MyCargoList } from './MyCargoList'

it('페이지 인디케이터에서 다음 페이지를 요청한다', async () => {
  const user = userEvent.setup()
  const onPageChange = vi.fn()

  renderWithProviders(
    <MyCargoList
      cargos={[
        {
          cargoId: 1,
          status: 'PENDING',
          origin: { sido: '서울특별시' },
          destination: { sido: '부산광역시' },
          desiredFare: 500000,
        },
      ]}
      loading={false}
      error={false}
      onCreate={vi.fn()}
      page={0}
      totalPages={3}
      onPageChange={onPageChange}
      onSelect={vi.fn()}
    />,
  )

  expect(screen.getByRole('button', { name: '1페이지' })).toHaveAttribute('aria-current', 'page')
  await user.click(screen.getByRole('button', { name: '다음' }))
  expect(onPageChange).toHaveBeenCalledWith(1)
})
