import { useEffect, useRef, useState } from 'react'
import { useI18n } from '../../shared/i18n/context'
import { LanguageSelector } from '../../shared/i18n/LanguageSelector'
import { locales } from '../../shared/i18n/messages'
import { ItemCard } from './ItemCard'
import { toItemCard } from './itemCardData'
import { ParsedItemDetails } from './ParsedItemDetails'
import { useItemTextImport } from './useItemTextImport'
import { CurrencyImage } from './CurrencyImage'
import { currencies } from './currencies'
import { materials, materialTabs } from './materials'
import type { Material, MaterialTab } from './materials'
import { useItemDraft } from './draft'
import './crafting.css'

type Selection = { id: string; name: string; image: string }

export function CraftingPage() {
  const draft = useItemDraft()
  const { t, locale } = useI18n()
  const imported = useItemTextImport()
  const [activeTab, setActiveTab] = useState<MaterialTab>('Currency')
  const [selected, setSelected] = useState<Selection | null>(null)
  const [held, setHeld] = useState<Material | null>(null)
  const [favorites, setFavorites] = useState<(Material | null)[]>(
    Array(15).fill(null),
  )
  const [pointer, setPointer] = useState<{ x: number; y: number } | null>(null)
  const [inputMode, setInputMode] = useState<'base' | 'text'>('base')
  const [inputOpen, setInputOpen] = useState(false)
  const [announcement, setAnnouncement] = useState('')
  const inputToggle = useRef<HTMLButtonElement>(null)
  const inputClose = useRef<HTMLButtonElement>(null)

  useEffect(() => {
    if (inputOpen) inputClose.current?.focus()
  }, [inputOpen])
  useEffect(() => {
    const cancel = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setSelected(null)
        setHeld(null)
        setPointer(null)
        setInputOpen(false)
        if (inputOpen) inputToggle.current?.focus()
        setAnnouncement(t('noticeCleared'))
      }
    }
    const hide = () => setPointer(null)
    window.addEventListener('keydown', cancel)
    window.addEventListener('blur', hide)
    return () => {
      window.removeEventListener('keydown', cancel)
      window.removeEventListener('blur', hide)
    }
  }, [inputOpen, t])

  function choose(resource: Selection) {
    setSelected(resource)
    setHeld(null)
    setAnnouncement(t('noticeSelected', { name: resource.name }))
  }
  function apply() {
    setAnnouncement(t(selected ? 'noticePreview' : 'noticeSelectFirst'))
  }
  function importText() {
    imported.submit(draft.text)
    setSelected(null)
    setHeld(null)
  }
  function placeFavorite(index: number) {
    if (!held) {
      const resource = favorites[index]
      if (resource) choose(resource)
      return
    }
    setFavorites((current) =>
      current.map((resource, position) =>
        position === index ? held : resource,
      ),
    )
    setAnnouncement(t('favoritePlaced', { name: held.name, slot: index + 1 }))
    setHeld(null)
  }

  const item = draft.source === 'text' ? imported.data : undefined
  const displayedName =
    draft.source === 'base'
      ? t('solarAmulet')
      : (item?.displayName ?? t('importedItem'))
  const currentMaterials = materials.filter(
    (material) => material.category === activeTab,
  )
  const cursor = held ?? selected
  const card =
    draft.source === 'base'
      ? {
          rarity: 'NORMAL' as const,
          name: t('solarAmulet'),
          base: t('solarAmulet'),
          itemClass: t('amulet'),
          itemLevel: null,
          properties: [],
          requirements: [],
          modifiers: [],
          flags: [],
        }
      : item
        ? toItemCard(item)
        : null

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
          <div className="panel-heading stash-heading">
            <div
              role="tablist"
              aria-label={t('materialTabs')}
              className="material-tabs"
            >
              {materialTabs.map((tab, index) => (
                <button
                  key={tab.id}
                  type="button"
                  role="tab"
                  id={`tab-${tab.id}`}
                  aria-selected={activeTab === tab.id}
                  aria-controls="material-list"
                  tabIndex={activeTab === tab.id ? 0 : -1}
                  onClick={() => setActiveTab(tab.id)}
                  onKeyDown={(event) => {
                    const next =
                      event.key === 'ArrowRight'
                        ? (index + 1) % materialTabs.length
                        : event.key === 'ArrowLeft'
                          ? (index + materialTabs.length - 1) %
                            materialTabs.length
                          : event.key === 'Home'
                            ? 0
                            : event.key === 'End'
                              ? materialTabs.length - 1
                              : null
                    if (next === null) return
                    event.preventDefault()
                    const target = materialTabs[next]!
                    setActiveTab(target.id)
                    document.getElementById(`tab-${target.id}`)?.focus()
                  }}
                >
                  {tab.label}
                </button>
              ))}
            </div>
            <span>
              {t('materialCount', {
                count:
                  activeTab === 'Currency'
                    ? currencies.length
                    : currentMaterials.length,
              })}
            </span>
          </div>
          <div className="stash-board">
            <div className="stash-canvas" aria-label={t('stash')}>
              <div
                id="material-list"
                role="tabpanel"
                aria-labelledby={`tab-${activeTab}`}
                className={
                  activeTab === 'Currency'
                    ? 'currency-catalog'
                    : 'material-catalog'
                }
              >
                {activeTab === 'Currency'
                  ? currencies.map((currency) => {
                      const name = locales[locale].currencies[currency.id]
                      return (
                        <button
                          key={currency.id}
                          type="button"
                          className={`currency-slot ${selected?.id === currency.id ? 'is-selected' : ''}`}
                          style={{
                            left: `${currency.x / 9.35}%`,
                            top: `${currency.y / 5.5}%`,
                          }}
                          aria-label={name}
                          aria-pressed={selected?.id === currency.id}
                          title={t('rightSelect', { name })}
                          onContextMenu={(event) => {
                            event.preventDefault()
                            setPointer({ x: event.clientX, y: event.clientY })
                            choose({ ...currency, name })
                          }}
                          onClick={(event) => {
                            if (event.detail === 0) setPointer(null)
                            choose({ ...currency, name })
                          }}
                        >
                          <CurrencyImage {...currency} name={name} />
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
                      )
                    })
                  : currentMaterials.map((material) => (
                      <button
                        type="button"
                        key={material.id}
                        className={`material-entry ${held?.id === material.id ? 'is-held' : ''}`}
                        aria-label={material.name}
                        aria-pressed={held?.id === material.id}
                        title={t('pickMaterial', { name: material.name })}
                        onClick={(event) => {
                          if (event.detail === 0) setPointer(null)
                          setHeld(material)
                          setSelected(null)
                          setAnnouncement(
                            t('materialHeld', { name: material.name }),
                          )
                        }}
                      >
                        <span className="material-entry__image">
                          <CurrencyImage key={material.id} {...material} />
                        </span>
                        <span>{material.name}</span>
                      </button>
                    ))}
              </div>
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
                <button
                  type="button"
                  ref={inputToggle}
                  className="item-input-toggle"
                  aria-expanded={inputOpen}
                  aria-controls="item-input-panel"
                  onClick={() => setInputOpen((open) => !open)}
                >
                  {t('editItem')}
                </button>
              </div>
              <div
                role="group"
                aria-label={t('favorites')}
                className={`favorite-slots ${held ? 'is-placing' : ''}`}
              >
                {favorites.map((resource, index) => (
                  <button
                    type="button"
                    key={index}
                    className={`currency-slot favorite-slot ${resource && selected?.id === resource.id ? 'is-selected' : ''}`}
                    style={{
                      left: `${(652 + (index % 3) * 90) / 9.35}%`,
                      top: `${(40 + Math.floor(index / 3) * 100) / 5.5}%`,
                    }}
                    aria-label={t(
                      resource ? 'favoriteNamed' : 'favoriteEmpty',
                      { slot: index + 1, name: resource?.name ?? '' },
                    )}
                    aria-pressed={Boolean(
                      resource && selected?.id === resource.id,
                    )}
                    title={t(
                      held
                        ? 'placeFavorite'
                        : resource
                          ? 'useFavorite'
                          : 'emptyFavorite',
                    )}
                    onClick={() => placeFavorite(index)}
                    onContextMenu={(event) => {
                      event.preventDefault()
                      if (resource) {
                        setPointer({ x: event.clientX, y: event.clientY })
                        choose(resource)
                      }
                    }}
                  >
                    {resource ? (
                      <CurrencyImage key={resource.id} {...resource} />
                    ) : (
                      <span className="favorite-empty" aria-hidden="true">
                        +
                      </span>
                    )}
                  </button>
                ))}
              </div>
            </div>
            <div className="bench-lower">
              <div className="bench-item-card">
                {card && <ItemCard item={card} />}
              </div>
              <div aria-hidden="true" />
            </div>
          </div>
          <section
            id="item-input-panel"
            className="item-input-panel detail-body"
            hidden={!inputOpen}
            aria-label={t('startItem')}
            aria-busy={imported.pending}
          >
            <button
              ref={inputClose}
              type="button"
              className="input-close"
              onClick={() => {
                setInputOpen(false)
                inputToggle.current?.focus()
              }}
            >
              {t('closeInput')}
            </button>
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

            {item && <ParsedItemDetails item={item} />}
          </section>
        </div>
      </div>
      <span className="sr-only" role="status">
        {imported.pending ? t('analyzing') : announcement}
      </span>
      <footer className="craft-footer">
        <span>
          EXILE HEPHAISTOS <span aria-hidden="true">/</span>{' '}
          {t('personalWorkbench')}
        </span>
        <a
          href="https://poe2db.tw/us/Currency"
          target="_blank"
          rel="noreferrer"
        >
          {t('imageCredit')}
        </a>
      </footer>
      {cursor && pointer && (
        <span
          className="currency-cursor"
          aria-hidden="true"
          style={{ left: pointer.x + 14, top: pointer.y + 14 }}
        >
          <CurrencyImage key={cursor.id} {...cursor} />
        </span>
      )}
    </main>
  )
}
