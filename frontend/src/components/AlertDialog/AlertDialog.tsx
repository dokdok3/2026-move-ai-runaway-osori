import type { ReactNode } from 'react'
import styled from '@emotion/styled'

import { Button } from '@/components/Button'

export interface AlertDialogProps {
  title: string
  description: ReactNode
  confirmLabel: string
  onConfirm: () => void
  onCancel?: () => void
  cancelLabel?: string
  busy?: boolean
}

const Backdrop = styled.div`
  position: fixed;
  inset: 0;
  z-index: 100;
  display: grid;
  place-items: center;
  padding: 20px;
  background: rgba(15, 23, 34, 0.58);
`

const Panel = styled.div`
  width: min(100%, 400px);
  padding: 24px 20px 20px;
  border: 1px solid ${(props) => props.theme.color.border};
  border-radius: 14px;
  background: ${(props) => props.theme.color.surface};
  box-shadow: 0 18px 48px rgba(15, 23, 34, 0.22);
  text-align: center;
`

const Icon = styled.div`
  display: grid;
  place-items: center;
  width: 52px;
  height: 52px;
  margin: 0 auto 14px;
  border-radius: 50%;
  background: ${(props) => props.theme.color.blue100};
  color: ${(props) => props.theme.color.navy700};
  font-size: 25px;
  font-weight: 850;
`

const Title = styled.h2`
  margin: 0;
  color: ${(props) => props.theme.color.text};
  font-size: 21px;
  font-weight: 800;
  line-height: 1.35;
`

const Description = styled.div`
  margin-top: 9px;
  color: ${(props) => props.theme.color.muted};
  font-size: 16px;
  line-height: 1.55;
`

const Actions = styled.div<{ single: boolean }>`
  display: grid;
  grid-template-columns: ${(props) => (props.single ? '1fr' : '1fr 1fr')};
  gap: 10px;
  margin-top: 22px;
`

export function AlertDialog({
  title,
  description,
  confirmLabel,
  onConfirm,
  onCancel,
  cancelLabel = '취소',
  busy = false,
}: AlertDialogProps) {
  return (
    <Backdrop>
      <Panel role="alertdialog" aria-modal="true" aria-labelledby="alert-dialog-title">
        <Icon aria-hidden="true">✓</Icon>
        <Title id="alert-dialog-title">{title}</Title>
        <Description>{description}</Description>
        <Actions single={!onCancel}>
          {onCancel && (
            <Button type="button" variant="ghost" onClick={onCancel} disabled={busy}>
              {cancelLabel}
            </Button>
          )}
          <Button type="button" onClick={onConfirm} disabled={busy} autoFocus>
            {busy ? '처리 중...' : confirmLabel}
          </Button>
        </Actions>
      </Panel>
    </Backdrop>
  )
}
