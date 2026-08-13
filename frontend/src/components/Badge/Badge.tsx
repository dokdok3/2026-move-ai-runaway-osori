import type { ReactNode } from 'react'
import styled from '@emotion/styled'
import type { AppTheme } from '@/theme/theme'

export interface BadgeProps {
  tone?: 'ok' | 'warn' | 'neutral'
  children: ReactNode
}

const toneColors = (theme: AppTheme, tone: 'ok' | 'warn' | 'neutral') => {
  if (tone === 'ok') return { bg: theme.color.green100, color: theme.color.green600 }
  if (tone === 'warn') return { bg: theme.color.amber100, color: theme.color.amberText }
  return { bg: theme.color.blue100, color: theme.color.navy700 }
}

const Chip = styled.span<{ tone: 'ok' | 'warn' | 'neutral' }>`
  display: inline-flex;
  align-items: center;
  padding: 7px 9px;
  border-radius: ${(props) => props.theme.radius.sm};
  font-size: 13px;
  font-weight: 700;
  line-height: 1.25;
  white-space: nowrap;
  background: ${(props) => toneColors(props.theme, props.tone).bg};
  color: ${(props) => toneColors(props.theme, props.tone).color};
`

export function Badge({ tone = 'neutral', children }: BadgeProps) {
  return <Chip tone={tone}>{children}</Chip>
}
