import styled from '@emotion/styled'
import { Hero } from '@/components/Hero'
import { PageLayout } from '@/components/PageLayout'
import { RolePickLink } from './RolePickLink'

const PickList = styled.div`
  display: flex;
  flex-direction: column;
  gap: 12px;
`

const ShipperIcon = (
  <svg viewBox="0 0 60 60">
    <path className="pick-fill" d="M30 9 L50 19 L50 41 L30 51 L10 41 L10 19 Z" />
    <path className="pick-stroke" d="M30 9 L50 19 L50 41 L30 51 L10 41 L10 19 Z" />
    <path className="pick-stroke" d="M10 19 L30 29 L50 19" />
    <path className="pick-stroke" d="M30 29 L30 51" />
    <path className="pick-stroke" d="M20 14 L40 24" />
  </svg>
)

const DriverIcon = (
  <svg viewBox="0 0 60 60">
    <path className="pick-fill" d="M7 17 H34 V41 H7 Z" />
    <path className="pick-stroke" d="M7 41 V19 a2 2 0 0 1 2-2 H32 a2 2 0 0 1 2 2 V41" />
    <path className="pick-stroke" d="M34 26 H43 L52 35 V41" />
    <path className="pick-stroke" d="M7 41 H14 M24 41 H38 M48 41 H52" />
    <circle className="pick-stroke" cx="19" cy="43" r="5" />
    <circle className="pick-stroke" cx="43" cy="43" r="5" />
  </svg>
)

export function RoleSelectPage() {
  return (
    <PageLayout>
      <Hero
        eyebrow="✦ 시작하기"
        title={
          <>
            어떤 일로
            <br />
            오셨나요?
          </>
        }
        description="역할을 고르면 바로 그 화면으로 갑니다. 각 화면에서 언제든 처음으로 돌아올 수 있어요."
      />

      <PickList>
        <RolePickLink
          to="/shipper"
          icon={ShipperIcon}
          title="화주"
          description="화물을 맡기고 기사를 찾습니다"
        />
        <RolePickLink
          to="/driver"
          icon={DriverIcon}
          title="기사"
          description="내 구간에 맞는 화물을 골라 받습니다"
        />
      </PickList>
    </PageLayout>
  )
}
