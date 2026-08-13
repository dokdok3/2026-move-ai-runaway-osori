import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ThemeProvider } from '@emotion/react'
import { describe, expect, it } from 'vitest'
import { theme } from '@/theme/theme'
import { Hero } from './Hero'

function renderHero() {
  return render(
    <ThemeProvider theme={theme}>
      <Hero eyebrow="✦ 테스트 배너" title="제목" description="설명" />
    </ThemeProvider>,
  )
}

describe('Hero', () => {
  it('닫기 버튼을 누르면 사라지고, 다시 마운트해도 노출되지 않는다', async () => {
    const { unmount } = renderHero()

    await userEvent.click(screen.getByRole('button', { name: '배너 닫기' }))
    expect(screen.queryByText('제목')).not.toBeInTheDocument()

    unmount()
    renderHero()
    expect(screen.queryByText('제목')).not.toBeInTheDocument()
  })
})
