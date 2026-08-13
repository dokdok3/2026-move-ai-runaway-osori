import { expect, it } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '@/test/render'
import { ShipperProfilePage } from './ShipperProfilePage'

it('화주 정보를 입력하고 저장한다', async () => {
  const user = userEvent.setup()
  renderWithProviders(<ShipperProfilePage />)

  const companyName = await screen.findByLabelText('회사명')
  await user.type(companyName, '오소리 물류')
  await user.type(screen.getByLabelText('담당자명'), '김화주')
  await user.type(screen.getByLabelText('연락처'), '010-1234-5678')
  await user.click(screen.getByRole('button', { name: '저장하기' }))

  await waitFor(() => {
    expect(screen.getByRole('status')).toHaveTextContent('화주 정보를 저장했어요.')
  })
})
