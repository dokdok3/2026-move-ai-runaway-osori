import type { HTMLAttributes } from 'react'
import styled from '@emotion/styled'

export type CardProps = HTMLAttributes<HTMLDivElement>

const StyledCard = styled.div`
  background: ${(props) => props.theme.color.surface};
  border: 1px solid ${(props) => props.theme.color.border};
  border-radius: 14px;
  padding: 20px 18px;
  margin-bottom: 14px;
  box-shadow: 0 2px 8px rgba(23, 32, 43, 0.06);
`

export function Card(props: CardProps) {
  return <StyledCard {...props} />
}
