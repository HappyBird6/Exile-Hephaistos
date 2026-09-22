import { useEffect, useState } from 'react'
import { CurrencyImage } from './CurrencyImage'
import { currencies } from './currencies'
import { useItemDraft } from './draft'
import './crafting.css'

type Currency = (typeof currencies)[number]

export function CraftingPage() {
  const draft = useItemDraft()
  const [selected, setSelected] = useState<Currency | null>(null)
  const [hovered, setHovered] = useState<Currency | null>(null)
  const [pointer, setPointer] = useState<{ x: number; y: number } | null>(null)
  const [inputMode, setInputMode] = useState<'base' | 'text'>('base')
  const [text, setText] = useState('')
  const [error, setError] = useState('')
  const [notice, setNotice] = useState(
    '화폐를 선택하고 중앙의 아이템을 클릭하세요.',
  )
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    const cancel = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setSelected(null)
        setPointer(null)
        setNotice('화폐 선택을 해제했습니다.')
      }
    }
    const hide = () => setPointer(null)
    window.addEventListener('keydown', cancel)
    window.addEventListener('blur', hide)
    return () => {
      window.removeEventListener('keydown', cancel)
      window.removeEventListener('blur', hide)
    }
  }, [])

  function choose(currency: Currency) {
    setSelected(currency)
    setNotice(`${currency.name} 선택 · 중앙 아이템을 클릭하세요.`)
  }

  function apply() {
    if (!selected) {
      setNotice('먼저 창고에서 화폐를 선택하세요.')
      return
    }
    setAttempt((value) => value + 1)
    setNotice(
      `${selected.name} 사용 요청 · 제작 효과 미연결. 아이템은 변경되지 않았습니다.`,
    )
  }

  function importText() {
    if (!text.trim()) {
      setError('게임에서 복사한 아이템 텍스트를 입력해 주세요.')
      return
    }
    draft.setText(text)
    setSelected(null)
    setError('')
    setNotice(
      '복사한 원문을 배치했습니다. 베이스와 옵션은 아직 검증되지 않았습니다.',
    )
  }

  const inspected = hovered ?? selected
  return (
    <main
      className="craft-page"
      onPointerMove={(event) => {
        if (event.pointerType !== 'touch')
          setPointer({ x: event.clientX, y: event.clientY })
      }}
      onPointerLeave={() => setPointer(null)}
    >
      <header className="craft-header">
        <a className="craft-brand" href="/" aria-label="Exile Hephaistos 홈">
          <span className="brand-mark" aria-hidden="true">
            H
          </span>
          <span>
            EXILE <b>HEPHAISTOS</b>
            <small>PATH OF EXILE 2 · CRAFTING WORKBENCH</small>
          </span>
        </a>
        <nav aria-label="주 메뉴">
          <span aria-current="page">제작 작업대</span>
          <a href="/admin">관리자</a>
        </nav>
      </header>
      <div className="craft-title">
        <div>
          <p className="craft-kicker">THE CRAFTING BENCH</p>
          <h1>제작 작업대</h1>
          <p>화폐를 고르고, 아이템의 다음 가능성을 준비하세요.</p>
        </div>
        <span className="preview-badge">
          <i />
          인터랙션 프리뷰
        </span>
      </div>
      <div className="workbench-layout">
        <div className="stash-panel">
          <div className="panel-heading">
            <h2>
              화폐 창고 <span>CURRENCY STASH</span>
            </h2>
            <span>{currencies.length}종</span>
          </div>
          <div className="stash-canvas" aria-label="화폐 창고">
            <div className="stash-tier-labels" aria-hidden="true">
              <span>일반</span>
              <span>상위</span>
              <span>완벽</span>
            </div>
            {currencies.map((currency) => (
              <button
                key={currency.id}
                type="button"
                className={`currency-slot ${selected?.id === currency.id ? 'is-selected' : ''}`}
                style={{
                  left: `${currency.x / 9.35}%`,
                  top: `${currency.y / 9.35}%`,
                }}
                aria-label={currency.name}
                aria-pressed={selected?.id === currency.id}
                title={`${currency.name} · 우클릭으로 선택`}
                onContextMenu={(event) => {
                  event.preventDefault()
                  setPointer({ x: event.clientX, y: event.clientY })
                  choose(currency)
                }}
                onClick={(event) => {
                  if (event.detail === 0) setPointer(null)
                  choose(currency)
                }}
                onPointerEnter={() => setHovered(currency)}
                onPointerLeave={() => setHovered(null)}
                onFocus={() => setHovered(currency)}
                onBlur={() => setHovered(null)}
              >
                <CurrencyImage {...currency} />
                {currency.id.startsWith('Greater') && (
                  <span className="currency-tier" aria-hidden="true">
                    II
                  </span>
                )}
                {currency.id.startsWith('Perfect') && (
                  <span className="currency-tier" aria-hidden="true">
                    III
                  </span>
                )}
              </button>
            ))}
            <div className="item-placement">
              <span className="placement-label">제작 아이템</span>
              <button
                className={`item-slot ${selected ? 'is-ready' : ''}`}
                type="button"
                aria-label="중앙 아이템에 선택한 화폐 사용"
                onClick={apply}
              >
                {draft.source === 'base' ? (
                  <img
                    src="/assets/currency/solar-amulet.webp"
                    alt="태양의 목걸이"
                    draggable="false"
                  />
                ) : (
                  <span className="text-item-symbol" aria-hidden="true">
                    ≡
                  </span>
                )}
                <span>
                  {draft.source === 'base' ? '태양의 목걸이' : '복사한 아이템'}
                </span>
              </button>
              <span className="placement-hint">
                {selected ? '좌클릭으로 사용 요청' : '화폐 선택 후 클릭'}
              </span>
            </div>
            <div className="stash-inspector">
              <span className="inspector-rule" />
              <strong>{inspected?.name ?? '당신의 다음 한 수'}</strong>
              <p>
                {inspected
                  ? '선택 후 중앙 아이템을 클릭하세요.'
                  : '화폐 위에 마우스를 올려 확인하세요.'}
              </p>
              <span className="inspector-rule" />
            </div>
          </div>
          <div className="stash-controls">
            <span>
              <kbd>우클릭</kbd> 화폐 선택
            </span>
            <span>
              <kbd>좌클릭</kbd> 아이템에 사용
            </span>
            <button
              type="button"
              onClick={() => {
                setSelected(null)
                setPointer(null)
                setNotice('화폐 선택을 해제했습니다.')
              }}
              disabled={!selected}
            >
              <kbd>Esc</kbd> 선택 해제
            </button>
          </div>
        </div>
        <aside className="item-panel" aria-label="아이템 상세정보">
          <div className="panel-heading">
            <h2>아이템 상세정보</h2>
            <span>AMULET</span>
          </div>
          <div className="item-summary">
            <span className="craft-kicker">
              {draft.source === 'base'
                ? '목걸이 · 기본 베이스'
                : '가져온 원문 · 미검증'}
            </span>
            <h2>
              {draft.source === 'base' ? '태양의 목걸이' : '복사한 아이템'}
            </h2>
            <span className="item-subtitle">
              {draft.source === 'base' ? 'Solar Amulet' : 'Imported item'}
            </span>
          </div>
          <div className="detail-body">
            {draft.source === 'base' ? (
              <>
                <div className="amulet-preview">
                  <img src="/assets/currency/solar-amulet.webp" alt="" />
                </div>
                <dl>
                  <div>
                    <dt>장비 유형</dt>
                    <dd>목걸이</dd>
                  </div>
                  <div>
                    <dt>베이스</dt>
                    <dd>태양의 목걸이</dd>
                  </div>
                </dl>
                <p className="detail-note">
                  아이템 레벨과 옵션은 아직 설정되지 않았습니다.
                </p>
              </>
            ) : (
              <>
                <p className="detail-note">
                  입력 원문을 그대로 보존합니다. 아이템 종류와 옵션 해석은 아직
                  지원하지 않습니다.
                </p>
                <pre className="item-raw">{draft.text}</pre>
              </>
            )}
            <div className="input-heading">
              <h3>시작 아이템</h3>
              <span>01</span>
            </div>
            <div
              className="input-tabs"
              role="group"
              aria-label="아이템 입력 방식"
            >
              <button
                type="button"
                aria-pressed={inputMode === 'base'}
                onClick={() => setInputMode('base')}
              >
                베이스 선택
              </button>
              <button
                type="button"
                aria-pressed={inputMode === 'text'}
                onClick={() => setInputMode('text')}
              >
                아이템 텍스트
              </button>
            </div>
            {inputMode === 'base' ? (
              <div className="base-form">
                <label htmlFor="base-select">목걸이 베이스</label>
                <select id="base-select" defaultValue="solar">
                  <option value="solar">태양의 목걸이</option>
                </select>
                <button
                  type="button"
                  className="primary-action"
                  onClick={() => {
                    draft.setBase()
                    setSelected(null)
                    setNotice('태양의 목걸이 베이스를 배치했습니다.')
                  }}
                >
                  베이스 배치 <span aria-hidden="true">↗</span>
                </button>
              </div>
            ) : (
              <div className="text-form">
                <label htmlFor="item-text">게임에서 복사한 아이템 텍스트</label>
                <textarea
                  id="item-text"
                  value={text}
                  maxLength={20000}
                  onChange={(event) => setText(event.target.value)}
                  placeholder={
                    '아이템에 마우스를 올리고 Ctrl+C\n복사한 텍스트를 여기에 붙여넣으세요.'
                  }
                  aria-describedby={error ? 'import-error' : undefined}
                />
                {error && (
                  <p id="import-error" role="alert">
                    {error}
                  </p>
                )}
                <button
                  type="button"
                  className="primary-action"
                  onClick={importText}
                >
                  원문 배치 <span aria-hidden="true">↗</span>
                </button>
              </div>
            )}
          </div>
          <div className="engine-note">
            <span aria-hidden="true">◇</span>
            <p>
              <strong>제작 효과 연결 준비 중</strong>현재는 선택과 사용
              인터랙션만 제공합니다. 화폐 소모, 옵션 변경, 확률 계산은 수행하지
              않습니다.
            </p>
          </div>
        </aside>
      </div>
      <div
        className="craft-status"
        role="status"
        aria-live="polite"
        key={attempt}
      >
        <span className="status-dot" />
        {notice}
      </div>
      <footer className="craft-footer">
        <span>
          EXILE HEPHAISTOS <span aria-hidden="true">/</span> 나만의 제작 작업대
        </span>
        <a
          href="https://poe2db.tw/kr/Currency"
          target="_blank"
          rel="noreferrer"
        >
          화폐 이미지 · PoE2DB ↗
        </a>
      </footer>
      {selected && pointer && (
        <span
          className="currency-cursor"
          aria-hidden="true"
          style={{ left: pointer.x + 14, top: pointer.y + 14 }}
        >
          <CurrencyImage key={selected.id} {...selected} />
        </span>
      )}
    </main>
  )
}
