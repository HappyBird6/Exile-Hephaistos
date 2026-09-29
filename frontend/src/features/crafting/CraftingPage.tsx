import { currencyNames } from './currencyNames'
import { useEffect, useRef, useState } from 'react'
import { ItemCard } from './ItemCard'
import { toItemCard } from './itemCardData'
import { MaterialTooltip } from './MaterialTooltip'
import { useItemTextImport } from './useItemTextImport'
import { CurrencyImage } from './CurrencyImage'
import { currencies } from './currencies'
import {
  materials,
  materialTabs,
  specialEssences,
  essenceRows,
} from './materials'
import type { Material, MaterialTab } from './materials'
import { useItemDraft } from './draft'
import { CraftingExplorer } from './CraftingExplorer'
import { currencyActions } from './craftingApi'
import type { Action } from './craftingApi'
import './crafting.css'

function tooltipEvents(id: string) {
  return { 'data-material-tooltip': id }
}

type Selection = { id: string; name: string; image: string }

export function CraftingPage() {
  const draft = useItemDraft()
  const imported = useItemTextImport()
  const dialog = useRef<HTMLDialogElement>(null)
  const [searches, setSearches] = useState<
    Partial<Record<MaterialTab, string>>
  >({})
  const [tooltipsEnabled, setTooltipsEnabled] = useState(true)
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
  const [baseLevel, setBaseLevel] = useState('82')
  const [previewRequest, setPreviewRequest] = useState<{
    count: number
    action: Action | null
  }>({ count: 0, action: null })
  const inputToggle = useRef<HTMLButtonElement>(null)
  const inputClose = useRef<HTMLButtonElement>(null)

  useEffect(() => {
    if (inputOpen) {
      dialog.current?.showModal()
      inputClose.current?.focus()
    } else dialog.current?.close()
  }, [inputOpen])
  useEffect(() => {
    const cancel = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setSelected(null)
        setHeld(null)
        setPointer(null)
        if (inputOpen)
          dialog.current?.dispatchEvent(
            new Event('cancel', { cancelable: true }),
          )
        if (inputOpen) inputToggle.current?.focus()
        setAnnouncement('Currency selection cleared.')
      }
    }
    const hide = () => setPointer(null)
    window.addEventListener('keydown', cancel)
    window.addEventListener('blur', hide)
    return () => {
      window.removeEventListener('keydown', cancel)
      window.removeEventListener('blur', hide)
    }
  }, [inputOpen])

  function choose(resource: Selection) {
    setSelected(resource)
    setHeld(null)
    setAnnouncement(`${resource.name} selected · Click the central item.`)
  }
  function apply() {
    if (!selected) {
      setAnnouncement('Select a currency from the stash first.')
      return
    }
    if (draft.source !== 'base') {
      setAnnouncement(
        'The item has not changed. Pasted items are display-only. Select the Solar Amulet base to explore probabilities.',
      )
      return
    }
    const action = currencyActions[selected.id]
    if (!action) {
      setAnnouncement(
        'The item has not changed. This currency is not supported by the current probability explorer.',
      )
      return
    }
    setPreviewRequest((old) => ({ count: old.count + 1, action }))
    setAnnouncement(
      'Showing possible outcomes below. The item has not changed.',
    )
  }
  async function importText() {
    if (await imported.submit(draft.text)) closeInput()
    setSelected(null)
    setHeld(null)
  }
  function closeInput() {
    imported.invalidate()
    setInputOpen(false)
    inputToggle.current?.focus()
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
    setAnnouncement(`${held.name} registered in favorite slot ${index + 1}.`)
    setHeld(null)
  }

  const item = draft.source === 'text' ? imported.data : undefined
  const displayedName =
    draft.source === 'base'
      ? 'Solar Amulet'
      : (item?.displayName ?? 'Pasted item')
  const search = searches[activeTab] ?? ''
  const currentMaterials = materials.filter(
    (m) =>
      m.category === activeTab &&
      m.name.toLowerCase().includes(search.trim().toLowerCase()),
  )
  function materialButton(material: Material) {
    return (
      <button
        type="button"
        key={material.id}
        className={`material-entry ${held?.id === material.id ? 'is-held' : ''} ${selected?.id === material.id ? 'is-selected' : ''}`}
        aria-label={material.name}
        aria-pressed={held?.id === material.id || selected?.id === material.id}
        {...tooltipEvents(material.id)}
        onContextMenu={(event) => {
          event.preventDefault()
          setPointer({ x: event.clientX, y: event.clientY })
          choose(material)
        }}
        onClick={(event) => {
          if (event.detail === 0) setPointer(null)
          setHeld(material)
          setSelected(null)
          setAnnouncement(
            `${material.name} picked up. Click a favorite slot to register; Escape to cancel.`,
          )
        }}
      >
        <span className="material-entry__image">
          <CurrencyImage key={material.id} {...material} />
        </span>
        <span>{material.name}</span>
      </button>
    )
  }
  const matchingEssenceRows = essenceRows(search)
  const cursor = held ?? selected
  const card = item ? toItemCard(item) : null

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
        <a className="craft-brand" href="/" aria-label="Exile Hephaistos home">
          <span className="brand-mark" aria-hidden="true">
            H
          </span>
          <span>
            EXILE <b>HEPHAISTOS</b>
            <small>PATH OF EXILE 2 · CRAFTING WORKBENCH</small>
          </span>
        </a>
        <nav aria-label="Main navigation">
          <span aria-current="page">Crafting workbench</span>
          <a href="/admin">Admin</a>
        </nav>
      </header>
      <div className="craft-title">
        <div>
          <p className="craft-kicker">THE CRAFTING BENCH</p>
          <h1>Crafting workbench</h1>
          <p>
            Choose a currency and explore the next possibilities for your item.
          </p>
        </div>
        <span className="preview-badge">
          <i />
          Solar Amulet · Base modifiers
        </span>
      </div>
      <div
        className="workbench-layout"
        onContextMenu={(event) => event.preventDefault()}
      >
        <div className="stash-panel">
          <div className="panel-heading stash-heading">
            <div
              role="tablist"
              aria-label="Material stash tabs"
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
            <label className="tooltip-toggle">
              <input
                type="checkbox"
                checked={tooltipsEnabled}
                onChange={(event) => setTooltipsEnabled(event.target.checked)}
              />
              Show tooltips
            </label>
          </div>
          <div className="stash-board">
            <div className="stash-canvas" aria-label="Currency stash">
              <div
                id="material-list"
                role="tabpanel"
                aria-labelledby={`tab-${activeTab}`}
                className={
                  activeTab === 'Currency'
                    ? 'currency-catalog'
                    : 'material-panel'
                }
              >
                {activeTab === 'Currency' ? (
                  currencies.map((currency) => {
                    const name = currencyNames[currency.id]
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
                        {...tooltipEvents(currency.id)}
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
                ) : (
                  <>
                    <input
                      type="search"
                      className="material-search"
                      aria-label={`Search ${activeTab.replaceAll('_', ' ')}`}
                      placeholder="Search…"
                      value={search}
                      onChange={(event) =>
                        setSearches((old) => ({
                          ...old,
                          [activeTab]: event.target.value,
                        }))
                      }
                    />
                    <div
                      className={`material-catalog ${activeTab === 'Essence' ? 'essence-catalog' : ''}`}
                    >
                      {activeTab === 'Essence'
                        ? matchingEssenceRows.map((row, i) => (
                            <div className="essence-row" key={i}>
                              {row.map((m, j) =>
                                m ? materialButton(m) : <span key={j} />,
                              )}
                            </div>
                          ))
                        : currentMaterials.map(materialButton)}
                      {(activeTab === 'Essence'
                        ? matchingEssenceRows.length === 0
                        : currentMaterials.length === 0) && (
                        <p className="material-no-results">
                          No materials found
                        </p>
                      )}
                    </div>
                  </>
                )}
              </div>
              {activeTab === 'Essence' && (
                <div
                  className="special-essences"
                  role="group"
                  aria-label="Special essences"
                >
                  {specialEssences.map(materialButton)}
                </div>
              )}
              <div className="item-placement">
                <span className="placement-label">Crafting item</span>
                <button
                  className={`item-slot ${selected ? 'is-ready' : ''}`}
                  type="button"
                  aria-label="Use selected currency on the central item"
                  onClick={apply}
                >
                  {draft.source === 'base' ? (
                    <img
                      src="/assets/currency/solar-amulet.webp"
                      alt="Solar Amulet"
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
                  Edit item
                </button>
              </div>
              <div
                role="group"
                aria-label="Shared material favorites"
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
                    aria-label={`Favorite slot ${index + 1}: ${resource?.name ?? 'empty'}`}
                    aria-pressed={Boolean(
                      resource && selected?.id === resource.id,
                    )}
                    {...(resource ? tooltipEvents(resource.id) : {})}
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
              {draft.source === 'base' ? (
                <CraftingExplorer
                  key={draft.baseRevision}
                  level={draft.baseItemLevel}
                  revision={draft.baseRevision}
                  requestCount={previewRequest.count}
                  requestedAction={previewRequest.action}
                />
              ) : (
                <div className="bench-item-card">
                  {card && <ItemCard item={card} />}
                  <p>
                    Pasted items are display-only. Select the Solar Amulet base
                    to explore probabilities.
                  </p>
                </div>
              )}
            </div>
          </div>
          <dialog
            ref={dialog}
            onCancel={(event) => {
              event.preventDefault()
              closeInput()
            }}
            onClick={(event) => {
              if (event.target === event.currentTarget) closeInput()
            }}
            id="item-input-panel"
            className="item-input-panel detail-body"
            aria-label="Starting item"
            aria-busy={imported.pending}
          >
            <div className="item-input-content">
              <button
                ref={inputClose}
                type="button"
                className="input-close"
                aria-label="Close item input"
                onClick={closeInput}
              >
                ×
              </button>
              <div className="input-heading">
                <h3>Starting item</h3>
                <span>01</span>
              </div>
              <div
                className="input-tabs"
                role="group"
                aria-label="Item input method"
              >
                <button
                  type="button"
                  aria-pressed={inputMode === 'base'}
                  onClick={() => {
                    imported.invalidate()
                    setInputMode('base')
                  }}
                >
                  Select base
                </button>
                <button
                  type="button"
                  aria-pressed={inputMode === 'text'}
                  onClick={() => setInputMode('text')}
                >
                  Item text
                </button>
              </div>
              {inputMode === 'base' ? (
                <div className="base-form">
                  <label htmlFor="base-select">Amulet base</label>
                  <select id="base-select" defaultValue="solar">
                    <option value="solar">Solar Amulet</option>
                  </select>
                  <label htmlFor="base-level">Item level</label>
                  <input
                    id="base-level"
                    type="number"
                    min="1"
                    max="100"
                    step="1"
                    value={baseLevel}
                    onChange={(e) => setBaseLevel(e.target.value)}
                  />
                  <button
                    type="button"
                    className="primary-action"
                    disabled={
                      !Number.isInteger(Number(baseLevel)) ||
                      Number(baseLevel) < 1 ||
                      Number(baseLevel) > 100
                    }
                    onClick={() => {
                      imported.invalidate()
                      draft.setBase(Number(baseLevel))
                      setPreviewRequest({ count: 0, action: null })
                      setSelected(null)
                      closeInput()
                    }}
                  >
                    Place base <span aria-hidden="true">↗</span>
                  </button>
                </div>
              ) : (
                <div className="text-form">
                  <label htmlFor="item-text">
                    Item text copied from the game
                  </label>
                  <textarea
                    id="item-text"
                    value={draft.text}
                    onChange={(event) => {
                      imported.invalidate()
                      draft.setText(event.target.value)
                    }}
                    placeholder={
                      'Hover over an item and press Ctrl+C\nPaste the copied text here.'
                    }
                    aria-invalid={Boolean(imported.error)}
                    aria-describedby={
                      imported.error ? 'import-error' : undefined
                    }
                  />
                  {imported.error && (
                    <p id="import-error" role="alert">
                      {imported.error}
                    </p>
                  )}
                  <button
                    type="button"
                    className="primary-action"
                    onClick={importText}
                    disabled={imported.pending}
                  >
                    {imported.pending ? 'Analyzing item…' : 'Analyze item'}{' '}
                    <span aria-hidden="true">↗</span>
                  </button>
                </div>
              )}
            </div>
          </dialog>
        </div>
      </div>
      <span className="sr-only" role="status">
        {imported.pending ? 'Analyzing item…' : announcement}
      </span>
      <footer className="craft-footer">
        <span>
          EXILE HEPHAISTOS <span aria-hidden="true">/</span> Your personal
          crafting workbench
        </span>
        <a
          href="https://poe2db.tw/us/Currency"
          target="_blank"
          rel="noreferrer"
        >
          Currency images · PoE2DB ↗
        </a>
      </footer>
      {tooltipsEnabled && !selected && <MaterialTooltip />}
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
