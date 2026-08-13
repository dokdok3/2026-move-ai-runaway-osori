import { Link } from 'react-router-dom'
import styled from '@emotion/styled'

const StyledLink = styled(Link)`
  display: inline-flex;
  align-items: center;
  margin-left: auto;
  flex-shrink: 0;
  padding: 9px 12px;
  border: 2px solid ${(props) => props.theme.color.borderStrong};
  border-radius: ${(props) => props.theme.radius.md};
  background: ${(props) => props.theme.color.surface};
  color: ${(props) => props.theme.color.navy700};
  font-size: 14px;
  font-weight: 750;
  line-height: 1.2;
  text-decoration: none;
  white-space: nowrap;
`

export function HeaderProfileLink() {
  return <StyledLink to="/shipper/profile">내 정보</StyledLink>
}
