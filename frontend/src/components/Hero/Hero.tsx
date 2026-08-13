import { useState, type ReactNode } from 'react'
import styled from '@emotion/styled'

/**
 * 닫은 배너의 eyebrow 목록. 모듈 스코프라서 화면을 오갔다 와도 유지되고,
 * 새로고침하면 초기화된다.
 * ponytail: 배너 종류가 몇 개뿐이라 eyebrow를 키로 쓴다. 문구가 겹치면 key prop 도입.
 */
const dismissedHeroes = new Set<string>()

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

const CloseButton = styled.button`
  position: absolute;
  top: 12px;
  right: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  padding: 0;
  color: ${(props) => props.theme.color.navy700};
  font-size: 14px;
  line-height: 1;
  background: rgba(255, 255, 255, 0.6);
  border: none;
  border-radius: 50%;
  cursor: pointer;

  &:hover {
    background: rgba(255, 255, 255, 0.95);
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
  const [dismissed, setDismissed] = useState(() => dismissedHeroes.has(eyebrow))

  if (dismissed) return null

  return (
    <Wrapper>
      <CloseButton
        type="button"
        aria-label="배너 닫기"
        onClick={() => {
          dismissedHeroes.add(eyebrow)
          setDismissed(true)
        }}
      >
        ✕
      </CloseButton>
      <Eyebrow>{eyebrow}</Eyebrow>
      <Title>{title}</Title>
      <Description>{description}</Description>
    </Wrapper>
  )
}
