import { useI18n } from '../../../shared/i18n/i18n'
import { CurrencyImage } from '../CurrencyImage'
import { isSelectableMaterial } from '../materials'
import { inServiceScope } from '../serviceScope'
import type { WorkbenchFavoritesProps, WorkbenchResource } from './stashTypes'
import { stashDragType } from './stashDragType'
import './workbenchStash.css'

/** Independent two-row favorite strip, mounted after the entire stash board. */
export function WorkbenchFavorites({
  excludedIds = [],
  selectedId,
  activeOmenIds,
  favorites,
  held,
  disabled = false,
  onFavoriteActivate,
  onFavoritePlace,
}: WorkbenchFavoritesProps) {
  const { t, name } = useI18n()
  const excluded = new Set(excludedIds)
  const available = (resource: WorkbenchResource) =>
    !excluded.has(resource.id) &&
    inServiceScope(resource.id) &&
    isSelectableMaterial(resource)
  const pointer = (event: { clientX: number; clientY: number }) => ({
    x: event.clientX,
    y: event.clientY,
  })
  return (
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
              if (!disabled && held && available(held)) {
                event.preventDefault()
                event.dataTransfer.dropEffect = 'copy'
              }
            }}
            onDrop={(event) => {
              event.preventDefault()
              const resource = held
              if (
                !disabled &&
                resource &&
                available(resource) &&
                event.dataTransfer.getData(stashDragType) === resource.id
              ) {
                onFavoritePlace(index, resource)
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
  )
}
