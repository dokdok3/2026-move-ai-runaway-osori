import { useEffect, useRef, useState } from 'react'
import styled from '@emotion/styled'
import { HeaderBackLink } from '@/components/AppHeader'
import { Button } from '@/components/Button'
import { Card } from '@/components/Card'
import { PageLayout } from '@/components/PageLayout'
import { useShipperProfileQuery, useUpdateShipperProfileMutation } from '@/api/shipper/service'
import type { UpdateShipperProfileRequest } from '@/api/shipper/model'

const EMPTY_PROFILE: UpdateShipperProfileRequest = {
  companyName: '',
  contactName: '',
  phoneNumber: '',
  businessNumber: '',
  address: '',
}

const Title = styled.h1`
  margin: 14px 0 6px;
  color: ${(props) => props.theme.color.navy900};
  font-size: 28px;
  line-height: 1.25;
  letter-spacing: -0.04em;
`

const Description = styled.p`
  margin: 0 0 22px;
  color: ${(props) => props.theme.color.muted};
  font-size: 15px;
  line-height: 1.55;
`

const Form = styled.form`
  display: grid;
  gap: 16px;
`

const Field = styled.label`
  display: grid;
  gap: 7px;
  color: ${(props) => props.theme.color.text};
  font-size: 14px;
  font-weight: 700;
`

const Input = styled.input`
  width: 100%;
  min-height: 48px;
  box-sizing: border-box;
  padding: 12px;
  border: 1px solid ${(props) => props.theme.color.borderStrong};
  border-radius: ${(props) => props.theme.radius.sm};
  background: ${(props) => props.theme.color.surface};
  color: ${(props) => props.theme.color.text};
  font: inherit;

  &:focus {
    outline: 3px solid ${(props) => props.theme.color.focus};
    outline-offset: 1px;
  }
`

const Hint = styled.p`
  margin: -4px 0 0;
  color: ${(props) => props.theme.color.muted};
  font-size: 13px;
  line-height: 1.45;
`

const Message = styled.p<{ $error?: boolean }>`
  margin: 0;
  color: ${(props) => (props.$error ? '#b3261e' : props.theme.color.green600)};
  font-size: 14px;
  font-weight: 700;
`

export function ShipperProfilePage() {
  const [form, setForm] = useState<UpdateShipperProfileRequest>(EMPTY_PROFILE)
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const initializedRef = useRef(false)
  const profileQuery = useShipperProfileQuery()
  const updateProfileMutation = useUpdateShipperProfileMutation()

  useEffect(() => {
    if (initializedRef.current || !profileQuery.data) return

    initializedRef.current = true
    setForm({
      companyName: profileQuery.data.companyName ?? '',
      contactName: profileQuery.data.contactName ?? '',
      phoneNumber: profileQuery.data.phoneNumber ?? '',
      businessNumber: profileQuery.data.businessNumber ?? '',
      address: profileQuery.data.address ?? '',
    })
  }, [profileQuery.data])

  const updateField = (field: keyof UpdateShipperProfileRequest, value: string) => {
    setForm((current) => ({ ...current, [field]: value }))
  }

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setMessage(null)
    setError(null)

    if (!form.companyName.trim() || !form.contactName.trim() || !form.phoneNumber.trim()) {
      setError('회사명, 담당자명, 연락처를 입력해 주세요.')
      return
    }

    try {
      await updateProfileMutation.mutateAsync({
        ...form,
        companyName: form.companyName.trim(),
        contactName: form.contactName.trim(),
        phoneNumber: form.phoneNumber.trim(),
        businessNumber: form.businessNumber?.trim() || undefined,
        address: form.address?.trim() || undefined,
      })
      await profileQuery.refetch()
      setMessage('화주 정보를 저장했어요.')
    } catch (requestError) {
      setError(
        requestError instanceof Error ? requestError.message : '화주 정보를 저장하지 못했어요.',
      )
    }
  }

  return (
    <PageLayout headerRight={<HeaderBackLink />}>
      <Title>화주 정보 관리</Title>
      <Description>배차와 연락에 사용할 회사 및 담당자 정보를 등록해 주세요.</Description>

      <Card>
        {profileQuery.isLoading ? (
          <Description>화주 정보를 불러오는 중이에요.</Description>
        ) : (
          <Form onSubmit={handleSubmit}>
            <Field>
              회사명
              <Input
                value={form.companyName}
                onChange={(event) => updateField('companyName', event.target.value)}
                maxLength={100}
                required
              />
            </Field>
            <Field>
              담당자명
              <Input
                value={form.contactName}
                onChange={(event) => updateField('contactName', event.target.value)}
                maxLength={50}
                required
              />
            </Field>
            <Field>
              연락처
              <Input
                type="tel"
                value={form.phoneNumber}
                onChange={(event) => updateField('phoneNumber', event.target.value)}
                maxLength={20}
                placeholder="010-1234-5678"
                required
              />
            </Field>
            <Field>
              사업자등록번호
              <Input
                value={form.businessNumber ?? ''}
                onChange={(event) => updateField('businessNumber', event.target.value)}
                maxLength={20}
                placeholder="123-45-67890"
              />
            </Field>
            <Field>
              사업장 주소
              <Input
                value={form.address ?? ''}
                onChange={(event) => updateField('address', event.target.value)}
                maxLength={200}
                placeholder="서울특별시 강남구 테헤란로 123"
              />
            </Field>
            <Hint>사업자등록번호와 주소는 선택 입력 항목입니다.</Hint>
            {error && (
              <Message $error role="alert">
                {error}
              </Message>
            )}
            {message && <Message role="status">{message}</Message>}
            <Button type="submit" fullWidth disabled={updateProfileMutation.isPending}>
              {updateProfileMutation.isPending ? '저장 중...' : '저장하기'}
            </Button>
          </Form>
        )}
      </Card>
    </PageLayout>
  )
}
