import type { ButtonHTMLAttributes } from 'react'
import styled from '@emotion/styled'

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'ghost'
  fullWidth?: boolean
}

const StyledButton = styled.button<{ variant: 'primary' | 'ghost'; fullWidth: boolean }>`
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: ${(props) => (props.fullWidth ? '100%' : 'auto')};
  min-height: 52px;
  padding: 14px 18px;
  border-radius: ${(props) => props.theme.radius.md};
  font-family: inherit;
  font-size: 17px;
  font-weight: 750;
  letter-spacing: -0.01em;
  line-height: 1.2;
  cursor: pointer;
  touch-action: manipulation;
  -webkit-tap-highlight-color: transparent;
  transition:
    background 0.18s ease,
    border-color 0.18s ease,
    transform 0.18s ease;

  ${(props) =>
    props.variant === 'primary'
      ? `
        background: ${props.theme.color.navy700};
        color: ${props.theme.color.white};
        border: none;
      `
      : `
        background: ${props.theme.color.surface};
        color: ${props.theme.color.navy700};
        border: 2px solid ${props.theme.color.borderStrong};
      `}

  &:active {
    transform: translateY(1px);
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
    transform: none;
  }

  &:focus-visible {
    outline: 3px solid ${(props) => props.theme.color.focus};
    outline-offset: 2px;
  }

  @media (hover: hover) {
    &:not(:disabled):hover {
      background: ${(props) =>
        props.variant === 'primary' ? props.theme.color.navy700Hover : props.theme.color.surface};
      border-color: ${(props) =>
        props.variant === 'primary' ? 'transparent' : props.theme.color.mutedLight};
    }
  }
`

export function Button({ variant = 'primary', fullWidth = false, ...props }: ButtonProps) {
  return <StyledButton variant={variant} fullWidth={fullWidth} {...props} />
}
