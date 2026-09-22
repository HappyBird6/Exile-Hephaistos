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
})

function show() {
  return render(
    <QueryClientProvider client={client}>
      <AdminCrawlingPage />
    </QueryClientProvider>,
  )
}

function mutations(method: string) {
  return requests.filter((entry) => entry.init.method === method)
}

describe('크롤링 관리자', () => {
  it('계정 미설정 시 설정과 실행 API에 접근하지 않는다', async () => {
    backend.configured = false
    backend.authenticated = false
    show()
    expect(
      await screen.findByRole('heading', { name: '관리자 계정 미설정' }),
    ).toBeVisible()
    expect(requests.every((entry) => entry.path.endsWith('/session'))).toBe(
      true,
    )
    expect(
      screen.queryByRole('button', { name: '크롤링 시작' }),
    ).not.toBeInTheDocument()
  })

  it('비밀번호를 저장하지 않고 CSRF와 함께 로그인한 뒤 관리 화면을 연다', async () => {
    backend.authenticated = false
    show()
    fireEvent.change(await screen.findByLabelText('아이디'), {
      target: { value: 'operator' },
    })
    fireEvent.change(screen.getByLabelText('비밀번호'), {
      target: { value: 'synthetic-secret' },
    })
    fireEvent.click(screen.getByRole('button', { name: '로그인' }))
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

  it('로그인 실패를 표시하며 비밀번호 입력을 비운다', async () => {
    backend.authenticated = false
    backend.loginStatus = 401
    show()
    fireEvent.change(await screen.findByLabelText('아이디'), {
      target: { value: 'operator' },
    })
    fireEvent.change(screen.getByLabelText('비밀번호'), {
      target: { value: 'incorrect' },
    })
    fireEvent.click(screen.getByRole('button', { name: '로그인' }))
    expect(await screen.findByRole('alert')).toHaveTextContent(
      '로그인 정보가 올바르지 않습니다',
    )
    expect(screen.getByLabelText('비밀번호')).toHaveValue('')
    expect(screen.queryByLabelText('페이지 주소')).not.toBeInTheDocument()
  })

  it('페이지를 열기만 해서는 수집을 시작하지 않는다', async () => {
    show()
    expect(await screen.findByDisplayValue('화폐')).toBeVisible()
    expect(mutations('POST')).toHaveLength(0)
    expect(
      screen.getByText(/데이터 정제·변경 비교·최신화는 이후 단계/),
    ).toBeVisible()
  })

  it('편집 중 실행을 막고 서버에 version과 변경 대상을 저장한다', async () => {
    show()
    fireEvent.change(await screen.findByLabelText('이름'), {
      target: { value: '화폐 목록' },
    })
    expect(screen.getByRole('button', { name: '크롤링 시작' })).toBeDisabled()
    backend.settings.targets[0] = {
      ...initialSettings.targets[0]!,
      name: '화폐 목록',
    }
    fireEvent.click(screen.getByRole('button', { name: '대상 설정 저장' }))
    await waitFor(() =>
      expect(
        screen.getByText('대상 설정을 서버에 저장했습니다.'),
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
    expect(screen.getByRole('button', { name: '크롤링 시작' })).toBeEnabled()
  })

  it('버전 충돌 시 입력을 유지하고 명시적 새로고침으로 같은 버전의 입력도 취소한다', async () => {
    backend.saveStatus = 409
    show()
    fireEvent.change(await screen.findByLabelText('이름'), {
      target: { value: '미저장 값' },
    })
    fireEvent.click(screen.getByRole('button', { name: '대상 설정 저장' }))
    expect(await screen.findByRole('alert')).toHaveTextContent(
      '다른 변경 또는 진행 중인 작업',
    )
    expect(screen.getByLabelText('이름')).toHaveValue('미저장 값')
    fireEvent.click(
      screen.getByRole('button', { name: /서버 설정 다시 불러오기/ }),
    )
    await waitFor(() =>
      expect(screen.getByLabelText('이름')).toHaveValue('화폐'),
    )
  })

  it('허용되지 않은 주소와 중복 주소를 서버에 저장하지 않는다', async () => {
    show()
    fireEvent.change(await screen.findByLabelText('페이지 주소'), {
      target: { value: 'https://example.invalid/private' },
    })
    fireEvent.click(screen.getByRole('button', { name: '대상 설정 저장' }))
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'poe2db 공개 페이지 주소',
    )
    expect(mutations('PUT')).toHaveLength(0)
    fireEvent.change(screen.getByLabelText('페이지 주소'), {
      target: { value: 'https://poe2db.tw/us/Currency' },
    })
    fireEvent.click(screen.getByRole('button', { name: '대상 추가' }))
    const row = screen.getByRole('group', { name: '대상 2' })
    fireEvent.change(within(row).getByLabelText('이름'), {
      target: { value: '중복' },
    })
    fireEvent.change(within(row).getByLabelText('페이지 주소'), {
      target: { value: 'https://poe2db.tw/us/Currency' },
    })
    fireEvent.click(screen.getByRole('button', { name: '대상 설정 저장' }))
    expect(await screen.findByRole('alert')).toHaveTextContent(
      '중복으로 등록할 수 없습니다',
    )
    expect(mutations('PUT')).toHaveLength(0)
  })

  it('실행 요청 후 진행 상태를 보여주며 중복 실행을 막는다', async () => {
    show()
    await waitFor(() =>
      expect(screen.getByRole('button', { name: '크롤링 시작' })).toBeEnabled(),
    )
    fireEvent.click(screen.getByRole('button', { name: '크롤링 시작' }))
    expect(await screen.findByText('실행 대기')).toBeVisible()
    expect(screen.getByRole('button', { name: '수집 진행 중' })).toBeDisabled()
    const starts = mutations('POST').filter((entry) =>
      entry.path.endsWith('/crawl-runs'),
    )
    expect(starts).toHaveLength(1)
    expect(starts[0]?.init.headers).toEqual({
      'X-CSRF-TOKEN': 'authenticated-csrf',
    })
  })

  it('실행기가 꺼져 있으면 크롤링 버튼을 비활성화한다', async () => {
    backend.settings.runnerEnabled = false
    show()
    expect(
      await screen.findByText('서버의 수집 실행기가 비활성화되어 있습니다.'),
    ).toBeVisible()
    expect(screen.getByRole('button', { name: '크롤링 시작' })).toBeDisabled()
  })

  it('원본 수집 성공과 실패를 구분하고 누락된 응답 수를 0으로 표시하지 않는다', async () => {
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
    expect(await screen.findByText('원본 수집 완료')).toBeVisible()
    expect(screen.getByText('수집 실패')).toBeVisible()
    expect(screen.getByText('오류 코드: HTTP_403')).toBeVisible()
    expect(screen.getByText('3개')).toBeVisible()
    expect(screen.getByText('—')).toBeVisible()
  })

  it('로그아웃 후 관리자 데이터 캐시와 화면을 비운다', async () => {
    show()
    expect(await screen.findByDisplayValue('화폐')).toBeVisible()
    fireEvent.click(screen.getByRole('button', { name: '로그아웃' }))
    expect(
      await screen.findByRole('heading', { name: '관리자 로그인' }),
    ).toBeVisible()
    expect(screen.queryByDisplayValue('화폐')).not.toBeInTheDocument()
    await waitFor(() =>
      expect(client.getQueryData(['admin', 'crawl-settings'])).toBeUndefined(),
    )
    expect(client.getQueryData(['admin', 'crawl-runs'])).toBeUndefined()
  })
})
