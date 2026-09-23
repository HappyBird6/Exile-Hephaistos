import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import {
  act,
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { LocaleProvider } from '../../shared/i18n/LocaleProvider'
import { CraftingPage } from './CraftingPage'
import { useItemDraft } from './draft'
import type { ParsedItemText, TextLine } from './itemTextApi'

const raw =
  'Item Class: Synthetic Amulets\nRarity: Rare\nSynthetic Name\nSynthetic Base\n--------\nItem Level: 42\n<script>alert(1)</script>\n'
const line = (number: number, raw: string): TextLine => ({
  number,
  raw,
  section: 1,
  kind: 'CONTENT',
})
function result(text = raw): ParsedItemText {
  return {
    text: { originalText: text, lines: [line(1, text)] },
    locale: 'en',
    itemClass: 'Synthetic Amulets',
    rarity: 'RARE',
    rarityText: 'Rare',
    nameLines: [line(3, 'Synthetic Name'), line(4, 'Synthetic Base')],
    displayName: 'Synthetic Name',
    displayBase: 'Synthetic Base',
    itemLevel: 42,
    properties: [
      { key: 'Item Level', value: '42', source: line(6, 'Item Level: 42') },
    ],
    requirements: [{ key: 'Level', value: '10', source: line(9, 'Level: 10') }],
    markedModifiers: [line(10, 'Synthetic effect (implicit)')],
    flags: [line(11, 'Unidentified')],
    unparsedLines: [line(7, '<script>alert(1)</script>')],
    warnings: [
      { code: 'CATALOG_VALIDATION_REQUIRED', lineNumber: 0 },
      { code: 'UNPARSED_LINES', lineNumber: 7 },
      { code: 'FUTURE_WARNING', lineNumber: 0 },
    ],
  }
}
const json = (value: unknown, status = 200) =>
  new Response(JSON.stringify(value), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
let client: QueryClient
let fetchMock: ReturnType<typeof vi.fn<typeof fetch>>
beforeEach(() => {
  useItemDraft.getState().setBase()
  client = new QueryClient({ defaultOptions: { mutations: { retry: false } } })
  fetchMock = vi.fn<typeof fetch>().mockResolvedValue(json(result()))
  vi.stubGlobal('fetch', fetchMock)
  localStorage.clear()
})
afterEach(() => {
  client.clear()
  vi.unstubAllGlobals()
  localStorage.clear()
})
function show() {
  return render(
    <QueryClientProvider client={client}>
      <LocaleProvider initialLanguage="ko">
        <CraftingPage />
      </LocaleProvider>
    </QueryClientProvider>,
  )
}
function enter(text = raw) {
  fireEvent.click(screen.getByRole('button', { name: '아이템 텍스트' }))
  fireEvent.change(screen.getByRole('textbox'), { target: { value: text } })
}
function submit() {
  fireEvent.click(screen.getByRole('button', { name: /아이템 분석/ }))
}

describe('아이템 텍스트 가져오기', () => {
  it('진행 상태·오류와 화폐 선택 알림이 언어를 바꾸면 즉시 번역된다', async () => {
    let reject: (error: Error) => void = () => {
      throw new Error('not started')
    }
    fetchMock.mockImplementationOnce(
      () =>
        new Promise((_resolve, fail) => {
          reject = fail
        }),
    )
    show()
    enter()
    submit()
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1))
    fireEvent.change(screen.getByLabelText('언어'), { target: { value: 'en' } })
    expect(screen.getByRole('status')).toHaveTextContent('Analyzing item')
    await act(async () => {
      reject(new TypeError('offline'))
    })
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Could not connect',
    )
    fireEvent.change(screen.getByLabelText('Language'), {
      target: { value: 'ko' },
    })
    expect(screen.getByRole('alert')).toHaveTextContent('서버에 연결')
    fireEvent.change(screen.getByRole('textbox'), {
      target: { value: raw + 'changed' },
    })
    fireEvent.click(screen.getByRole('button', { name: '진화의 오브' }))
    fireEvent.change(screen.getByLabelText('언어'), { target: { value: 'en' } })
    expect(screen.getByRole('status')).toHaveTextContent(
      'Orb of Transmutation selected',
    )
    fireEvent.keyDown(window, { key: 'Escape' })
    expect(screen.getByRole('status')).toHaveTextContent(
      'Currency selection cleared',
    )
  })
  it('API 정보를 보여주고 미해석 행·경고·원문을 보존하며 HTML은 실행하지 않는다', async () => {
    show()
    enter()
    submit()
    expect(
      await screen.findByRole('heading', { name: 'Synthetic Name' }),
    ).toBeVisible()
    expect(screen.getByText('42')).toBeVisible()
    expect(screen.getByText('Level: 10')).toBeVisible()
    expect(screen.getByText('Synthetic effect (implicit)')).toBeVisible()
    expect(screen.getByText('Unidentified')).toBeVisible()
    expect(screen.getByText(/FUTURE_WARNING/)).toBeVisible()
    expect(document.querySelector('.parsed-item script')).toBeNull()
    expect(document.querySelector('.item-raw')?.textContent).toBe(raw)
    expect(useItemDraft.getState().text).toBe(raw)
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/v1/items/parse',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ text: raw }),
        headers: { 'Content-Type': 'application/json' },
      }),
    )
    fireEvent.click(screen.getByRole('button', { name: '카오스 오브' }))
    fireEvent.click(
      screen.getByRole('button', { name: '중앙 아이템에 선택한 화폐 사용' }),
    )
    expect(screen.getByRole('status')).toHaveTextContent(
      '아이템은 변경되지 않았습니다',
    )
  })
  it('빈 값과 UTF-8 16KiB 초과를 서버 호출 없이 거부한다', () => {
    show()
    enter(' ')
    submit()
    expect(screen.getByRole('alert')).toHaveTextContent('텍스트를 입력')
    fireEvent.change(screen.getByRole('textbox'), {
      target: { value: '가'.repeat(5500) },
    })
    submit()
    expect(screen.getByRole('alert')).toHaveTextContent('16 KiB')
    expect(fetchMock).not.toHaveBeenCalled()
  })
  it.each([
    ['INVALID_ITEM_TEXT', 422, '아이템 종류'],
    ['ITEM_TEXT_TOO_LARGE', 413, '16 KiB'],
    ['MALFORMED_REQUEST', 400, '요청 형식'],
    ['PRIVATE_SERVER_DETAIL', 500, '서버에 연결'],
  ])(
    '오류 %s를 번역된 안전한 안내로 표시하고 재시도할 수 있다',
    async (code, status, expected) => {
      fetchMock.mockResolvedValueOnce(
        json({ code, detail: 'secret internal details' }, status),
      )
      show()
      enter()
      submit()
      expect(await screen.findByRole('alert')).toHaveTextContent(expected)
      expect(screen.queryByText(/secret internal/)).not.toBeInTheDocument()
      expect(useItemDraft.getState().text).toBe(raw)
      submit()
      expect(
        await screen.findByRole('heading', { name: 'Synthetic Name' }),
      ).toBeVisible()
    },
  )
  it('네트워크 실패와 잘못된 성공 응답을 성공처럼 표시하지 않는다', async () => {
    fetchMock
      .mockRejectedValueOnce(new TypeError('offline'))
      .mockResolvedValueOnce(json({ ...result(), itemLevel: '42' }))
    show()
    enter()
    submit()
    expect(await screen.findByRole('alert')).toHaveTextContent('서버에 연결')
    submit()
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2))
    expect(await screen.findByRole('alert')).toHaveTextContent('서버에 연결')
    expect(
      screen.queryByRole('heading', { name: 'Synthetic Name' }),
    ).not.toBeInTheDocument()
  })
  it('편집 중 이전 요청을 취소하고 더 늦게 도착한 결과도 무시한다', async () => {
    let resolveOld: (response: Response) => void = () => {
      throw new Error('not started')
    }
    fetchMock.mockImplementationOnce(
      () =>
        new Promise((resolve) => {
          resolveOld = resolve
        }),
    )
    show()
    enter()
    submit()
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1))
    expect(screen.getByRole('status')).toHaveTextContent('분석 중')
    const signal = fetchMock.mock.calls[0]?.[1]?.signal
    const next = raw + 'new input'
    fireEvent.change(screen.getByRole('textbox'), { target: { value: next } })
    expect(signal?.aborted).toBe(true)
    fetchMock.mockResolvedValueOnce(
      json({ ...result(next), displayName: 'Latest Name' }),
    )
    submit()
    expect(
      await screen.findByRole('heading', { name: 'Latest Name' }),
    ).toBeVisible()
    await act(async () => {
      resolveOld(json(result()))
    })
    expect(screen.getByRole('heading', { name: 'Latest Name' })).toBeVisible()
    expect(
      screen.queryByRole('heading', { name: 'Synthetic Name' }),
    ).not.toBeInTheDocument()
  })
  it('이전 성공 이후 새 입력이 실패하면 이전 결과를 현재 결과처럼 표시하지 않는다', async () => {
    show()
    enter()
    submit()
    await screen.findByRole('heading', { name: 'Synthetic Name' })
    fireEvent.change(screen.getByRole('textbox'), { target: { value: 'bad' } })
    fetchMock.mockResolvedValueOnce(json({ code: 'INVALID_ITEM_TEXT' }, 422))
    submit()
    await screen.findByRole('alert')
    expect(
      screen.queryByRole('heading', { name: 'Synthetic Name' }),
    ).not.toBeInTheDocument()
  })
  it('베이스 전환과 unmount가 진행 중인 요청을 취소한다', async () => {
    fetchMock.mockImplementation(() => new Promise(() => {}))
    const view = show()
    enter()
    submit()
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1))
    const first = fetchMock.mock.calls[0]?.[1]?.signal
    fireEvent.click(screen.getByRole('button', { name: '베이스 선택' }))
    expect(first?.aborted).toBe(true)
    fireEvent.click(screen.getByRole('button', { name: /베이스 배치/ }))
    expect(useItemDraft.getState().source).toBe('base')
    enter()
    submit()
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2))
    const second = fetchMock.mock.calls[1]?.[1]?.signal
    view.unmount()
    expect(second?.aborted).toBe(true)
  })
  it('언어를 바꿔도 원문은 유지하고 경고·label·상태만 즉시 번역한다', async () => {
    const koreanText = '아이템 종류: 테스트\n아이템 희귀도: 일반\n테스트 이름'
    fetchMock.mockResolvedValueOnce(
      json({
        ...result(koreanText),
        locale: 'ko',
        displayName: '테스트 이름',
        displayBase: null,
        itemLevel: null,
      }),
    )
    show()
    enter(koreanText)
    submit()
    await screen.findByRole('heading', { name: '테스트 이름' })
    fireEvent.change(screen.getByLabelText('언어'), { target: { value: 'en' } })
    expect(screen.getByRole('heading', { name: '테스트 이름' })).toBeVisible()
    expect(screen.getByLabelText('Item text copied from the game')).toHaveValue(
      koreanText,
    )
    expect(screen.getByRole('status')).toHaveTextContent(
      'Item information loaded',
    )
    expect(
      within(
        screen.getByRole('complementary', { name: 'Item details' }),
      ).getAllByText('Unknown'),
    ).toHaveLength(2)
    expect(document.documentElement.lang).toBe('en')
  })
})
