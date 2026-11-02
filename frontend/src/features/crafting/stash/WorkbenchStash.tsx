import { useState } from 'react'
import { matchesGameName, useI18n } from '../../../shared/i18n/i18n'
import { CurrencyImage } from '../CurrencyImage'
import {
  essenceRows,
  isSelectableMaterial,
  specialEssences,
} from '../materials'
import type { MaterialTab } from '../materials'
import { inServiceScope } from '../serviceScope'
import { legacyOmenIds } from '../workbenchApi'
import type { WorkbenchResource, WorkbenchStashProps } from './stashTypes'
import { stashDragType } from './stashDragType'
import './workbenchStash.css'

/** Category content only: the existing page-level tabs own activeTab. */
export function WorkbenchStash({
  resources,
  excludedIds = [],
  activeTab,
  selectedId,
  activeOmenIds,
  held,
  disabled = false,
  showLegacyOmens = false,
  onShowLegacyOmensChange,
  onSelect,
  onPickUp,
}: WorkbenchStashProps) {
  const { t, name } = useI18n()
  const [searches, setSearches] = useState<
    Partial<Record<MaterialTab, string>>
  >({})
  const excluded = new Set(excludedIds)
  const search = searches[activeTab] ?? ''
  const available = (resource: WorkbenchResource) =>
    !excluded.has(resource.id) &&
    inServiceScope(resource.id) &&
    isSelectableMaterial(resource)
  const visible = resources.filter(
    (resource) =>
      resource.category === activeTab &&
      available(resource) &&
      (showLegacyOmens || !legacyOmenIds.includes(resource.id)) &&
      matchesGameName(resource.id, resource.name, search),
  )
  const rows = essenceRows(search)
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
          if (!disabled) onSelect(resource, pointer(event))
        }}
        onDragStart={(event) => {
          event.dataTransfer.setData(stashDragType, resource.id)
          event.dataTransfer.effectAllowed = 'copy'
          onPickUp(resource, pointer(event))
        }}
      >
        <span className="workbench-stash__image">
          <CurrencyImage key={resource.id} {...resource} />
        </span>
        <span className="workbench-stash__name">
          {name(resource.id, resource.name)}
          {legacyOmenIds.includes(resource.id) ? t('legacy.suffix') : ''}
        </span>
      </button>
    )
  }
  return (
    <section className="workbench-stash" aria-label={t('ui.currency_stash')}>
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
        {activeTab === 'Omen' && onShowLegacyOmensChange && (
          <label className="workbench-stash__legacy">
            <input
              type="checkbox"
              checked={showLegacyOmens}
              disabled={disabled}
              onChange={(event) =>
                onShowLegacyOmensChange(event.target.checked)
              }
            />
            {t('ui.show_legacy_omens')}
          </label>
        )}
        {activeTab === 'Omen' && showLegacyOmens && (
          <p className="workbench-stash__legacy-note">
            Homogenising: drops disabled in 0.4; existing items work. Alchemy,
            Coronation and Greater Annulment: no longer obtainable in 0.3.
            Legacy effects are modeled within the supported scope; current
            acquisition is not asserted. Coronation and Homogenising support
            matching ordinary, Greater and Perfect currency. Tiered composition
            is an unverified model. Alchemy and Annulment use ordinary currency.
            Greater Exaltation with Homogenising uses pre-craft tags for both
            additions. Only reviewed combinations are enabled; Catalysing
            Exaltation remains unsupported. Failed requests preserve resources
            here; actual game failure consumption is unverified.{' '}
            <a
              href="https://www.pathofexile.com/forum/view-thread/3883495/filter-account-type/staff"
              target="_blank"
              rel="noreferrer"
            >
              {t('ui.official_homogenising_source')}
            </a>{' '}
            <a
              href="https://www.pathofexile.com/forum/view-thread/3826682"
              target="_blank"
              rel="noreferrer"
            >
              {t('ui.official_0_3_availability')}
            </a>
          </p>
        )}
        <div className="workbench-stash__resources">
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
