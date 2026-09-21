import { render, screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { App } from './App'
import { AppProviders } from './AppProviders'

describe('시작 페이지', () => {
  it('계산과 외부 연동이 아직 제공되지 않음을 명확하게 안내한다', () => {
    render(
      <AppProviders>
        <App />
      </AppProviders>,
    )
    expect(
      screen.getByRole('heading', { level: 1, name: 'Exile Hephaistos' }),
    ).toBeVisible()
    const availability = screen.getByRole('region', {
      name: '제작 계산 준비 중',
    })
    expect(
      within(availability).getByText(/제작 계산을 제공하지 않습니다/),
    ).toBeVisible()
    expect(
      within(availability).getByText(/AI 목표 초안, 가격 조회, 로그인/),
    ).toBeVisible()
    expect(
      screen.queryByRole('button', { name: /계산/ }),
    ).not.toBeInTheDocument()
  })
})
