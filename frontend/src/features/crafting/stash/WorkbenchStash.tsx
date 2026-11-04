import { useState } from 'react'
import { matchesGameName, useI18n } from '../../../shared/i18n/i18n'
import { CurrencyImage } from '../CurrencyImage'
import { essenceGridRows, specialEssences } from '../materials'
import type { MaterialTab } from '../materials'
import { visibilityPolicy } from './visibilityPolicy'
import type { WorkbenchResource, WorkbenchStashProps } from './stashTypes'
import { stashDragType } from './stashDragType'
import './workbenchStash.css'
import '../workbench-scrollbar.css'

/** Controlled right panel; the page owns the single category state/tablist. */
export function WorkbenchStash({
  resources,
  excludedIds = [],
  activeTab,
  tabs,
  fixedCurrencyIds = [],
  selectedId,
  activeOmenIds,
  held,
  disabled = false,
  onActivateOmen,
  onSelect,
  onPickUp,
}: WorkbenchStashProps) {
  const { t, name } = useI18n()
  const [searches, setSearches] = useState<
    Partial<Record<MaterialTab, string>>
  >({})
  const excluded = new Set(excludedIds)
  const fixedCurrencies = new Set(fixedCurrencyIds)
  const search = searches[activeTab] ?? ''
  const available = (resource: WorkbenchResource) =>
    !excluded.has(resource.id) && visibilityPolicy(resource)
  const visible = resources.filter(
    (resource) =>
      resource.category === activeTab &&
      available(resource) &&
      !(resource.category === 'Currency' && fixedCurrencies.has(resource.id)) &&
      matchesGameName(resource.id, resource.name, search),
  )
  const rows = essenceGridRows(search)
    .map((row) =>
      row.map((entry) =>
        entry
          ? resources.find(
              (resource) => resource.id === entry.id && available(resource),
            )
          : undefined,
      ),
    )
    .filter((row) => row.some(Boolean))
  const specials = specialEssences.flatMap((entry) => {
    const resource = resources.find(
      (resource) => resource.id === entry.id && available(resource),
    )
    return resource ? [resource] : []
  })
  const pointer = (event: { clientX: number; clientY: number }) => ({
    x: event.clientX,
    y: event.clientY,
  })
  function resourceButton(resource: WorkbenchResource) {
    return (
      <button
        type="button"
        key={resource.id}
        className={`workbench-stash__resource ${selectedId === resource.id ? 'is-selected' : ''} ${held?.id === resource.id ? 'is-held' : ''} ${activeOmenIds.includes(resource.id) ? 'is-active-omen' : ''}`}
        aria-label={name(resource.id, resource.name)}
        aria-pressed={
          selectedId === resource.id ||
          held?.id === resource.id ||
          activeOmenIds.includes(resource.id)
        }
        data-material-tooltip={resource.id}
        disabled={disabled}
        draggable={!disabled}
        onClick={(event) => {
          const position = event.detail === 0 ? undefined : pointer(event)
          if (resource.category === 'Currency') onSelect(resource, position)
          else onPickUp(resource, position)
        }}
        onContextMenu={(event) => {
          event.preventDefault()
          if (disabled) return
          if (resource.category === 'Omen' && onActivateOmen)
            onActivateOmen(resource, pointer(event))
          else onSelect(resource, pointer(event))
        }}
        onDragStart={(event) => {
          event.dataTransfer.setData(stashDragType, resource.id)
          event.dataTransfer.effectAllowed = 'copy'
          onPickUp(resource, pointer(event))
        }}
      >
        <span className="workbench-stash__image">
          {resource.image ? (
            <CurrencyImage key={resource.id} {...resource} />
          ) : (
            <span className="currency-missing">
              {t('ui.image_unavailable')}
            </span>
          )}
        </span>
      </button>
    )
  }
  return (
    <section className="workbench-stash" aria-label={t('ui.currency_stash')}>
      {tabs && <div className="workbench-stash__tabs">{tabs}</div>}
      <div
        className="workbench-stash__panel"
        id="material-list"
        role="tabpanel"
        aria-labelledby={`tab-${activeTab}`}
      >
        <input
          type="search"
          className="workbench-stash__search"
          aria-label={t('material.search', {
            category: t(`stash.${activeTab}`),
          })}
          placeholder={t('ui.search')}
          value={search}
          disabled={disabled}
          onChange={(event) =>
            setSearches((current) => ({
              ...current,
              [activeTab]: event.target.value,
            }))
          }
        />
        <div className="workbench-stash__resources wb-scrollbar">
          {activeTab === 'Essence'
            ? rows.map((row, index) => (
                <div className="essence-row" key={index}>
                  {row.map((entry, slot) =>
                    entry ? resourceButton(entry) : <span key={slot} />,
                  )}
                </div>
              ))
            : visible.map(resourceButton)}
          {(activeTab === 'Essence'
            ? rows.length === 0
            : visible.length === 0) && (
            <p className="workbench-stash__empty">
              {t('ui.no_materials_found')}
            </p>
          )}
        </div>
      </div>
      {activeTab === 'Essence' && (
        <div
          className="workbench-stash__specials"
          role="group"
          aria-label={t('ui.special_essences')}
        >
          {specials.map(resourceButton)}
        </div>
      )}
    </section>
  )
}
