import { afterEach, describe, expect, it, vi } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '@/test/render'
import { DriverPage } from './DriverPage'

describe('DriverPage', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('내 기사 프로필로 활동 지역을 채우고 추천 화물 목록을 보여준다', async () => {
    renderWithProviders(<DriverPage />)

    await waitFor(() => {
      expect(screen.getByLabelText<HTMLSelectElement>('출발지 시도').value).toBe('서울특별시')
    })

    const acceptButtons = await screen.findAllByRole('button', { name: '수락' })
    expect(acceptButtons.length).toBeGreaterThan(0)
  })

  it('숨기기를 누르면 해당 화물 카드가 목록에서 사라진다', async () => {
    const user = userEvent.setup()
    renderWithProviders(<DriverPage />)

    const hideButtons = await screen.findAllByRole('button', { name: '숨기기' })
    const initialCount = hideButtons.length

    await user.click(hideButtons[0])

    await waitFor(() => {
      expect(screen.getAllByRole('button', { name: '숨기기' })).toHaveLength(initialCount - 1)
    })
  })

  it('수락을 누르면 수락 요청이 처리되고 버튼이 다시 활성화된다', async () => {
    const user = userEvent.setup()
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true)
    renderWithProviders(<DriverPage />)

    const [firstAcceptButton] = await screen.findAllByRole('button', { name: '수락' })
    await user.click(firstAcceptButton)

    expect(confirmSpy).toHaveBeenCalledWith(expect.stringContaining('화물을 수락하시겠어요?'))

    await waitFor(() => {
      expect(firstAcceptButton).toHaveTextContent('수락')
      expect(firstAcceptButton).not.toBeDisabled()
    })
  })

  it('수락 확인창에서 취소하면 수락 요청을 보내지 않는다', async () => {
    const user = userEvent.setup()
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(false)
    renderWithProviders(<DriverPage />)

    const [firstAcceptButton] = await screen.findAllByRole('button', { name: '수락' })
    await user.click(firstAcceptButton)

    expect(confirmSpy).toHaveBeenCalledOnce()
    expect(firstAcceptButton).toHaveTextContent('수락')
    expect(firstAcceptButton).not.toBeDisabled()
  })
})
