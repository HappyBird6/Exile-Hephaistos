import { act, renderHook, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import type { PropsWithChildren } from 'react'
import { afterEach, expect, it, vi } from 'vitest'
import { useItemTextImport } from './useItemTextImport'
import { useItemDraft } from './draft'
import { locales, setLocale, translate } from '../../shared/i18n/i18n'
import type { MessageKey } from '../../shared/i18n/i18n'

afterEach(() => {
  vi.unstubAllGlobals()
  setLocale('en')
})
it.each(locales)(
  'localizes input validation and safe server failures in %s',
  async (locale) => {
    setLocale(locale)
    const client = new QueryClient({
      defaultOptions: { mutations: { retry: false } },
    })
    const wrapper = ({ children }: PropsWithChildren) => (
      <QueryClientProvider client={client}>{children}</QueryClientProvider>
    )
    const { result, unmount } = renderHook(useItemTextImport, { wrapper })
    const fetch = vi.fn()
    vi.stubGlobal('fetch', fetch)
    const cases: {
      text: string
      key: MessageKey
      code?: string
      status?: number
    }[] = [
      { text: ' ', key: 'input.empty' },
      { text: '한'.repeat(6000), key: 'input.too_large' },
      {
        text: 'invalid',
        key: 'input.invalid',
        code: 'INVALID_ITEM_TEXT',
        status: 400,
      },
      {
        text: 'oversized',
        key: 'input.too_large',
        code: 'ITEM_TEXT_TOO_LARGE',
        status: 413,
      },
      {
        text: 'malformed',
        key: 'input.malformed',
        code: 'MALFORMED_REQUEST',
        status: 400,
      },
      {
        text: 'private details',
        key: 'input.connection',
        code: 'INTERNAL_PRIVATE_DETAIL',
        status: 500,
      },
    ]
    for (const item of cases) {
      fetch.mockResolvedValue(
        new Response(JSON.stringify({ code: item.code }), {
          status: item.status ?? 200,
        }),
      )
      useItemDraft.getState().setText(item.text)
      await act(async () => {
        await result.current.submit(item.text)
      })
      await waitFor(() =>
        expect(result.current.error).toBe(translate(item.key, {}, locale)),
      )
    }
    expect(fetch).toHaveBeenCalledTimes(4)
    act(() => setLocale(locale === 'en' ? 'ko' : 'en'))
    expect(result.current.error).toBe(
      translate('input.connection', {}, locale === 'en' ? 'ko' : 'en'),
    )
    expect(fetch).toHaveBeenCalledTimes(4)
    unmount()
    client.clear()
  },
)
