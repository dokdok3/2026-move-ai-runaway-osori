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
  padding: 14px 16px 32px;
`

const Footer = styled.footer`
  max-width: 472px;
  margin: 0 auto;
  padding: 6px 16px 28px;
  font-size: 12px;
  color: ${(props) => props.theme.color.muted};
  text-align: center;
`

export function PageLayout({ headerRight, children }: PageLayoutProps) {
  return (
    <>
      <AppHeader right={headerRight} />
      <Main>{children}</Main>
      <Footer>Hi-fi 목업 · 기사 등록 화면 없음 (mock 데이터 전제)</Footer>
    </>
  )
}
