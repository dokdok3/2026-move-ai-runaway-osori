import { Link } from 'react-router-dom'
import styled from '@emotion/styled'
import type { DriverResponse } from '@/api/driver/model'

const SummaryLink = styled(Link)`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
  padding: 15px;
  border: 1px solid ${(props) => props.theme.color.border};
  border-radius: ${(props) => props.theme.radius.md};
  background: ${(props) => props.theme.color.surface};
  color: ${(props) => props.theme.color.text};
  font-size: 16px;
  font-weight: 700;
  line-height: 1.45;
  text-decoration: none;
  touch-action: manipulation;

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

const Profile = styled.span`
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
`

const Avatar = styled.span`
  width: 40px;
  height: 40px;
  flex: none;
  border-radius: 50%;
  background: ${(props) => props.theme.color.blue100};
`

const Rating = styled.span`
  flex: none;
  padding: 7px 9px;
  border-radius: ${(props) => props.theme.radius.sm};
  background: ${(props) => props.theme.color.green100};
  color: ${(props) => props.theme.color.green600};
  font-size: 14px;
  font-weight: 800;
  white-space: nowrap;
`

export function DriverProfileSummary({ profile }: { profile: DriverResponse | undefined }) {
  if (!profile) return null

  const name = profile.name ?? '기사'
  const vehicle = [
    profile.bodyType ?? profile.vehicleType,
    profile.capacityTon && `${profile.capacityTon}톤`,
  ]
    .filter(Boolean)
    .join(' ')

  return (
    <SummaryLink to="/driver/profile" aria-label={`${name} 기사 내 정보 보기`}>
      <Profile>
        <Avatar aria-hidden="true" />
        <span>
          {name} 기사{vehicle ? ` · ${vehicle}` : ''}
        </span>
      </Profile>
      {profile.rating !== undefined && <Rating>평점 {profile.rating.toFixed(1)}</Rating>}
    </SummaryLink>
  )
}
