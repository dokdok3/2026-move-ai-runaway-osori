import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter } from 'react-router-dom'
import { Global, ThemeProvider, css } from '@emotion/react'
import './index.css'
import { App } from './App.tsx'
import { theme } from './theme/theme.ts'

const queryClient = new QueryClient()

const globalStyles = css`
  body {
    background: ${theme.color.bg};
    color: ${theme.color.text};
    font-family: ${theme.font.family};
  }
`

const renderApp = () => {
  createRoot(document.getElementById('root')!).render(
    <StrictMode>
      <QueryClientProvider client={queryClient}>
        <ThemeProvider theme={theme}>
          <Global styles={globalStyles} />
          <BrowserRouter>
            <App />
          </BrowserRouter>
        </ThemeProvider>
      </QueryClientProvider>
    </StrictMode>,
  )
}

const enableMocking = async () => {
  if (import.meta.env.VITE_API_MOCKING !== 'enabled') return

  const { worker } = await import('./mocks/browser')
  await worker.start({ onUnhandledRequest: 'bypass' })
}

enableMocking().then(renderApp)
