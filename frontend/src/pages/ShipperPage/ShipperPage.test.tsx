import { describe, expect, it } from 'vitest'
import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { server } from '@/mocks/server'
import { renderWithProviders } from '@/test/render'
import { ShipperPage } from './ShipperPage'

describe('ShipperPage', () => {
  it('원문을 붙여넣고 변환하면 인식한 값이 채워진 등록 폼을 보여준다', async () => {
    const user = userEvent.setup()
    renderWithProviders(<ShipperPage />)

    await user.click(screen.getByRole('button', { name: /새 화물/ }))

    const textarea = screen.getByLabelText('화물 요청 원문')
    await user.type(textarea, '내일 서울에서 부산으로 냉장 5톤 50만원에 보내주세요')
    await user.click(screen.getByRole('button', { name: 'AI 자동 변환' }))

    expect(await screen.findByRole('heading', { name: '화물 정보 입력' })).toBeInTheDocument()
    expect(screen.getByLabelText('출발지 시도')).toHaveValue('서울특별시')
    expect(screen.getByLabelText('출발지 시군구')).toHaveValue('송파구')
    expect(screen.getByLabelText('도착지 시도')).toHaveValue('부산광역시')
    expect(screen.getByLabelText('도착지 시군구')).toHaveValue('강서구')
    expect(screen.getByLabelText(/중량/)).toHaveValue(5)
    expect(screen.getByLabelText(/운임/)).toHaveValue(500000)
    expect(screen.getByLabelText(/화물 종류/)).toHaveValue('REFRIGERATED')
  })

  it('채워진 등록 폼을 그대로 등록하면 화물 요약과 매칭 상태를 보여준다', async () => {
    const user = userEvent.setup()
    renderWithProviders(<ShipperPage />)

    await user.click(screen.getByRole('button', { name: /새 화물/ }))
    await user.type(
      screen.getByLabelText('화물 요청 원문'),
      '내일 서울에서 부산으로 냉장 5톤 50만원에 보내주세요',
    )
    await user.click(screen.getByRole('button', { name: 'AI 자동 변환' }))
    await user.click(await screen.findByRole('button', { name: '등록하기' }))
    await waitFor(() => {
      expect(screen.queryByLabelText('출발지 시도')).not.toBeInTheDocument()
    })

    const summaryHeading = await screen.findByText('화물 정보')
    const summaryCard = summaryHeading.closest('div')?.parentElement as HTMLElement
    expect(within(summaryCard).getByText('서울특별시 송파구')).toBeInTheDocument()
    expect(within(summaryCard).getByText('부산광역시 강서구')).toBeInTheDocument()
    expect(within(summaryCard).getByText(/냉장 · 5톤/)).toBeInTheDocument()
    expect(within(summaryCard).getByText('500,000원')).toBeInTheDocument()

    await waitFor(() => {
      expect(screen.getByText('조건에 맞는 기사님을 찾고 있어요')).toBeInTheDocument()
    })
  })

  it('직접 입력하기를 누르면 항목별 화물 등록 폼을 보여준다', async () => {
    const user = userEvent.setup()
    renderWithProviders(<ShipperPage />)

    await user.click(screen.getByRole('button', { name: /새 화물/ }))
    await user.click(screen.getByRole('button', { name: /직접 입력하기/ }))

    expect(screen.getByRole('heading', { name: '화물 정보 입력' })).toBeInTheDocument()
    expect(screen.getByLabelText('출발지 시도')).toBeInTheDocument()
    expect(screen.getByLabelText('출발지 시군구')).toBeDisabled()
    expect(screen.getByRole('button', { name: '등록하기' })).toBeInTheDocument()
  })

  it('화물 요청 입력란이 비어 있으면 자동 변환 버튼을 비활성화한다', async () => {
    const user = userEvent.setup()
    renderWithProviders(<ShipperPage />)

    await user.click(screen.getByRole('button', { name: /새 화물/ }))

    const textarea = screen.getByLabelText('화물 요청 원문')
    const convertButton = screen.getByRole('button', { name: 'AI 자동 변환' })

    expect(convertButton).toBeDisabled()

    await user.type(textarea, '   ')
    expect(convertButton).toBeDisabled()

    await user.type(textarea, '서울에서 부산으로 보내주세요')
    expect(convertButton).toBeEnabled()
  })

  it('인식하지 못한 항목은 에러 스타일로 표시하고 등록하기를 막는다', async () => {
    const user = userEvent.setup()
    renderWithProviders(<ShipperPage />)

    await user.click(screen.getByRole('button', { name: /새 화물/ }))

    const textarea = screen.getByLabelText('화물 요청 원문')
    await user.type(textarea, '냉장 화물 옮겨주세요')
    await user.click(screen.getByRole('button', { name: 'AI 자동 변환' }))

    expect(await screen.findByText(/원문에서 인식하지 못한 항목이 있어요/)).toBeInTheDocument()
    expect(screen.getByLabelText('출발지 시도')).toBeInvalid()
    expect(screen.getByLabelText('출발지 시군구')).toBeInvalid()
    expect(screen.getByLabelText('도착지 시도')).toBeInvalid()
    expect(screen.getByLabelText('도착지 시군구')).toBeInvalid()
    expect(screen.getByLabelText(/중량/)).toBeInvalid()
    expect(screen.getByLabelText(/운임/)).toBeInvalid()
    expect(screen.getByRole('button', { name: '등록하기' })).toBeDisabled()

    await user.selectOptions(screen.getByLabelText('출발지 시도'), '서울특별시')
    await user.selectOptions(screen.getByLabelText('출발지 시군구'), '송파구')
    expect(screen.getByLabelText('출발지 시도')).toBeValid()
    expect(screen.getByLabelText('출발지 시군구')).toBeValid()
    expect(screen.getByRole('button', { name: '등록하기' })).toBeDisabled()
  })

  it('AI 응답의 필수값이 누락되면 해당 입력을 오류 처리하고 등록을 막는다', async () => {
    server.use(
      http.post('/api/v1/cargos/parse', () =>
        HttpResponse.json({
          success: true,
          data: {
            origin: { sido: '충청북도', sigungu: '청주시', detail: null },
            destination: { sido: '부산광역시', sigungu: null, detail: null },
            cargoType: 'OTHER',
            cargoDescription: '내동 화물',
            weightTon: 5,
            offeredFareKrw: 500000,
            loadingDate: null,
            unloadingDate: null,
            missingFields: [
              '도착지 시군구는 필수입니다.',
              '상차일은 필수입니다.',
              '하차일은 필수입니다.',
            ],
          },
        }),
      ),
    )
    const user = userEvent.setup()
    renderWithProviders(<ShipperPage />)

    await user.click(screen.getByRole('button', { name: /새 화물/ }))
    await user.type(screen.getByLabelText('화물 요청 원문'), '내동 화물')
    await user.click(screen.getByRole('button', { name: 'AI 자동 변환' }))

    expect(await screen.findByLabelText('출발지 시도')).toHaveValue('충청북도')
    expect(screen.getByLabelText('출발지 시군구')).toHaveValue('청주시')
    expect(screen.getByLabelText('출발지 시도')).toBeValid()
    expect(screen.getByLabelText('출발지 시군구')).toBeValid()
    expect(screen.getByLabelText('도착지 시도')).toBeValid()
    expect(screen.getByLabelText('도착지 시군구')).toBeInvalid()
    expect(screen.getByLabelText('화물 종류')).toBeInvalid()
    expect(screen.getByLabelText('출발 일시')).toBeInvalid()
    expect(screen.getByLabelText('도착 일시')).toBeInvalid()
    expect(screen.getByLabelText('중량')).toBeValid()
    expect(screen.getByLabelText('운임')).toBeValid()
    expect(screen.getByRole('button', { name: '등록하기' })).toBeDisabled()
  })

  it('변환 API가 실패하면 빈 등록 폼으로 넘겨 직접 입력하도록 안내한다', async () => {
    server.use(
      http.post('/api/v1/cargos/parse', () =>
        HttpResponse.json({ success: false, message: 'AI 변환 서버 오류입니다.' }, { status: 500 }),
      ),
    )
    const user = userEvent.setup()
    renderWithProviders(<ShipperPage />)

    await user.click(screen.getByRole('button', { name: /새 화물/ }))
    await user.type(screen.getByLabelText('화물 요청 원문'), '내일 서울에서 부산으로 5톤')
    await user.click(screen.getByRole('button', { name: 'AI 자동 변환' }))

    expect(await screen.findByRole('heading', { name: '화물 정보 입력' })).toBeInTheDocument()
    expect(screen.getByText(/AI 변환 서버 오류입니다\. 직접 입력해주세요\./)).toBeInTheDocument()
    expect(screen.getByLabelText('출발지 시도')).toBeInvalid()
    expect(screen.getByLabelText('출발지 시군구')).toBeInvalid()
    expect(screen.getByLabelText('도착지 시도')).toBeInvalid()
    expect(screen.getByLabelText('도착지 시군구')).toBeInvalid()
    expect(screen.getByLabelText(/중량/)).toBeInvalid()
    expect(screen.getByLabelText(/운임/)).toBeInvalid()
    expect(screen.getByRole('button', { name: '등록하기' })).toBeDisabled()
  })

  it('등록 폼에서 취소하면 이전 원문과 변환 결과를 비운다', async () => {
    const user = userEvent.setup()
    renderWithProviders(<ShipperPage />)

    await user.click(screen.getByRole('button', { name: /새 화물/ }))
    await user.type(
      screen.getByLabelText('화물 요청 원문'),
      '내일 서울에서 부산으로 냉장 5톤 50만원에 보내주세요',
    )
    await user.click(screen.getByRole('button', { name: 'AI 자동 변환' }))
    await user.click(await screen.findByRole('button', { name: '취소' }))

    expect(screen.getByLabelText('화물 요청 원문')).toHaveValue('')
    expect(screen.getByRole('button', { name: 'AI 자동 변환' })).toBeDisabled()

    await user.click(screen.getByRole('button', { name: /직접 입력하기/ }))
    expect(screen.getByLabelText('출발지 시도')).toHaveValue('')
  })

  it('모든 항목이 자동 변환되면 등록하기가 활성화된다', async () => {
    const user = userEvent.setup()
    renderWithProviders(<ShipperPage />)

    await user.click(screen.getByRole('button', { name: /새 화물/ }))
    await user.type(
      screen.getByLabelText('화물 요청 원문'),
      '내일 서울에서 부산으로 냉장 5톤 50만원에 보내주세요',
    )
    await user.click(screen.getByRole('button', { name: 'AI 자동 변환' }))

    expect(await screen.findByRole('button', { name: '등록하기' })).toBeEnabled()
    expect(screen.getByLabelText('출발지 시도')).toBeValid()
    expect(screen.getByLabelText('출발지 시군구')).toBeValid()
  })
})
