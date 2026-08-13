import type { ReactNode } from 'react'
import styled from '@emotion/styled'
import { AppHeader } from '@/components/AppHeader'

export interface PageLayoutProps {
  headerRight?: ReactNode
  children: ReactNode
}

const Main = styled.main`
  max-width: 472px;
  margin: 0 auto;
  padding: 12px 12px 36px;
`

export function PageLayout({ headerRight, children }: PageLayoutProps) {
  return (
    <>
      <AppHeader right={headerRight} />
      <Main>{children}</Main>
    </>
  )
}
