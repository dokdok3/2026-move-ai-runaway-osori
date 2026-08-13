import { afterEach, describe, expect, it, vi } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '@/test/render'
import { DriverPage } from './DriverPage'

function actionButtons(name: '수락' | '숨기기'): HTMLButtonElement[] {
  return screen
    .getAllByRole<HTMLButtonElement>('button', { name })
    .filter((button) => !button.hasAttribute('aria-pressed'))
}

async function waitForActionButtons(name: '수락' | '숨기기'): Promise<HTMLButtonElement[]> {
  await waitFor(() => expect(actionButtons(name).length).toBeGreaterThan(0))
  return actionButtons(name)
}

describe('DriverPage', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('내 기사 프로필로 활동 지역을 채우고 추천 화물 목록을 보여준다', async () => {
    renderWithProviders(<DriverPage />)

    await waitFor(() => {
      expect(screen.getByLabelText<HTMLSelectElement>('출발지 시도').value).toBe('서울특별시')
    })

    const acceptButtons = await waitForActionButtons('수락')
    expect(acceptButtons.length).toBeGreaterThan(0)
  })

  it('숨기기를 누르면 해당 화물 카드가 목록에서 사라진다', async () => {
    const user = userEvent.setup()
    renderWithProviders(<DriverPage />)

    const hideButtons = await waitForActionButtons('숨기기')
    const initialCount = hideButtons.length

    await user.click(hideButtons[0])

    await waitFor(() => {
      expect(actionButtons('숨기기')).toHaveLength(initialCount - 1)
    })
  })

  it('수락을 누르면 수락 요청이 처리되고 추천 목록에서 제거된다', async () => {
    const user = userEvent.setup()
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true)
    renderWithProviders(<DriverPage />)

    const [firstAcceptButton] = await waitForActionButtons('수락')
    await user.click(firstAcceptButton)

    expect(confirmSpy).toHaveBeenCalledWith(expect.stringContaining('화물을 수락하시겠어요?'))

    await waitFor(() => expect(firstAcceptButton).not.toBeInTheDocument())
  })

  it('수락 확인창에서 취소하면 수락 요청을 보내지 않는다', async () => {
    const user = userEvent.setup()
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(false)
    renderWithProviders(<DriverPage />)

    const [firstAcceptButton] = await waitForActionButtons('수락')
    await user.click(firstAcceptButton)

    expect(confirmSpy).toHaveBeenCalledOnce()
    expect(firstAcceptButton).toHaveTextContent('수락')
    expect(firstAcceptButton).not.toBeDisabled()
  })

  it('필터 배지를 누르면 쿼리 파라미터를 변경하고 해당 목록을 보여준다', async () => {
    const user = userEvent.setup()
    renderWithProviders(<DriverPage />)

    const [firstHideButton] = await waitForActionButtons('숨기기')
    await user.click(firstHideButton)
    await user.click(screen.getByRole('button', { name: '숨기기', pressed: false }))

    expect(screen.getByRole('button', { name: '숨기기', pressed: true })).toBeInTheDocument()
    expect(actionButtons('숨기기').length).toBeGreaterThan(0)
  })
})
