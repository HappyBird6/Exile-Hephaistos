import { AdminCrawlingPage } from '../features/admin-crawling/AdminCrawlingPage'

export function App() {
  if (
    window.location.pathname === '/admin' ||
    window.location.pathname === '/admin/crawling'
  ) {
    return <AdminCrawlingPage />
  }
  return (
    <main>
      <p className="eyebrow">POE2 · 제작 의사결정 지원</p>
      <h1>Exile Hephaistos</h1>
      <p className="intro">
        아이템 제작의 확률과 위험, 비용을 비교하고 직접 선택하세요.
      </p>
      <section aria-labelledby="availability-heading">
        <h2 id="availability-heading">제작 계산 준비 중</h2>
        <p>
          현재는 개발 환경만 구성되어 있습니다. 검증된 게임 데이터와 Crafting
          Engine이 연결되기 전까지 제작 계산을 제공하지 않습니다.
        </p>
        <p>AI 목표 초안, 가격 조회, 로그인 기능도 아직 지원하지 않습니다.</p>
      </section>
      <p>
        <a className="admin-link" href="/admin">
          관리자 페이지
        </a>
      </p>
    </main>
  )
}
