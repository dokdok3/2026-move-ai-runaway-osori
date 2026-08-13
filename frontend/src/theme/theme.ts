export const theme = {
  color: {
    bg: '#f2f4f7',
    surface: '#ffffff',
    border: '#d5dae1',
    borderStrong: '#c8ced8',
    text: '#17202b',
    muted: '#586474',
    mutedLight: '#77839a',
    label: '#8b95a6',
    navy900: '#17202b',
    navy700: '#2855d9',
    navy700Hover: '#2048c0',
    navy500: '#5577df',
    navy300: '#b8c6f2',
    blue50: '#f4f7ff',
    blue100: '#e8eeff',
    amber100: '#fff4e8',
    amberText: '#8a4a12',
    amberBorder: '#eec9a8',
    amberIcon: '#d98a3c',
    green100: '#e5f6ef',
    green600: '#147653',
    focus: '#ffbf47',
    white: '#ffffff',
  },
  radius: {
    sm: '8px',
    md: '12px',
    lg: '16px',
  },
  font: {
    family: "'Pretendard Variable', 'Pretendard', -apple-system, sans-serif",
  },
} as const

export type AppTheme = typeof theme
