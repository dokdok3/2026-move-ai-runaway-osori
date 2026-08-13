import styled from '@emotion/styled'

const Main = styled.main`
  width: min(720px, calc(100% - 48px));
  margin: 0 auto;
  padding: 96px 0;
`

const Badge = styled.span`
  display: inline-block;
  padding: 6px 10px;
  border-radius: 999px;
  background: #fff1b8;
  color: #5c4500;
  font-size: 14px;
  font-weight: 700;
`

function App() {
  return (
    <Main>
      <Badge>READY</Badge>
      <h1>Hackathon Starter</h1>
      <p>React, Emotion, TanStack Query, Storybook, Playwright 설정이 완료되었습니다.</p>
    </Main>
  )
}

export default App
