import { describe, expect, it } from 'vitest'
import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderWithProviders } from '@/test/render'
import { ShipperPage } from './ShipperPage'

describe('ShipperPage', () => {
  it('원문을 붙여넣고 변환하면 화물 요약과 매칭 상태를 보여준다', async () => {
    const user = userEvent.setup()
    renderWithProviders(<ShipperPage />)

    const textarea = screen.getByLabelText('화물 요청 원문')
    await user.type(textarea, '내일 서울에서 부산으로 냉장 5톤 50만원에 보내주세요')
    await user.click(screen.getByRole('button', { name: 'AI 자동 변환' }))

    const summaryHeading = await screen.findByText('화물 정보')
    const summaryCard = summaryHeading.closest('div')?.parentElement as HTMLElement
    expect(within(summaryCard).getByText('서울특별시')).toBeInTheDocument()
    expect(within(summaryCard).getByText('부산광역시')).toBeInTheDocument()
    expect(within(summaryCard).getByText(/냉장 · 5톤/)).toBeInTheDocument()
    expect(within(summaryCard).getByText('500,000원')).toBeInTheDocument()

    await waitFor(() => {
      expect(
        screen.getByText('아직 매칭된 기사가 없어요. 잠시 후 다시 확인해 주세요.'),
      ).toBeInTheDocument()
    })
  })

  it('빈 문장을 제출하면 API를 호출하지 않고 안내 문구를 보여준다', async () => {
    const user = userEvent.setup()
    renderWithProviders(<ShipperPage />)

    await user.click(screen.getByRole('button', { name: 'AI 자동 변환' }))

    expect(await screen.findByText('화물 요청 내용을 입력해 주세요.')).toBeInTheDocument()
    expect(screen.queryByText('화물 정보')).not.toBeInTheDocument()
  })

  it('출발지·도착지를 인식하지 못하면 오류 문구를 보여주고 요약을 만들지 않는다', async () => {
    const user = userEvent.setup()
    renderWithProviders(<ShipperPage />)

    const textarea = screen.getByLabelText('화물 요청 원문')
    await user.type(textarea, '냉장 화물 옮겨주세요')
    await user.click(screen.getByRole('button', { name: 'AI 자동 변환' }))

    expect(
      await screen.findByText(
        '원문에서 출발지·도착지·톤수를 인식하지 못했어요. 조금 더 구체적으로 적어주세요.',
      ),
    ).toBeInTheDocument()
    expect(screen.queryByText('화물 정보')).not.toBeInTheDocument()
  })
})
