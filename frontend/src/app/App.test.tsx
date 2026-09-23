import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { LocaleProvider } from '../shared/i18n/LocaleProvider'
import { fireEvent, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { App } from './App'
import { useItemDraft } from '../features/crafting/draft'

const client = new QueryClient({
  defaultOptions: { mutations: { retry: false } },
})
function show() {
  return render(
    <QueryClientProvider client={client}>
      <LocaleProvider initialLanguage="ko">
        <App />
      </LocaleProvider>
    </QueryClientProvider>,
  )
}
afterEach(() => {
  client.clear()
  vi.unstubAllGlobals()
})

describe('제작 작업대', () => {
  beforeEach(() => {
    useItemDraft.getState().setBase()
    window.history.replaceState({}, '', '/')
  })
  it('32종과 제작 효과 미연결을 안내한다', () => {
    show()
    expect(
      screen.getByRole('heading', { level: 1, name: '제작 작업대' }),
    ).toBeVisible()
    expect(screen.getByText('32종')).toBeVisible()
    expect(
      screen.queryByRole('button', { name: '감정 주문서' }),
    ).not.toBeInTheDocument()
    expect(
      screen.getByText(/화폐 소모, 옵션 변경, 확률 계산은 수행하지 않습니다/),
    ).toBeVisible()
  })
  it('우클릭 선택과 사용 요청 후 아이템을 유지하고 Esc로 취소한다', () => {
    show()
    const currency = screen.getByRole('button', { name: '진화의 오브' })
    fireEvent.contextMenu(currency, { clientX: 30, clientY: 40 })
    expect(currency).toHaveAttribute('aria-pressed', 'true')
    fireEvent.click(
      screen.getByRole('button', { name: '중앙 아이템에 선택한 화폐 사용' }),
    )
    expect(screen.getByRole('status')).toHaveTextContent(
      '아이템은 변경되지 않았습니다',
    )
    expect(useItemDraft.getState().source).toBe('base')
    fireEvent.keyDown(window, { key: 'Escape' })
    expect(currency).toHaveAttribute('aria-pressed', 'false')
    fireEvent.click(
      screen.getByRole('button', { name: '중앙 아이템에 선택한 화폐 사용' }),
    )
    expect(screen.getByRole('status')).toHaveTextContent(
      '먼저 창고에서 화폐를 선택하세요',
    )
  })
  it('확보한 이미지를 렌더하고 일반 클릭으로 선택·해제한다', () => {
    show()
    const currency = screen.getByRole('button', { name: '상위 쥬얼러 오브' })
    fireEvent.click(currency)
    expect(currency).toHaveAttribute('aria-pressed', 'true')
    expect(currency.querySelector('img')).toHaveAttribute(
      'src',
      '/assets/currency/CurrencyRerollSocketNumbers02.webp',
    )
    expect(
      screen
        .getByRole('button', { name: '히네코라의 머리카락' })
        .querySelector('img'),
    ).toHaveAttribute('src', '/assets/currency/HinekorasLock.webp')
    fireEvent.click(screen.getByRole('button', { name: /선택 해제/ }))
    expect(currency).toHaveAttribute('aria-pressed', 'false')
  })
  it('실제 로딩 오류가 발생하면 슬롯과 포인터 모두 폴백을 표시한다', () => {
    show()
    const currency = screen.getByRole('button', { name: '히네코라의 머리카락' })
    fireEvent.contextMenu(currency, { clientX: 30, clientY: 40 })
    const slotImage = currency.querySelector('img')
    const cursorImage = document.querySelector('.currency-cursor img')
    expect(slotImage).not.toBeNull()
    expect(cursorImage).not.toBeNull()
    if (!slotImage || !cursorImage)
      throw new Error('Expected slot and cursor images')
    fireEvent.error(slotImage)
    fireEvent.error(cursorImage)
    expect(currency.querySelector('img')).toBeNull()
    expect(currency).toHaveTextContent('이미지 미확보')
    expect(document.querySelector('.currency-cursor')).toHaveTextContent(
      '이미지 미확보',
    )
  })
})
