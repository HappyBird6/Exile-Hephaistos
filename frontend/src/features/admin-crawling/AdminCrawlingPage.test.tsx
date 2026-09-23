import { LocaleProvider } from '../../shared/i18n/LocaleProvider'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import {
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { AdminCrawlingPage } from './AdminCrawlingPage'
import type { CrawlRun, CrawlSettings } from './api'

const initialSettings: CrawlSettings = {
  version: 0,
  runnerEnabled: true,
  limits: { maxTargets: 20 },
  targets: [
    {
      id: 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa',
      name: '화폐',
      url: 'https://poe2db.tw/us/Currency',
      enabled: true,
    },
  ],
}
const initialRun: CrawlRun = {
  id: 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb',
  status: 'RAW_CAPTURED',
  createdAt: '2026-09-22T01:00:00Z',
  startedAt: '2026-09-22T01:00:01Z',
  finishedAt: '2026-09-22T01:00:02Z',
  targets: initialSettings.targets,
  errorCode: null,
  sourceCount: 3,
}

interface BackendState {
  configured: boolean
  authenticated: boolean
  settings: CrawlSettings
  runs: CrawlRun[]
  saveStatus: number
  startStatus: number
  loginStatus: number
}

let backend: BackendState
let client: QueryClient
let requests: { path: string; init: RequestInit }[]

function json(value: unknown, status = 200) {
  return new Response(JSON.stringify(value), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

beforeEach(() => {
  backend = {
    configured: true,
    authenticated: true,
    settings: structuredClone(initialSettings),
    runs: [],
    saveStatus: 200,
    startStatus: 202,
    loginStatus: 204,
  }
  requests = []
  client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
  vi.stubGlobal(
    'fetch',
    vi.fn(async (path: string, init: RequestInit = {}) => {
      requests.push({ path, init })
      const method = init.method ?? 'GET'
      if (path === '/api/v1/admin/session') {
        return json({
          configured: backend.configured,
          authenticated: backend.authenticated,
          csrfToken: backend.authenticated
            ? 'authenticated-csrf'
            : 'login-csrf',
          csrfHeaderName: 'X-CSRF-TOKEN',
        })
      }
      if (path === '/api/v1/admin/login') {
        if (backend.loginStatus !== 204)
          return json({ code: 'AUTH_REQUIRED' }, backend.loginStatus)
        backend.authenticated = true
        return new Response(null, { status: 204 })
      }
      if (path === '/api/v1/admin/logout') {
        backend.authenticated = false
        return new Response(null, { status: 204 })
      }
      if (path === '/api/v1/admin/crawl-settings') {
        if (method === 'PUT') {
          if (backend.saveStatus !== 200)
            return json({ code: 'VERSION_CONFLICT' }, backend.saveStatus)
          return json({
            ...backend.settings,
            version: backend.settings.version + 1,
          })
        }
        return json(backend.settings)
      }
      if (path === '/api/v1/admin/crawl-runs') {
        if (method === 'POST') {
          if (backend.startStatus !== 202)
            return json({ code: 'RUN_ACTIVE' }, backend.startStatus)
          backend.runs = [
            {
              ...initialRun,
              status: 'QUEUED',
              sourceCount: null,
              finishedAt: null,
            },
          ]
          return json(backend.runs[0], 202)
        }
        return json({ runs: backend.runs })
      }
      throw new Error(`Unexpected request: ${path}`)
    }),
  )
})

afterEach(() => {
  client.clear()
  vi.unstubAllGlobals()
  localStorage.clear()
})

function show() {
  return render(
    <QueryClientProvider client={client}>
      <LocaleProvider initialLanguage="en">
        <AdminCrawlingPage />
      </LocaleProvider>
    </QueryClientProvider>,
  )
}

function mutations(method: string) {
  return requests.filter((entry) => entry.init.method === method)
}

describe('Crawling management', () => {
  it('shows English status and validation while preserving user target names', async () => {
    backend.runs = [initialRun]
    show()
    expect(await screen.findByText('Raw collection completed')).toBeVisible()
    fireEvent.change(screen.getByLabelText('Page URL'), {
      target: { value: 'https://example.invalid/private' },
    })
    fireEvent.click(
      screen.getByRole('button', { name: 'Save target settings' }),
    )
    expect(await screen.findByRole('alert')).toHaveTextContent('public poe2db')
    expect(
      screen.getByRole('heading', { name: 'Crawling management' }),
    ).toBeVisible()
    expect(screen.getByText('Raw collection completed')).toBeVisible()
    expect(screen.getByRole('alert')).toHaveTextContent('public poe2db')
    expect(
      screen.getByRole('button', { name: 'Delete target 1' }),
    ).toBeVisible()
    expect(screen.getByLabelText('Name')).toHaveValue('화폐')
    expect(document.documentElement.lang).toBe('en')
  })
  it('does not access settings or run APIs without admin configuration', async () => {
    backend.configured = false
    backend.authenticated = false
    show()
    expect(
      await screen.findByRole('heading', {
        name: 'Admin account not configured',
      }),
    ).toBeVisible()
    expect(requests.every((entry) => entry.path.endsWith('/session'))).toBe(
      true,
    )
    expect(
      screen.queryByRole('button', { name: 'Start crawling' }),
    ).not.toBeInTheDocument()
  })

  it('logs in using CSRF without storing a password', async () => {
    backend.authenticated = false
    show()
    fireEvent.change(await screen.findByLabelText('Username'), {
      target: { value: 'operator' },
    })
    fireEvent.change(screen.getByLabelText('Password'), {
      target: { value: 'synthetic-secret' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Log in' }))
    expect(await screen.findByDisplayValue('화폐')).toBeVisible()
    const login = mutations('POST').find((entry) =>
      entry.path.endsWith('/login'),
    )
    expect(login?.init.headers).toEqual({ 'X-CSRF-TOKEN': 'login-csrf' })
    expect(login?.init.body?.toString()).toBe(
      'username=operator&password=synthetic-secret',
    )
    expect(sessionStorage.length).toBe(0)
    expect(localStorage.length).toBe(0)
  })

  it('clears the password after failed login', async () => {
    backend.authenticated = false
    backend.loginStatus = 401
    show()
    fireEvent.change(await screen.findByLabelText('Username'), {
      target: { value: 'operator' },
    })
    fireEvent.change(screen.getByLabelText('Password'), {
      target: { value: 'incorrect' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Log in' }))
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'login details are incorrect',
    )
    expect(screen.getByLabelText('Password')).toHaveValue('')
    expect(screen.queryByLabelText('Page URL')).not.toBeInTheDocument()
  })

  it('does not start collection on page load', async () => {
    show()
    expect(await screen.findByDisplayValue('화폐')).toBeVisible()
    expect(mutations('POST')).toHaveLength(0)
    expect(screen.getByText(/Data cleanup, change comparison/)).toBeVisible()
  })

  it('blocks runs while editing and saves versioned target changes', async () => {
    show()
    fireEvent.change(await screen.findByLabelText('Name'), {
      target: { value: '화폐 목록' },
    })
    expect(
      screen.getByRole('button', { name: 'Start crawling' }),
    ).toBeDisabled()
    backend.settings.targets[0] = {
      ...initialSettings.targets[0]!,
      name: '화폐 목록',
    }
    fireEvent.click(
      screen.getByRole('button', { name: 'Save target settings' }),
    )
    await waitFor(() =>
      expect(
        screen.getByText('Target settings saved to the server.'),
      ).toBeVisible(),
    )
    const saved = mutations('PUT')[0]
    expect(saved?.init.headers).toEqual({
      'X-CSRF-TOKEN': 'authenticated-csrf',
      'Content-Type': 'application/json',
    })
    expect(saved?.init.body).toBe(
      JSON.stringify({ version: 0, targets: backend.settings.targets }),
    )
    expect(screen.getByRole('button', { name: 'Start crawling' })).toBeEnabled()
  })

  it('preserves edits after conflicts and supports explicit reload', async () => {
    backend.saveStatus = 409
    show()
    fireEvent.change(await screen.findByLabelText('Name'), {
      target: { value: '미저장 값' },
    })
    fireEvent.click(
      screen.getByRole('button', { name: 'Save target settings' }),
    )
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Another change or active run',
    )
    expect(screen.getByLabelText('Name')).toHaveValue('미저장 값')
    fireEvent.click(
      screen.getByRole('button', { name: /Reload server settings/ }),
    )
    await waitFor(() =>
      expect(screen.getByLabelText('Name')).toHaveValue('화폐'),
    )
  })

  it('rejects unsupported and duplicate URLs before saving', async () => {
    show()
    fireEvent.change(await screen.findByLabelText('Page URL'), {
      target: { value: 'https://example.invalid/private' },
    })
    fireEvent.click(
      screen.getByRole('button', { name: 'Save target settings' }),
    )
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'public poe2db page URL',
    )
    expect(mutations('PUT')).toHaveLength(0)
    fireEvent.change(screen.getByLabelText('Page URL'), {
      target: { value: 'https://poe2db.tw/us/Currency' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Add target' }))
    const row = screen.getByRole('group', { name: 'Target 2' })
    fireEvent.change(within(row).getByLabelText('Name'), {
      target: { value: '중복' },
    })
    fireEvent.change(within(row).getByLabelText('Page URL'), {
      target: { value: 'https://poe2db.tw/us/Currency' },
    })
    fireEvent.click(
      screen.getByRole('button', { name: 'Save target settings' }),
    )
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'cannot be registered twice',
    )
    expect(mutations('PUT')).toHaveLength(0)
  })

  it('shows requested run status and prevents duplicate runs', async () => {
    show()
    await waitFor(() =>
      expect(
        screen.getByRole('button', { name: 'Start crawling' }),
      ).toBeEnabled(),
    )
    fireEvent.click(screen.getByRole('button', { name: 'Start crawling' }))
    expect(await screen.findByText('Queued')).toBeVisible()
    expect(
      screen.getByRole('button', { name: 'Collection in progress' }),
    ).toBeDisabled()
    const starts = mutations('POST').filter((entry) =>
      entry.path.endsWith('/crawl-runs'),
    )
    expect(starts).toHaveLength(1)
    expect(starts[0]?.init.headers).toEqual({
      'X-CSRF-TOKEN': 'authenticated-csrf',
    })
  })

  it('disables collection when the runner is off', async () => {
    backend.settings.runnerEnabled = false
    show()
    expect(
      await screen.findByText('The server collection runner is disabled.'),
    ).toBeVisible()
    expect(
      screen.getByRole('button', { name: 'Start crawling' }),
    ).toBeDisabled()
  })

  it('distinguishes raw collection outcomes and preserves missing counts', async () => {
    backend.runs = [
      initialRun,
      {
        ...initialRun,
        id: 'failed-run',
        status: 'FAILED',
        errorCode: 'HTTP_403',
        sourceCount: null,
      },
    ]
    show()
    expect(await screen.findByText('Raw collection completed')).toBeVisible()
    expect(screen.getByText('Collection failed')).toBeVisible()
    expect(screen.getByText('Error code: HTTP_403')).toBeVisible()
    expect(screen.getByText('3 responses')).toBeVisible()
    expect(screen.getByText('—')).toBeVisible()
  })

  it('clears private cache and UI on logout', async () => {
    show()
    expect(await screen.findByDisplayValue('화폐')).toBeVisible()
    fireEvent.click(screen.getByRole('button', { name: 'Log out' }))
    expect(
      await screen.findByRole('heading', { name: 'Admin login' }),
    ).toBeVisible()
    expect(screen.queryByDisplayValue('화폐')).not.toBeInTheDocument()
    await waitFor(() =>
      expect(client.getQueryData(['admin', 'crawl-settings'])).toBeUndefined(),
    )
    expect(client.getQueryData(['admin', 'crawl-runs'])).toBeUndefined()
  })
})
