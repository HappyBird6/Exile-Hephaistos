import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { expect, it, vi } from 'vitest'
import { AdminPage } from './AdminPage'
const dashboard = {
  observedAt: '2026-11-11T01:00:00Z',
  uptimeSeconds: 600,
  requests: 42,
  serverErrors: 1,
  rejectedRequests: 2,
  meanLatencyMs: 20,
  recentMaxLatencyMs: 50,
  heapUsedBytes: 104857600,
  heapMaxBytes: 524288000,
  processors: 2,
  cpuPercent: 12,
  database: 'UP',
  redis: 'UP',
  databaseConnections: 1,
  priceUpdatedAt: null,
  priceStale: true,
  browserEvents: { view_workbench: 3, runtime_error: 1 },
  traffic: [{ at: '2026-11-11T01:00:00Z', requests: 42, errors: 1 }],
  alerts: ['PRICES'],
}
it('authenticates in memory, shows operational data and clears it when locked', async () => {
  const request = vi
    .fn()
    .mockResolvedValue(new Response(JSON.stringify(dashboard), { status: 200 }))
  vi.stubGlobal('fetch', request)
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  render(
    <QueryClientProvider client={client}>
      <AdminPage />
    </QueryClientProvider>,
  )
  expect(request).not.toHaveBeenCalled()
  fireEvent.change(screen.getByLabelText('Password'), {
    target: { value: 'test-password' },
  })
  fireEvent.click(screen.getByRole('button', { name: 'Sign in' }))
  await screen.findByText('42')
  expect(request.mock.calls[0]?.[1]?.headers.Authorization).toBe(
    'Basic ' + btoa('admin:test-password'),
  )
  expect(
    JSON.stringify(
      client
        .getQueryCache()
        .getAll()
        .map((q) => q.queryKey),
    ),
  ).not.toContain('test-password')
  expect(
    screen.getByText('Currency prices', { selector: 'span' }),
  ).toBeInTheDocument()
  fireEvent.click(screen.getByRole('button', { name: 'Lock' }))
  await screen.findByLabelText('Password')
  expect(screen.queryByText('42')).not.toBeInTheDocument()
})
it('keeps metrics hidden after failed authentication', async () => {
  vi.stubGlobal(
    'fetch',
    vi.fn().mockResolvedValue(new Response(null, { status: 401 })),
  )
  render(
    <QueryClientProvider
      client={
        new QueryClient({ defaultOptions: { queries: { retry: false } } })
      }
    >
      <AdminPage />
    </QueryClientProvider>,
  )
  fireEvent.change(screen.getByLabelText('Password'), {
    target: { value: 'incorrect' },
  })
  fireEvent.click(screen.getByRole('button', { name: 'Sign in' }))
  await waitFor(() =>
    expect(screen.getByRole('alert')).toHaveTextContent('Check your password.'),
  )
  expect(screen.queryByText('API requests')).not.toBeInTheDocument()
})
