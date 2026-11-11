import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import './admin.css'
type Dashboard = {
  observedAt: string
  uptimeSeconds: number
  requests: number
  serverErrors: number
  rejectedRequests: number
  meanLatencyMs: number
  recentMaxLatencyMs: number
  heapUsedBytes: number
  heapMaxBytes: number
  processors: number
}
const sections = [{ id: 'dashboard', label: '대시보드', icon: '▦' }] as const
export function AdminPage() {
  const [password, setPassword] = useState(''),
    [credential, setCredential] = useState('')
  const data = useQuery({
    queryKey: ['admin-dashboard', credential],
    enabled: !!credential,
    retry: false,
    refetchInterval: 30000,
    refetchIntervalInBackground: false,
    gcTime: 0,
    queryFn: async ({ signal }) => {
      const response = await fetch('/api/admin/dashboard', {
        signal,
        cache: 'no-store',
        headers: { Authorization: credential },
      })
      if (!response.ok)
        throw new Error(
          response.status === 401
            ? '비밀번호를 확인해주세요.'
            : response.status === 404
              ? '관리자 페이지가 활성화되지 않았습니다.'
              : '운영 정보를 불러오지 못했습니다.',
        )
      return (await response.json()) as Dashboard
    },
  })
  const dashboard = data.data
  return (
    <main className="admin-shell">
      <aside className="admin-sidebar">
        <a href="/">EXILE HEPHAISTOS</a>
        <small>사이트 관리</small>
        <nav aria-label="관리 메뉴">
          {sections.map((section) => (
            <button key={section.id} aria-current="page">
              <span aria-hidden="true">{section.icon}</span> {section.label}
            </button>
          ))}
        </nav>
      </aside>
      <section className="admin-main">
        <header>
          <div>
            <p>사이트 운영</p>
            <h1>대시보드</h1>
          </div>
          {credential && (
            <button
              onClick={() => {
                setCredential('')
                setPassword('')
              }}
            >
              잠금
            </button>
          )}
        </header>
        {!dashboard ? (
          <form
            className="admin-login"
            onSubmit={(event) => {
              event.preventDefault()
              setCredential(
                'Basic ' +
                  btoa(unescape(encodeURIComponent('admin:' + password))),
              )
              setPassword('')
            }}
          >
            <h2>관리자 로그인</h2>
            <label>
              비밀번호
              <input
                type="password"
                autoComplete="current-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
            </label>
            <button disabled={data.isFetching}>로그인</button>
            {data.error && <p role="alert">{data.error.message}</p>}
          </form>
        ) : (
          <>
            <div className="admin-status">
              <span>● 운영 정보 연결됨</span>
              <span>
                마지막 확인 {new Date(dashboard.observedAt).toLocaleString()}
              </span>
              <button
                disabled={data.isFetching}
                onClick={() => void data.refetch()}
              >
                새로고침
              </button>
            </div>
            {data.error && <p role="alert">{data.error.message}</p>}
            <div className="admin-grid">
              {[
                [
                  'API 요청',
                  dashboard.requests.toLocaleString(),
                  '서버 시작 이후 처리한 API 요청',
                ],
                [
                  '서버 오류',
                  dashboard.serverErrors.toLocaleString(),
                  '서버에서 처리하지 못한 요청 (5xx)',
                ],
                [
                  '요청 제한',
                  dashboard.rejectedRequests.toLocaleString(),
                  '혼잡 또는 호출 제한으로 거절한 요청',
                ],
                [
                  '평균 응답 시간',
                  Math.round(dashboard.meanLatencyMs) + ' ms',
                  '서버 시작 이후 API 처리 시간',
                ],
                [
                  '최근 최대 응답 시간',
                  Math.round(dashboard.recentMaxLatencyMs) + ' ms',
                  '최근 측정 구간의 가장 느린 응답',
                ],
                [
                  '메모리 사용',
                  Math.round(dashboard.heapUsedBytes / 1048576) + ' MB',
                  '최대 ' +
                    Math.round(dashboard.heapMaxBytes / 1048576) +
                    ' MB',
                ],
                [
                  '가동 시간',
                  Math.floor(dashboard.uptimeSeconds / 3600) +
                    '시간 ' +
                    Math.floor((dashboard.uptimeSeconds % 3600) / 60) +
                    '분',
                  '현재 서버 프로세스의 연속 실행 시간',
                ],
                [
                  '처리 자원',
                  dashboard.processors + ' CPU',
                  '서버에서 확인되는 가용 프로세서',
                ],
              ].map(([label, value, description]) => (
                <article className="admin-card" key={label}>
                  <h2>{label}</h2>
                  <strong>{value}</strong>
                  <p>{description}</p>
                </article>
              ))}
            </div>
            <p className="admin-note">
              현재 서버 인스턴스 기준입니다. API 요청 수는 방문자 수와 다르며,
              서버를 다시 시작하면 누적 지표가 초기화됩니다.
            </p>
          </>
        )}
      </section>
    </main>
  )
}
