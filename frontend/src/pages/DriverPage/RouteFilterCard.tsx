import styled from '@emotion/styled'
import { Card } from '@/components/Card'
import type { RegionResponse } from '@/api/region/model'
import { RegionSelect } from './RegionSelect'

export interface RouteFilterCardProps {
  regions: RegionResponse[]
  originSido: string
  originSigungu: string
  destSido: string
  destSigungu: string
  onOriginSidoChange: (sido: string) => void
  onOriginSigunguChange: (sigungu: string) => void
  onDestSidoChange: (sido: string) => void
  onDestSigunguChange: (sigungu: string) => void
}

const Title = styled.div`
  margin-bottom: 14px;
  font-size: 19px;
  font-weight: 800;
  color: ${(props) => props.theme.color.navy900};
`

const Row = styled.div`
  margin-bottom: 12px;

  &:last-of-type {
    margin-bottom: 0;
  }
`

const Label = styled.div`
  margin-bottom: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #354052;
`

const SelectRow = styled.div`
  display: flex;
  gap: 8px;

  > * {
    flex: 1;
  }
`

function sigungusOf(regions: RegionResponse[], sido: string): string[] {
  return regions.find((region) => region.sido === sido)?.sigungus ?? []
}

export function RouteFilterCard({
  regions,
  originSido,
  originSigungu,
  destSido,
  destSigungu,
  onOriginSidoChange,
  onOriginSigunguChange,
  onDestSidoChange,
  onDestSigunguChange,
}: RouteFilterCardProps) {
  const sidoOptions = regions.map((region) => region.sido).filter((sido): sido is string => !!sido)

  return (
    <Card>
      <Title>활동 지역 설정</Title>

      <Row>
        <Label>출발지</Label>
        <SelectRow>
          <RegionSelect
            ariaLabel="출발지 시도"
            value={originSido}
            options={sidoOptions}
            placeholder="시도 선택"
            onChange={onOriginSidoChange}
          />
          <RegionSelect
            ariaLabel="출발지 시군구"
            value={originSigungu}
            options={sigungusOf(regions, originSido)}
            placeholder="전체"
            onChange={onOriginSigunguChange}
            disabled={!originSido}
          />
        </SelectRow>
      </Row>

      <Row>
        <Label>도착지</Label>
        <SelectRow>
          <RegionSelect
            ariaLabel="도착지 시도"
            value={destSido}
            options={sidoOptions}
            placeholder="시도 선택"
            onChange={onDestSidoChange}
          />
          <RegionSelect
            ariaLabel="도착지 시군구"
            value={destSigungu}
            options={sigungusOf(regions, destSido)}
            placeholder="전체"
            onChange={onDestSigunguChange}
            disabled={!destSido}
          />
        </SelectRow>
      </Row>
    </Card>
  )
}
