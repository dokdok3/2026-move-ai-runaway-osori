import styled from '@emotion/styled'

export interface FareWarningBannerProps {
  message: string
}

const Wrapper = styled.div`
  display: flex;
  gap: 12px;
  align-items: flex-start;
  border-radius: ${(props) => props.theme.radius.md};
  padding: 15px 14px;
  margin-bottom: 14px;
  background: ${(props) => props.theme.color.amber100};
  border: 1px solid ${(props) => props.theme.color.amberBorder};
  color: ${(props) => props.theme.color.amberText};
  font-size: 16px;
  line-height: 1.5;
`

const Icon = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  margin-top: 1px;
  flex-shrink: 0;
  border-radius: 50%;
  background: ${(props) => props.theme.color.amberIcon};
  color: #fff;
  font-size: 15px;
  font-weight: 700;
`

export function FareWarningBanner({ message }: FareWarningBannerProps) {
  return (
    <Wrapper role="alert">
      <Icon aria-hidden="true">!</Icon>
      <div>{message}</div>
    </Wrapper>
  )
}
