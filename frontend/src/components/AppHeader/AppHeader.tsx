import type { ReactNode } from 'react'
import styled from '@emotion/styled'

export interface AppHeaderProps {
  right?: ReactNode
}

const Wrapper = styled.header`
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 72px;
  padding: 14px 16px;
  background: ${(props) => props.theme.color.surface};
  border-bottom: 1px solid ${(props) => props.theme.color.border};
`

const Logo = styled.img`
  width: 44px;
  height: 44px;
  border-radius: 50%;
  object-fit: cover;
  background: ${(props) => props.theme.color.blue100};
  flex-shrink: 0;
`

const Name = styled.div`
  font-size: 18px;
  font-weight: 800;
  line-height: 1.25;
  color: ${(props) => props.theme.color.text};
`

const Tag = styled.div`
  font-size: 13px;
  line-height: 1.4;
  color: ${(props) => props.theme.color.muted};
`

export function AppHeader({ right }: AppHeaderProps) {
  return (
    <Wrapper>
      <Logo src="/logo.png" alt="집나간 벌꿀오소리 로고" />
      <div>
        <Name>집나간 벌꿀오소리</Name>
        <Tag>카카오모빌리티 물류 해커톤</Tag>
      </div>
      {right}
    </Wrapper>
  )
}
