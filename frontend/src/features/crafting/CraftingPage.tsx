import { useEffect, useState } from 'react'
import { useI18n } from '../../shared/i18n/context'
import { LanguageSelector } from '../../shared/i18n/LanguageSelector'
import { locales } from '../../shared/i18n/messages'
import type { MessageKey } from '../../shared/i18n/messages'
import { ItemCard } from './ItemCard'
import { ParsedItemDetails } from './ParsedItemDetails'
import { useItemTextImport } from './useItemTextImport'
import { CurrencyImage } from './CurrencyImage'
import { currencies } from './currencies'
import { useItemDraft } from './draft'
import './crafting.css'

type Currency = (typeof currencies)[number]

export function CraftingPage() {
  const draft = useItemDraft()
  const { t, locale } = useI18n()
  const imported = useItemTextImport()
  const currencyName = (currency: Currency) =>
    locales[locale].currencies[currency.id]
  const [selected, setSelected] = useState<Currency | null>(null)
  const [hovered, setHovered] = useState<Currency | null>(null)
  const [pointer, setPointer] = useState<{ x: number; y: number } | null>(null)
  const [inputMode, setInputMode] = useState<'base' | 'text'>('base')
  const [notice, setNotice] = useState<MessageKey>('noticeInitial')
  const [noticeCurrency, setNoticeCurrency] = useState<Currency | null>(null)
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    const cancel = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setSelected(null)
        setPointer(null)
        setNotice('noticeCleared')
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
    setNoticeCurrency(currency)
    setNotice('noticeSelected')
  }

  function apply() {
    if (!selected) {
      setNotice('noticeSelectFirst')
      return
    }
    setAttempt((value) => value + 1)
    setNoticeCurrency(selected)
    setNotice('noticeApply')
  }

  function importText() {
    imported.submit(draft.text)
    setNotice('noticeParsed')
    setSelected(null)
  }

  const item = draft.source === 'text' ? imported.data : undefined
  const displayedName =
    draft.source === 'base'
      ? t('solarAmulet')
      : (item?.displayName ?? t('importedItem'))
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
        <a className="craft-brand" href="/" aria-label={t('home')}>
          <span className="brand-mark" aria-hidden="true">
            H
          </span>
          <span>
            EXILE <b>HEPHAISTOS</b>
            <small>PATH OF EXILE 2 · CRAFTING WORKBENCH</small>
          </span>
        </a>
        <nav aria-label={t('navigation')}>
          <span aria-current="page">{t('workbench')}</span>
          <LanguageSelector />
          <a href="/admin">{t('admin')}</a>
        </nav>
      </header>
      <div className="craft-title">
        <div>
          <p className="craft-kicker">THE CRAFTING BENCH</p>
          <h1>{t('workbench')}</h1>
          <p>{t('subtitle')}</p>
        </div>
        <span className="preview-badge">
          <i />
          {t('preview')}
        </span>
      </div>
      <div className="workbench-layout">
        <div className="stash-panel">
          <div className="panel-heading">
            <h2>
              {t('stash')} <span>CURRENCY STASH</span>
            </h2>
            <span>{t('currencyCount', { count: currencies.length })}</span>
          </div>
          <div className="stash-canvas" aria-label={t('stash')}>
            <div className="stash-tier-labels" aria-hidden="true">
              <span>{t('normal')}</span>
              <span>{t('greater')}</span>
              <span>{t('perfect')}</span>
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
                aria-label={currencyName(currency)}
                aria-pressed={selected?.id === currency.id}
                title={t('rightSelect', { name: currencyName(currency) })}
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
                <CurrencyImage {...currency} name={currencyName(currency)} />
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
              <span className="placement-label">{t('craftingItem')}</span>
              <button
                className={`item-slot ${selected ? 'is-ready' : ''}`}
                type="button"
                aria-label={t('applyCurrency')}
                onClick={apply}
              >
                {draft.source === 'base' ? (
                  <img
                    src="/assets/currency/solar-amulet.webp"
                    alt={t('solarAmulet')}
                    draggable="false"
                  />
                ) : (
                  <span className="text-item-symbol" aria-hidden="true">
                    ≡
                  </span>
                )}
                <span>{displayedName}</span>
              </button>
              <span className="placement-hint">
                {selected ? t('clickApply') : t('clickAfterSelect')}
              </span>
            </div>
            <div className="stash-inspector">
              <span className="inspector-rule" />
              <strong>
                {inspected ? currencyName(inspected) : t('nextMove')}
              </strong>
              <p>{inspected ? t('selectThenClick') : t('hoverCurrency')}</p>
              <span className="inspector-rule" />
            </div>
          </div>
          <div className="stash-controls">
            <span>
              <kbd>{t('rightClick')}</kbd> {t('selectCurrency')}
            </span>
            <span>
              <kbd>{t('leftClick')}</kbd> {t('useOnItem')}
            </span>
            <button
              type="button"
              onClick={() => {
                setSelected(null)
                setPointer(null)
                setNotice('noticeCleared')
              }}
              disabled={!selected}
            >
              <kbd>Esc</kbd> {t('clearSelection')}
            </button>
          </div>
        </div>
        <aside className="item-panel" aria-label={t('itemDetails')}>
          <div className="panel-heading">
            <h2>{t('itemDetails')}</h2>
            <span>
              {draft.source === 'base' ? t('amulet') : (item?.itemClass ?? '—')}
            </span>
          </div>
          <div className="detail-body" aria-busy={imported.pending}>
            {draft.source === 'base' ? (
              <>
                <ItemCard
                  item={{
                    rarity: 'NORMAL',
                    name: t('solarAmulet'),
                    base: t('solarAmulet'),
                    itemClass: t('amulet'),
                    itemLevel: null,
                    properties: [],
                    requirements: [],
                    modifiers: [],
                    flags: [],
                  }}
                />
                <p className="detail-note">{t('baseUnset')}</p>
              </>
            ) : item ? (
              <ParsedItemDetails item={item} />
            ) : (
              <p className="detail-note">{t('textNotAnalyzed')}</p>
            )}
            <div className="input-heading">
              <h3>{t('startItem')}</h3>
              <span>01</span>
            </div>
            <div
              className="input-tabs"
              role="group"
              aria-label={t('inputMethod')}
            >
              <button
                type="button"
                aria-pressed={inputMode === 'base'}
                onClick={() => {
                  imported.invalidate()
                  setInputMode('base')
                }}
              >
                {t('baseSelect')}
              </button>
              <button
                type="button"
                aria-pressed={inputMode === 'text'}
                onClick={() => setInputMode('text')}
              >
                {t('itemText')}
              </button>
            </div>
            {inputMode === 'base' ? (
              <div className="base-form">
                <label htmlFor="base-select">{t('amuletBase')}</label>
                <select id="base-select" defaultValue="solar">
                  <option value="solar">{t('solarAmulet')}</option>
                </select>
                <button
                  type="button"
                  className="primary-action"
                  onClick={() => {
                    imported.invalidate()
                    draft.setBase()
                    setSelected(null)
                    setNotice('noticeBase')
                  }}
                >
                  {t('placeBase')} <span aria-hidden="true">↗</span>
                </button>
              </div>
            ) : (
              <div className="text-form">
                <label htmlFor="item-text">{t('pasteLabel')}</label>
                <textarea
                  id="item-text"
                  value={draft.text}
                  onChange={(event) => {
                    imported.invalidate()
                    draft.setText(event.target.value)
                    setNotice('noticeEdited')
                  }}
                  placeholder={t('pastePlaceholder')}
                  aria-invalid={Boolean(imported.error)}
                  aria-describedby={
                    imported.error
                      ? 'import-error item-text-help'
                      : 'item-text-help'
                  }
                />
                <p id="item-text-help" className="detail-note">
                  {t('inputLimit')}
                </p>
                {imported.error && (
                  <p id="import-error" role="alert">
                    {t(imported.error)}
                  </p>
                )}
                <button
                  type="button"
                  className="primary-action"
                  onClick={importText}
                  disabled={imported.pending}
                >
                  {t(imported.pending ? 'analyzing' : 'analyze')}{' '}
                  <span aria-hidden="true">↗</span>
                </button>
              </div>
            )}
          </div>
          <div className="engine-note">
            <span aria-hidden="true">◇</span>
            <p>
              <strong>{t('engineTitle')}</strong>
              {t('engineNote')}
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
        {imported.pending
          ? t('analyzing')
          : imported.error
            ? t('noticeParseFailed')
            : t(
                notice === 'noticeParsed' && !item ? 'textNotAnalyzed' : notice,
                { name: noticeCurrency ? currencyName(noticeCurrency) : '' },
              )}
      </div>
      <footer className="craft-footer">
        <span>
          EXILE HEPHAISTOS <span aria-hidden="true">/</span>{' '}
          {t('personalWorkbench')}
        </span>
        <a
          href="https://poe2db.tw/kr/Currency"
          target="_blank"
          rel="noreferrer"
        >
          {t('imageCredit')}
        </a>
      </footer>
      {selected && pointer && (
        <span
          className="currency-cursor"
          aria-hidden="true"
          style={{ left: pointer.x + 14, top: pointer.y + 14 }}
        >
          <CurrencyImage
            key={selected.id}
            {...selected}
            name={currencyName(selected)}
          />
        </span>
      )}
    </main>
  )
}
