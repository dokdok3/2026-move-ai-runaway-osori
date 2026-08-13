import type { ReactNode } from 'react'
import styled from '@emotion/styled'

export interface HeaderPillProps {
  children: ReactNode
}

const Pill = styled.span`
  margin-left: auto;
  flex-shrink: 0;
  padding: 8px 10px;
  border-radius: ${(props) => props.theme.radius.sm};
  background: ${(props) => props.theme.color.navy700};
  color: ${(props) => props.theme.color.white};
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.02em;
  white-space: nowrap;
`

export function HeaderPill({ children }: HeaderPillProps) {
  return <Pill>{children}</Pill>
}
