import type { ChangeEvent } from 'react'
import styled from '@emotion/styled'
import { Card } from '@/components/Card'
import { Button } from '@/components/Button'

export interface CargoRequestFormProps {
  value: string
  onChange: (value: string) => void
  onSubmit: () => void
  loading: boolean
  error: string | null
  onManualEntry: () => void
}

const Title = styled.div`
  margin-bottom: 14px;
  font-size: 19px;
  font-weight: 800;
  color: ${(props) => props.theme.color.navy900};
`

const Textarea = styled.textarea`
  display: block;
  width: 100%;
  min-height: 90px;
  margin-bottom: 10px;
  padding: 15px 14px;
  border: 2px solid ${(props) => props.theme.color.borderStrong};
  border-radius: ${(props) => props.theme.radius.md};
  background: ${(props) => props.theme.color.white};
  color: ${(props) => props.theme.color.text};
  font-family: inherit;
  font-size: 16px;
  line-height: 1.45;
  resize: none;

  &::placeholder {
    color: ${(props) => props.theme.color.muted};
  }

  &:focus-visible {
    outline: none;
    border-color: ${(props) => props.theme.color.navy700};
  }
`

const ErrorText = styled.p`
  margin: 0 0 10px;
  color: #b3261e;
  font-size: 14px;
  line-height: 1.5;
`

const Divider = styled.div`
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 2px 0 12px;
  color: ${(props) => props.theme.color.mutedLight};
  font-size: 14px;
  font-weight: 650;

  &:before,
  &:after {
    content: '';
    height: 1px;
    flex: 1;
    background: ${(props) => props.theme.color.border};
  }
`

const ManualButton = styled.button`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  width: 100%;
  min-height: 82px;
  padding: 16px 18px;
  border: 2px solid ${(props) => props.theme.color.borderStrong};
  border-radius: 14px;
  background: ${(props) => props.theme.color.surface};
  color: ${(props) => props.theme.color.text};
  font-family: inherit;
  text-align: left;
  cursor: pointer;

  strong {
    display: block;
    font-size: 17px;
    font-weight: 800;
  }

  small {
    display: block;
    margin-top: 3px;
    color: ${(props) => props.theme.color.muted};
    font-size: 14px;
  }

  > span:last-child {
    color: ${(props) => props.theme.color.navy700};
    font-size: 25px;
    font-weight: 750;
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

export function CargoRequestForm({
  value,
  onChange,
  onSubmit,
  loading,
  error,
  onManualEntry,
}: CargoRequestFormProps) {
  const handleChange = (event: ChangeEvent<HTMLTextAreaElement>) => {
    onChange(event.target.value)
  }

  return (
    <>
      <Card>
        <Title>화물 요청 입력</Title>
        <Textarea
          rows={3}
          aria-label="화물 요청 원문"
          placeholder="예: 내일 서울→부산 냉장 5톤 예산 50만원"
          value={value}
          onChange={handleChange}
        />
        {error && <ErrorText role="alert">{error}</ErrorText>}
        <Button type="button" fullWidth onClick={onSubmit} disabled={loading || !value.trim()}>
          {loading ? '변환 중...' : 'AI 자동 변환'}
        </Button>
      </Card>
      <Divider>또는</Divider>
      <ManualButton type="button" onClick={onManualEntry}>
        <span>
          <strong>직접 입력하기</strong>
          <small>화물 정보를 항목별로 입력합니다</small>
        </span>
        <span aria-hidden="true">→</span>
      </ManualButton>
    </>
  )
}
