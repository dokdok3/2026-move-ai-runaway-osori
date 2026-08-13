import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import styled from '@emotion/styled'

export interface RolePickLinkProps {
  to: string
  icon: ReactNode
  title: string
  description: string
}

const StyledLink = styled(Link)`
  display: flex;
  align-items: center;
  gap: 16px;
  width: 100%;
  min-height: 124px;
  padding: 22px 20px;
  border: 2px solid ${(props) => props.theme.color.borderStrong};
  border-radius: 14px;
  background: ${(props) => props.theme.color.surface};
  text-align: left;
  text-decoration: none;
  cursor: pointer;
  touch-action: manipulation;
  -webkit-tap-highlight-color: transparent;
  transition:
    border-color 0.18s ease,
    transform 0.18s ease;

  &:active {
    transform: translateY(1px);
  }

  &:focus-visible {
    outline: 3px solid ${(props) => props.theme.color.focus};
    outline-offset: 2px;
  }

  @media (hover: hover) {
    &:hover {
      border-color: ${(props) => props.theme.color.navy700};
    }
  }
`

const IconWrapper = styled.span`
  flex: none;
  width: 60px;
  height: 60px;

  svg .pick-fill {
    fill: ${(props) => props.theme.color.blue100};
  }

  svg .pick-stroke {
    fill: none;
    stroke: ${(props) => props.theme.color.navy700};
    stroke-width: 2;
    stroke-linecap: round;
    stroke-linejoin: round;
  }
`

const Title = styled.span`
  display: block;
  font-size: 20px;
  font-weight: 800;
  line-height: 1.3;
  color: ${(props) => props.theme.color.navy900};
`

const Description = styled.span`
  display: block;
  margin-top: 5px;
  font-size: 15px;
  line-height: 1.5;
  color: ${(props) => props.theme.color.muted};
  word-break: keep-all;
`

export function RolePickLink({ to, icon, title, description }: RolePickLinkProps) {
  return (
    <StyledLink to={to}>
      <IconWrapper aria-hidden="true">{icon}</IconWrapper>
      <span>
        <Title>{title}</Title>
        <Description>{description}</Description>
      </span>
    </StyledLink>
  )
}
