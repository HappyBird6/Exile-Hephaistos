import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { afterEach, expect, it, vi } from 'vitest'
import { OperationsLogs } from './OperationsLogs'
const logs = {
  summary: { requests: 81, errors: 1, limited: 2, visitors: 3 },
  minutes: [
    { minute: '14:00', requests: 81, errors: 1, limited: 2, average_ms: 15 },
  ],
  routes: [{ value: '/api/craft', requests: 81 }],
  ips: [],
  visitors: [],
  rows: [],
  page: 1,
  pageSize: 40,
  logging: { queued: 0, dropped: 0, storage: 'OK', archive: 'OK' },
}
function renderLogs(section: 'requests' | 'dashboard' = 'requests') {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return render(
    <QueryClientProvider client={client}>
      <OperationsLogs
        policy={{ previousDays: 7, timeZone: 'Asia/Seoul', pageSize: 40 }}
        credential="Basic test"
        epoch={1}
        section={section}
        onSection={vi.fn()}
      />
    </QueryClientProvider>,
  )
}
afterEach(() => vi.useRealTimers())
it('filters by API and IP, resets paging on filter changes and keeps credentials out of URLs', async () => {
  const request = vi
    .fn()
    .mockImplementation(() =>
      Promise.resolve(new Response(JSON.stringify(logs), { status: 200 })),
    )
  vi.stubGlobal('fetch', request)
  renderLogs()
  fireEvent.click(await screen.findByRole('button', { name: /\/api\/craft/ }))
  await waitFor(() =>
    expect(request.mock.calls.at(-1)?.[0]).toContain('route=%2Fapi%2Fcraft'),
  )
  fireEvent.click(await screen.findByRole('button', { name: 'Next' }))
  await waitFor(() =>
    expect(request.mock.calls.at(-1)?.[0]).toContain('page=2'),
  )
  fireEvent.change(await screen.findByLabelText('IP address'), {
    target: { value: '192.0.2.7' },
  })
  fireEvent.click(screen.getByRole('button', { name: 'Apply filters' }))
  await waitFor(() => {
    expect(request.mock.calls.at(-1)?.[0]).toContain('ip=192.0.2.7')
    expect(request.mock.calls.at(-1)?.[0]).toContain('page=1')
  })
  expect(request.mock.calls.every((call) => !call[0].includes('Basic'))).toBe(
    true,
  )
  fireEvent.click(await screen.findByRole('button', { name: 'Clear filters' }))
  await waitFor(() =>
    expect(request.mock.calls.at(-1)?.[0]).not.toContain('ip='),
  )
})
it('switches the selected current day at Korean midnight', async () => {
  vi.useFakeTimers()
  vi.setSystemTime(new Date('2026-11-10T14:59:59Z'))
  vi.stubGlobal(
    'fetch',
    vi
      .fn()
      .mockImplementation(() =>
        Promise.resolve(new Response(JSON.stringify(logs), { status: 200 })),
      ),
  )
  renderLogs('dashboard')
  expect(screen.getByLabelText('Date (Korea)')).toHaveValue('2026-11-10')
  await act(async () => {
    await vi.advanceTimersByTimeAsync(2000)
  })
  expect(screen.getByLabelText('Date (Korea)')).toHaveValue('2026-11-11')
  // The new day's query resolves after React commits the date change.
  await act(async () => {
    await vi.advanceTimersByTimeAsync(20)
  })
  expect(screen.queryByLabelText('Hour')).not.toBeInTheDocument()
  expect(screen.getByRole('img')).toHaveTextContent('20:00')
  expect(screen.getByLabelText('Date (Korea)')).toHaveAttribute(
    'min',
    '2026-11-04',
  )
})
