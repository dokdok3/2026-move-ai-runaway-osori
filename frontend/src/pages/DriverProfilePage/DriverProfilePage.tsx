import { type FormEvent, useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useQueryClient } from '@tanstack/react-query'
import styled from '@emotion/styled'
import { useDriverProfileQuery, useUpdateDriverProfileMutation } from '@/api/driver/service'
import { driverQueryKeys } from '@/api/driver/queries'
import type { UpdateDriverProfileRequest } from '@/api/driver/model'
import { Button } from '@/components/Button'
import { Card } from '@/components/Card'
import { PageLayout } from '@/components/PageLayout'

const DEMO_DRIVER_ID = 1

const VEHICLE_TYPES = ['1톤 트럭', '2.5톤 트럭', '5톤 트럭', '11톤 트럭', '카고트레일러']
const CAPACITY_TONS = [1, 2.5, 5, 11, 18]
const BODY_TYPES = ['카고', '냉동탑차', '윙바디', '탱크로리']
const CARGO_TYPES = [
  ['REFRIGERATED', '냉장'],
  ['GENERAL', '일반화물'],
  ['FROZEN', '냉동'],
  ['CONSTRUCTION', '건설자재'],
  ['HAZARDOUS', '위험물'],
] as const

const EMPTY_FORM: UpdateDriverProfileRequest = {
  name: '',
  phoneNumber: '',
  plateNumber: '',
  vehicleType: '',
  capacityTon: 1,
  bodyType: '',
  vehicleCargoTypes: [],
  minAcceptFare: 0,
  contactableFrom: '06:00',
  contactableTo: '20:00',
}

const PageHead = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: 4px 0 12px;
`

const PageTitle = styled.h1`
  margin: 0;
  color: ${(props) => props.theme.color.text};
  font-size: 19px;
  font-weight: 800;
`

const BackLink = styled(Link)`
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 44px;
  padding: 10px 14px;
  border: 2px solid ${(props) => props.theme.color.navy300};
  border-radius: ${(props) => props.theme.radius.md};
  background: ${(props) => props.theme.color.blue100};
  color: ${(props) => props.theme.color.navy700};
  font-size: 15px;
  font-weight: 750;
  text-decoration: none;

  &:focus-visible {
    outline: 3px solid ${(props) => props.theme.color.focus};
    outline-offset: 2px;
  }
`

const CardTitle = styled.h2`
  margin: 0 0 16px;
  color: ${(props) => props.theme.color.text};
  font-size: 19px;
  font-weight: 800;
`

const FormRow = styled.div`
  margin-bottom: 18px;

  &:last-child {
    margin-bottom: 0;
  }
`

const Label = styled.label`
  display: block;
  margin-bottom: 8px;
  color: #354052;
  font-size: 16px;
`

const Input = styled.input`
  display: block;
  width: 100%;
  min-height: 54px;
  padding: 15px 14px;
  border: 2px solid ${(props) => props.theme.color.borderStrong};
  border-radius: ${(props) => props.theme.radius.md};
  background: ${(props) => props.theme.color.surface};
  color: ${(props) => props.theme.color.text};
  font: inherit;

  &:focus {
    border-color: ${(props) => props.theme.color.navy700};
    outline: none;
  }
`

const Select = Input.withComponent('select')

const CheckList = styled.div`
  display: grid;
  gap: 10px;
`

const CheckItem = styled.label`
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 52px;
  padding: 10px 14px;
  border: 2px solid ${(props) => props.theme.color.borderStrong};
  border-radius: ${(props) => props.theme.radius.md};
  background: ${(props) => props.theme.color.surface};
  color: ${(props) => props.theme.color.text};
  font-size: 17px;
  font-weight: 650;

  &:has(input:checked) {
    border-color: ${(props) => props.theme.color.navy700};
    background: ${(props) => props.theme.color.blue100};
  }

  &:focus-within {
    outline: 3px solid ${(props) => props.theme.color.focus};
    outline-offset: 2px;
  }

  input {
    width: 24px;
    height: 24px;
    margin: 0;
    accent-color: ${(props) => props.theme.color.navy700};
  }
`

const Hint = styled.p`
  margin: 8px 0 0;
  color: ${(props) => props.theme.color.muted};
  font-size: 14px;
  line-height: 1.5;
`

const TimeRow = styled.div`
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
`

const StickyActions = styled.div`
  position: sticky;
  bottom: 0;
  display: grid;
  grid-template-columns: 1fr 1.6fr;
  gap: 10px;
  margin: 0 -12px;
  padding: 12px;
  border-top: 1px solid ${(props) => props.theme.color.border};
  background: ${(props) => props.theme.color.bg};
`

const CancelLink = styled(Link)`
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 52px;
  border: 2px solid ${(props) => props.theme.color.borderStrong};
  border-radius: ${(props) => props.theme.radius.md};
  background: ${(props) => props.theme.color.surface};
  color: ${(props) => props.theme.color.navy700};
  font-size: 17px;
  font-weight: 750;
  text-decoration: none;
`

const ErrorText = styled.p`
  color: #b3261e;
  font-size: 14px;
  text-align: center;
`

export function DriverProfilePage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const profileQuery = useDriverProfileQuery({ driverId: DEMO_DRIVER_ID })
  const updateMutation = useUpdateDriverProfileMutation()
  const [form, setForm] = useState<UpdateDriverProfileRequest>(EMPTY_FORM)
  const [seeded, setSeeded] = useState(false)

  useEffect(() => {
    if (!profileQuery.data || seeded) return
    const profile = profileQuery.data
    setForm({
      name: profile.name ?? '',
      phoneNumber: profile.phoneNumber ?? '',
      plateNumber: profile.plateNumber ?? '',
      vehicleType: profile.vehicleType ?? '',
      capacityTon: profile.capacityTon ?? 1,
      bodyType: profile.bodyType ?? '',
      vehicleCargoTypes: profile.vehicleCargoTypes ?? [],
      minAcceptFare: profile.minAcceptFare ?? 0,
      contactableFrom: profile.contactableFrom ?? '06:00',
      contactableTo: profile.contactableTo ?? '20:00',
    })
    setSeeded(true)
  }, [profileQuery.data, seeded])

  const updateField = <K extends keyof UpdateDriverProfileRequest>(
    key: K,
    value: UpdateDriverProfileRequest[K],
  ) => setForm((current) => ({ ...current, [key]: value }))

  const toggleCargoType = (cargoType: string) => {
    const selected = form.vehicleCargoTypes.includes(cargoType)
    updateField(
      'vehicleCargoTypes',
      selected
        ? form.vehicleCargoTypes.filter((type) => type !== cargoType)
        : [...form.vehicleCargoTypes, cargoType].slice(0, 5),
    )
  }

  const submit = async (event: FormEvent) => {
    event.preventDefault()
    const updated = await updateMutation.mutateAsync({
      params: { driverId: DEMO_DRIVER_ID },
      body: form,
    })
    queryClient.setQueryData(driverQueryKeys.profile({ driverId: DEMO_DRIVER_ID }), updated)
    navigate('/driver')
  }

  return (
    <PageLayout>
      <PageHead>
        <PageTitle>내 정보</PageTitle>
        <BackLink to="/driver">← 화물 보기</BackLink>
      </PageHead>

      <form onSubmit={submit}>
        <Card>
          <CardTitle>기본 정보</CardTitle>
          <FormRow>
            <Label htmlFor="driver-name">이름</Label>
            <Input
              id="driver-name"
              value={form.name}
              onChange={(e) => updateField('name', e.target.value)}
              required
            />
          </FormRow>
          <FormRow>
            <Label htmlFor="driver-phone">연락처</Label>
            <Input
              id="driver-phone"
              type="tel"
              value={form.phoneNumber}
              onChange={(e) => updateField('phoneNumber', e.target.value)}
              required
            />
          </FormRow>
          <FormRow>
            <Label htmlFor="driver-plate">차량번호</Label>
            <Input
              id="driver-plate"
              value={form.plateNumber}
              onChange={(e) => updateField('plateNumber', e.target.value)}
              required
            />
          </FormRow>
        </Card>

        <Card>
          <CardTitle>차량 정보</CardTitle>
          <FormRow>
            <Label htmlFor="vehicle-type">차종</Label>
            <Select
              id="vehicle-type"
              value={form.vehicleType}
              onChange={(e) => updateField('vehicleType', e.target.value)}
              required
            >
              <option value="">선택</option>
              {VEHICLE_TYPES.map((type) => (
                <option key={type}>{type}</option>
              ))}
            </Select>
          </FormRow>
          <FormRow>
            <Label htmlFor="capacity">적재 중량</Label>
            <Select
              id="capacity"
              value={form.capacityTon}
              onChange={(e) => updateField('capacityTon', Number(e.target.value))}
            >
              {CAPACITY_TONS.map((ton) => (
                <option key={ton} value={ton}>
                  {ton}톤
                </option>
              ))}
            </Select>
          </FormRow>
          <FormRow>
            <Label htmlFor="body-type">적재함 형태</Label>
            <Select
              id="body-type"
              value={form.bodyType}
              onChange={(e) => updateField('bodyType', e.target.value)}
              required
            >
              <option value="">선택</option>
              {BODY_TYPES.map((type) => (
                <option key={type}>{type}</option>
              ))}
            </Select>
          </FormRow>
          <FormRow>
            <Label as="span" id="cargo-types-label">
              운송 가능 화물
            </Label>
            <CheckList role="group" aria-labelledby="cargo-types-label">
              {CARGO_TYPES.map(([value, label]) => (
                <CheckItem key={value}>
                  <input
                    type="checkbox"
                    checked={form.vehicleCargoTypes.includes(value)}
                    onChange={() => toggleCargoType(value)}
                  />
                  {label}
                </CheckItem>
              ))}
            </CheckList>
            <Hint>고른 화물만 추천 목록에 올라와요. 최대 5개까지 고를 수 있어요.</Hint>
          </FormRow>
        </Card>

        <Card>
          <CardTitle>운행 조건</CardTitle>
          <FormRow>
            <Label htmlFor="min-fare">희망 최소 운임</Label>
            <Input
              id="min-fare"
              type="number"
              inputMode="numeric"
              step="10000"
              value={form.minAcceptFare}
              onChange={(e) => updateField('minAcceptFare', Number(e.target.value))}
              required
            />
            <Hint>건당 금액이에요. 이보다 낮은 화물은 추천에서 빠져요.</Hint>
          </FormRow>
          <FormRow>
            <Label as="span" id="contact-hours-label">
              연락 가능 시간
            </Label>
            <TimeRow role="group" aria-labelledby="contact-hours-label">
              <Input
                type="time"
                aria-label="연락 가능 시작 시간"
                value={form.contactableFrom}
                onChange={(e) => updateField('contactableFrom', e.target.value)}
                required
              />
              <Input
                type="time"
                aria-label="연락 가능 종료 시간"
                value={form.contactableTo}
                onChange={(e) => updateField('contactableTo', e.target.value)}
                required
              />
            </TimeRow>
          </FormRow>
        </Card>

        {updateMutation.isError && (
          <ErrorText role="alert">기사 정보를 수정하지 못했어요.</ErrorText>
        )}
        <StickyActions>
          <CancelLink to="/driver">취소</CancelLink>
          <Button type="submit" fullWidth disabled={updateMutation.isPending}>
            {updateMutation.isPending ? '수정 중...' : '수정하기'}
          </Button>
        </StickyActions>
      </form>
    </PageLayout>
  )
}
