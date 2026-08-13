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
  border: 1px solid #eec9a8;
  color: ${(props) => props.theme.color.amberText};
  font-size: 14px;
  line-height: 1.5;
`

const Icon = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  margin-top: 1px;
  flex-shrink: 0;
  border-radius: 50%;
  background: #8a4a12;
  color: #fff;
  font-size: 13px;
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
