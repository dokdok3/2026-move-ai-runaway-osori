import { describe, expect, it } from 'vitest'
import { screen } from '@testing-library/react'
import { renderWithProviders } from '@/test/render'
import { RoleSelectPage } from './RoleSelectPage'

describe('RoleSelectPage', () => {
  it('역할 선택 문구와 화주/기사 링크를 보여준다', () => {
    renderWithProviders(<RoleSelectPage />)

    expect(screen.getByRole('heading', { name: '어떤 일로오셨나요?' })).toBeInTheDocument()

    const shipperLink = screen.getByRole('link', { name: /^화주/ })
    expect(shipperLink).toHaveAttribute('href', '/shipper')

    const driverLink = screen.getByRole('link', { name: /^기사/ })
    expect(driverLink).toHaveAttribute('href', '/driver')
  })
})
