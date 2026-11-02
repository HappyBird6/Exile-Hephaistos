import { useId, useRef, useState } from 'react'
import { matchesGameName, useI18n } from '../../../shared/i18n/i18n'
import { CurrencyImage } from '../CurrencyImage'
import { isSelectableMaterial, materialTabs } from '../materials'
import type { MaterialTab } from '../materials'
import { inServiceScope } from '../serviceScope'
import { legacyOmenIds } from '../workbenchApi'
import type { WorkbenchResource, WorkbenchStashProps } from './stashTypes'
import './workbenchStash.css'

const dragType = 'application/x-hephaistos-resource'

/** Replaces only the old right-hand favorites region inside .stash-canvas. */
export function WorkbenchStash({
  resources,
  excludedIds = [],
  activeTab,
  onTabChange,
  selectedId,
  activeOmenIds,
  favorites,
  held,
  disabled = false,
  showLegacyOmens = false,
  onShowLegacyOmensChange,
  onSelect,
  onPickUp,
  onFavoriteActivate,
  onFavoritePlace,
}: WorkbenchStashProps) {
  const { t, name } = useI18n()
  const instanceId = useId()
  const tabs = useRef<(HTMLButtonElement | null)[]>([])
  const dragged = useRef<WorkbenchResource | null>(null)
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
  const panelId = `${instanceId}-panel`
  const pointer = (event: { clientX: number; clientY: number }) => ({
    x: event.clientX,
    y: event.clientY,
  })

  return (
    <section className="workbench-stash" aria-label={t('ui.currency_stash')}>
      <div
        className="workbench-stash__tabs"
        role="tablist"
        aria-label={t('ui.material_stash_tabs')}
      >
        {materialTabs.map((tab, index) => (
          <button
            key={tab.id}
            type="button"
            role="tab"
            id={`${instanceId}-${tab.id}`}
            ref={(element) => {
              tabs.current[index] = element
            }}
            aria-selected={activeTab === tab.id}
            aria-controls={panelId}
            tabIndex={activeTab === tab.id ? 0 : -1}
            disabled={disabled}
            onClick={() => onTabChange(tab.id)}
            onKeyDown={(event) => {
              const next =
                event.key === 'ArrowRight'
                  ? (index + 1) % materialTabs.length
                  : event.key === 'ArrowLeft'
                    ? (index + materialTabs.length - 1) % materialTabs.length
                    : event.key === 'Home'
                      ? 0
                      : event.key === 'End'
                        ? materialTabs.length - 1
                        : null
              if (next === null) return
              event.preventDefault()
              onTabChange(materialTabs[next]!.id)
              tabs.current[next]?.focus()
            }}
          >
            {t(`stash.${tab.id}`)}
          </button>
        ))}
      </div>
      <div
        className="workbench-stash__panel"
        id={panelId}
        role="tabpanel"
        aria-labelledby={`${instanceId}-${activeTab}`}
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
        <div className="workbench-stash__resources">
          {visible.map((resource) => (
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
                if (resource.category === 'Currency')
                  onSelect(resource, position)
                else onPickUp(resource, position)
              }}
              onContextMenu={(event) => {
                event.preventDefault()
                if (!disabled) onSelect(resource, pointer(event))
              }}
              onDragStart={(event) => {
                dragged.current = resource
                event.dataTransfer.setData(dragType, resource.id)
                event.dataTransfer.effectAllowed = 'copy'
                onPickUp(resource, pointer(event))
              }}
              onDragEnd={() => {
                dragged.current = null
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
          ))}
          {visible.length === 0 && (
            <p className="workbench-stash__empty">
              {t('ui.no_materials_found')}
            </p>
          )}
        </div>
      </div>
      <div
        className={`workbench-stash__favorites ${held ? 'is-placing' : ''}`}
        role="group"
        aria-label={t('ui.shared_material_favorites')}
      >
        {Array.from({ length: 10 }, (_, index) => {
          const resource = favorites[index] ?? null
          const unavailable = resource !== null && !available(resource)
          return (
            <button
              type="button"
              key={index}
              className={`workbench-stash__favorite ${resource && selectedId === resource.id ? 'is-selected' : ''} ${resource && activeOmenIds.includes(resource.id) ? 'is-active-omen' : ''}`}
              aria-label={t('material.favorite', {
                index: index + 1,
                name: resource
                  ? name(resource.id, resource.name)
                  : t('material.empty'),
              })}
              aria-pressed={Boolean(
                resource &&
                (selectedId === resource.id ||
                  activeOmenIds.includes(resource.id)),
              )}
              aria-disabled={disabled || (unavailable && !held)}
              disabled={disabled}
              data-material-tooltip={resource?.id}
              onClick={() => {
                if (held && available(held)) onFavoritePlace(index, held)
                else if (!unavailable) onFavoriteActivate(index, resource)
              }}
              onContextMenu={(event) => {
                event.preventDefault()
                if (!disabled && !unavailable)
                  onFavoriteActivate(index, resource, pointer(event))
              }}
              onDragOver={(event) => {
                if (
                  !disabled &&
                  dragged.current &&
                  available(dragged.current)
                ) {
                  event.preventDefault()
                  event.dataTransfer.dropEffect = 'copy'
                }
              }}
              onDrop={(event) => {
                event.preventDefault()
                const resource = dragged.current
                if (
                  !disabled &&
                  resource &&
                  available(resource) &&
                  event.dataTransfer.getData(dragType) === resource.id
                ) {
                  onFavoritePlace(index, resource)
                  dragged.current = null
                }
              }}
            >
              {resource ? (
                <CurrencyImage key={resource.id} {...resource} />
              ) : (
                <span aria-hidden="true">+</span>
              )}
            </button>
          )
        })}
      </div>
    </section>
  )
}
