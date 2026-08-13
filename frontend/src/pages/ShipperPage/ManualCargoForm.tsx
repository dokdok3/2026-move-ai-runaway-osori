import type { ChangeEvent, FormEvent } from 'react'
import styled from '@emotion/styled'

import { Button } from '@/components/Button'
import { Card } from '@/components/Card'
import type { CargoType } from '@/api/fare/model'

export interface ManualCargoDraft {
  origin: string
  destination: string
  loadingAt: string
  unloadingAt: string
  cargoType: CargoType | ''
  weightTon: string
  desiredFare: string
}

export interface ManualCargoFormProps {
  value: ManualCargoDraft
  onChange: (value: ManualCargoDraft) => void
  onSubmit: () => void
  onCancel: () => void
  loading: boolean
  error: string | null
  /** AI 변환으로 채운 폼처럼, 빈 항목을 바로 에러로 보여줘야 할 때 켠다. */
  showErrors?: boolean
}

/** 모든 항목이 필수이며 중량과 운임은 0보다 큰 숫자여야 한다. */
function isInvalidValue(value: ManualCargoDraft, key: keyof ManualCargoDraft) {
  const fieldValue = value[key].trim()
  if (!fieldValue) return true

  if (key === 'weightTon' || key === 'desiredFare') {
    const numberValue = Number(fieldValue)
    return !Number.isFinite(numberValue) || numberValue <= 0
  }

  return false
}

const Title = styled.h1`
  margin: 0 0 16px;
  color: ${(props) => props.theme.color.text};
  font-size: 19px;
  font-weight: 800;
`

const Row = styled.label`
  display: block;
  margin-bottom: 18px;
  color: #354052;
  font-size: 16px;
  font-weight: 650;
`

const RequiredMark = styled.span`
  margin-left: 3px;
  color: #b3261e;
`

const Field = styled.input`
  display: block;
  width: 100%;
  min-height: 54px;
  margin-top: 8px;
  padding: 15px 14px;
  border: 2px solid ${(props) => props.theme.color.borderStrong};
  border-radius: ${(props) => props.theme.radius.md};
  background: ${(props) => props.theme.color.surface};
  color: ${(props) => props.theme.color.text};
  font-family: inherit;
  font-size: 16px;

  &[aria-invalid='true'] {
    border-color: #b3261e;
    background: #fff6f5;
  }
`

const Select = Field.withComponent('select')

const Actions = styled.div`
  position: sticky;
  bottom: 0;
  display: grid;
  grid-template-columns: 1fr 1.6fr;
  gap: 10px;
  padding: 12px 0;
  background: ${(props) => props.theme.color.bg};
`

const ErrorText = styled.p`
  color: #b3261e;
  font-size: 14px;
`

export function ManualCargoForm({
  value,
  onChange,
  onSubmit,
  onCancel,
  loading,
  error,
  showErrors = false,
}: ManualCargoFormProps) {
  const change =
    (key: keyof ManualCargoDraft) => (event: ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
      onChange({ ...value, [key]: event.target.value })

  const invalid = (key: keyof ManualCargoDraft) =>
    showErrors && isInvalidValue(value, key) ? true : undefined

  const incomplete = (Object.keys(value) as (keyof ManualCargoDraft)[]).some((key) =>
    isInvalidValue(value, key),
  )

  const submit = (event: FormEvent) => {
    event.preventDefault()
    onSubmit()
  }

  return (
    <form onSubmit={submit}>
      <Card>
        <Title>화물 정보</Title>
        <Row>
          출발지<RequiredMark aria-hidden="true">*</RequiredMark>
          <Field
            required
            aria-label="출발지"
            value={value.origin}
            onChange={change('origin')}
            aria-invalid={invalid('origin')}
            placeholder="예: 서울특별시 송파구"
          />
        </Row>
        <Row>
          출발 일시<RequiredMark aria-hidden="true">*</RequiredMark>
          <Field
            required
            aria-label="출발 일시"
            type="datetime-local"
            value={value.loadingAt}
            onChange={change('loadingAt')}
            aria-invalid={invalid('loadingAt')}
          />
        </Row>
        <Row>
          도착지<RequiredMark aria-hidden="true">*</RequiredMark>
          <Field
            required
            aria-label="도착지"
            value={value.destination}
            onChange={change('destination')}
            aria-invalid={invalid('destination')}
            placeholder="예: 부산광역시 강서구"
          />
        </Row>
        <Row>
          도착 일시<RequiredMark aria-hidden="true">*</RequiredMark>
          <Field
            required
            aria-label="도착 일시"
            type="datetime-local"
            value={value.unloadingAt}
            onChange={change('unloadingAt')}
            aria-invalid={invalid('unloadingAt')}
          />
        </Row>
        <Row>
          화물 종류<RequiredMark aria-hidden="true">*</RequiredMark>
          <Select
            required
            aria-label="화물 종류"
            aria-invalid={invalid('cargoType')}
            value={value.cargoType}
            onChange={change('cargoType')}
          >
            <option value="" disabled>
              화물 종류를 선택해 주세요
            </option>
            <option value="GENERAL">일반화물</option>
            <option value="REFRIGERATED">냉장</option>
            <option value="FROZEN">냉동</option>
            <option value="CONSTRUCTION">건설자재</option>
            <option value="HAZARDOUS">위험물</option>
          </Select>
        </Row>
        <Row>
          중량<RequiredMark aria-hidden="true">*</RequiredMark>
          <Field
            required
            aria-label="중량"
            type="number"
            inputMode="decimal"
            value={value.weightTon}
            onChange={change('weightTon')}
            aria-invalid={invalid('weightTon')}
            placeholder="예: 5"
          />
        </Row>
        <Row>
          운임<RequiredMark aria-hidden="true">*</RequiredMark>
          <Field
            required
            aria-label="운임"
            type="number"
            inputMode="numeric"
            value={value.desiredFare}
            onChange={change('desiredFare')}
            aria-invalid={invalid('desiredFare')}
            placeholder="예: 500000"
            step="10000"
          />
        </Row>
        {error && <ErrorText role="alert">{error}</ErrorText>}
      </Card>
      <Actions>
        <Button type="button" variant="ghost" onClick={onCancel} disabled={loading}>
          취소
        </Button>
        <Button type="submit" disabled={loading || incomplete}>
          {loading ? '등록 중...' : '등록하기'}
        </Button>
      </Actions>
    </form>
  )
}
