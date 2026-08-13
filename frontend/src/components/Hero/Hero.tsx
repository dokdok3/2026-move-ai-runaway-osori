import type { ReactNode } from 'react'
import styled from '@emotion/styled'

export interface HeroProps {
  eyebrow: string
  title: ReactNode
  description: string
}

const Wrapper = styled.div`
  position: relative;
  overflow: hidden;
  padding: 28px 24px;
  margin-bottom: 12px;
  background: linear-gradient(145deg, #edf2ff, #dce6ff);
  border: 1px solid #c8d5f6;
  border-radius: ${(props) => props.theme.radius.lg};
  box-shadow: 0 18px 48px rgba(33, 67, 148, 0.1);

  &:after {
    content: '';
    position: absolute;
    width: 150px;
    height: 150px;
    right: -55px;
    bottom: -85px;
    border: 28px solid rgba(40, 85, 217, 0.09);
    border-radius: 50%;
    pointer-events: none;
  }
`

const Eyebrow = styled.div`
  margin-bottom: 16px;
  color: ${(props) => props.theme.color.navy700};
  font-size: 14px;
  font-weight: 700;
`

const Title = styled.h1`
  margin: 0 0 10px;
  font-size: 29px;
  font-weight: 800;
  line-height: 1.2;
  letter-spacing: -0.02em;
  color: ${(props) => props.theme.color.navy900};

  @media (max-width: 520px) {
    font-size: 27px;
  }
`

const Description = styled.p`
  max-width: 31ch;
  margin: 0;
  font-size: 16px;
  line-height: 1.55;
  color: #526174;
  word-break: keep-all;
`

export function Hero({ eyebrow, title, description }: HeroProps) {
  return (
    <Wrapper>
      <Eyebrow>{eyebrow}</Eyebrow>
      <Title>{title}</Title>
      <Description>{description}</Description>
    </Wrapper>
  )
}
