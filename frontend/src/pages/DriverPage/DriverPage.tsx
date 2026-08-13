import { Hero } from '@/components/Hero'
import { HeaderBackLink } from '@/components/AppHeader'
import { PageLayout } from '@/components/PageLayout'
import { Card } from '@/components/Card'

export function DriverPage() {
  return (
    <PageLayout headerRight={<HeaderBackLink />}>
      <Hero
        eyebrow="✦ 내 지역 화물만 골라보기"
        title={
          <>
            오늘 뜬 화물,
            <br />
            바로 확인하세요
          </>
        }
        description="활동 지역과 조건에 맞는 화물만 추천해서 놓치지 않게 알려드려요."
      />
      <Card>기사 화면은 곧 제공될 예정이에요.</Card>
    </PageLayout>
  )
}
