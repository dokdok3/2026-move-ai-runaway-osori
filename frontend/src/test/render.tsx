/* eslint-disable react-refresh/only-export-components -- 테스트 전용 커스텀 render 헬퍼라 fast refresh 대상이 아니다 */
import type { ReactElement, ReactNode } from 'react'
import { render } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { ThemeProvider } from '@emotion/react'
import { theme } from '@/theme/theme'

interface RenderWithProvidersOptions {
  route?: string
}

function createTestQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  })
}

function AllProviders({ route = '/', children }: { route?: string; children: ReactNode }) {
  const queryClient = createTestQueryClient()

  return (
    <QueryClientProvider client={queryClient}>
      <ThemeProvider theme={theme}>
        <MemoryRouter initialEntries={[route]}>{children}</MemoryRouter>
      </ThemeProvider>
    </QueryClientProvider>
  )
}

export function renderWithProviders(ui: ReactElement, options: RenderWithProvidersOptions = {}) {
  return render(ui, {
    wrapper: ({ children }) => <AllProviders route={options.route}>{children}</AllProviders>,
  })
}

export * from '@testing-library/react'
