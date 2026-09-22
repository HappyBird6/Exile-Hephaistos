import { fireEvent, render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it } from 'vitest'
import { App } from './App'
import { useItemDraft } from '../features/crafting/draft'

describe('제작 작업대', () => {
  beforeEach(() => {
    useItemDraft.getState().setBase()
    window.history.replaceState({}, '', '/')
  })
  it('32종과 제작 효과 미연결을 안내한다', () => {
    render(<App />)
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
    render(<App />)
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
    render(<App />)
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
    render(<App />)
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
  it('빈 입력을 거부하고 알 수 없는 옵션까지 원문 그대로 보존한다', () => {
    render(<App />)
    fireEvent.click(screen.getByRole('button', { name: '아이템 텍스트' }))
    fireEvent.click(screen.getByRole('button', { name: /원문 배치/ }))
    expect(screen.getByRole('alert')).toBeVisible()
    const raw = '  미확인 아이템\n--------\n알 수 없는 옵션 +123\n'
    fireEvent.change(screen.getByRole('textbox'), { target: { value: raw } })
    fireEvent.click(screen.getByRole('button', { name: /원문 배치/ }))
    expect(useItemDraft.getState().text).toBe(raw)
    expect(document.querySelector('.item-raw')?.textContent).toBe(raw)
    fireEvent.click(screen.getByRole('button', { name: '카오스 오브' }))
    fireEvent.click(
      screen.getByRole('button', { name: '중앙 아이템에 선택한 화폐 사용' }),
    )
    expect(useItemDraft.getState().text).toBe(raw)
    fireEvent.click(screen.getByRole('button', { name: '베이스 선택' }))
    fireEvent.click(screen.getByRole('button', { name: /베이스 배치/ }))
    expect(useItemDraft.getState().source).toBe('base')
  })
})
