import type { ChangeEvent } from 'react'
import styled from '@emotion/styled'

export interface RegionSelectProps {
  value: string
  options: string[]
  placeholder: string
  ariaLabel: string
  onChange: (value: string) => void
  disabled?: boolean
}

const Select = styled.select`
  width: 100%;
  min-height: 54px;
  padding: 15px 14px;
  border: 2px solid ${(props) => props.theme.color.borderStrong};
  border-radius: ${(props) => props.theme.radius.md};
  background: ${(props) => props.theme.color.surface};
  color: ${(props) => props.theme.color.text};
  font-family: inherit;
  font-size: 16px;
  font-weight: 600;
  text-align: center;
  text-align-last: center;

  &:focus-visible {
    outline: 3px solid ${(props) => props.theme.color.focus};
    outline-offset: 2px;
  }

  &:disabled {
    opacity: 0.6;
  }
`

export function RegionSelect({
  value,
  options,
  placeholder,
  ariaLabel,
  onChange,
  disabled,
}: RegionSelectProps) {
  const handleChange = (event: ChangeEvent<HTMLSelectElement>) => {
    onChange(event.target.value)
  }

  return (
    <Select aria-label={ariaLabel} value={value} onChange={handleChange} disabled={disabled}>
      <option value="">{placeholder}</option>
      {options.map((option) => (
        <option key={option} value={option}>
          {option}
        </option>
      ))}
    </Select>
  )
}
