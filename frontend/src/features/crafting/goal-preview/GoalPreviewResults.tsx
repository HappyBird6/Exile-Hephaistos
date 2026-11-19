import { useQuery } from '@tanstack/react-query'
import { craftingFetch } from '../../../shared/gameIdTransport'
import { loadModifierDefinitions } from '../../../shared/i18n/localeData'
import {
  gameName,
  uiText,
  useI18n,
  type Locale,
} from '../../../shared/i18n/i18n'
import { ItemCard } from '../ItemCard'
import type { Definition, Initial } from '../craftingApi'
import { localizedAction, localizedCatalystQuality } from '../localizedCrafting'
import { createItemPresentation } from '../path-tree/presentation'
import type { Item } from '../path-tree/types'
import type { CatalystQuality } from '../catalystQuality'
import {
  workbenchActionNames,
  type WorkbenchAction,
} from '../currencyDefinitions'
import { augmentName } from '../socketAugments'
import { rulesetHeaders, verifyRulesetResponse } from '../rulesetIdentity'
import { goalFilterMessages } from '../goal-filter/i18n'
import { setupMessages } from '../goal-filter/setupMessages'
import { hasActiveStatFilters } from '../goal-filter/useGoalFilter'
import { qualityGoalMessages } from '../goal-filter/qualityGoalMessages'
import { readGoalFilterResponse } from '../goal-filter/api'
import { presentGoalStat } from '../goal-filter/statPresentation'
import type { StartBase } from '../goal-filter/startItem'
import type { Catalog, Context, GoalFilter, Range } from '../goal-filter/types'
import { weighted } from '../goal-filter/types'
import { previewResultMessages } from './previewResultMessages'
import './previewResults.css'

type Source =
  | { kind: 'IMPLICIT' | 'NORMAL'; id?: null }
  | {
      kind: 'ESSENCE' | 'ALLOY' | 'LIQUID' | 'DESECRATION' | 'AUGMENT'
      id: string
    }
type Example = {
  item: Item & {
    catalystQuality: CatalystQuality | null
    installedAugments?: { augmentId: string }[]
  }
  sources: Record<string, Source[]>
}
type Preview = {
  cards: Example[]
  modifiers: Record<string, Definition>
  resolvedCountRanges?: Record<string, Range>
  reasonCode?: string | null
}
type Props = {
  base: StartBase
  initial: Initial
  context: Context
  catalog: Catalog
  goal: GoalFilter
}

function sourceLabel(source: Source, locale: Locale) {
  const copy = previewResultMessages[locale]
  if (source.kind === 'IMPLICIT') return copy.implicit
  if (source.kind === 'NORMAL') return copy.ordinary
  const id = source.id!
  if (source.kind === 'AUGMENT') return `${augmentName(id)} (${copy.augment})`
  if (Object.hasOwn(workbenchActionNames, id))
    return localizedAction(id as WorkbenchAction)
  return gameName(id, id.replaceAll('_', ' '), locale)
}

function bounds(range: Range, locale: Locale, unit = '') {
  const n = (value: number) =>
    value.toLocaleString(locale, { maximumFractionDigits: 20 }) +
    (unit === 'percent' ? '%' : '')
  if (range.min === null && range.max === null)
    return previewResultMessages[locale].presence
  if (range.min === null) return '≤ ' + n(range.max!)
  if (range.max === null) return '≥ ' + n(range.min)
  return range.min === range.max
    ? n(range.min)
    : n(range.min) + ' – ' + n(range.max)
}

function rarityLabel(rarity: string, locale: Locale) {
  return uiText(rarity.charAt(0) + rarity.slice(1).toLowerCase(), locale)
}

export function GoalPreviewResults({
  base,
  initial,
  context,
  catalog,
  goal,
}: Props) {
  const { locale, name } = useI18n()
  const copy = previewResultMessages[locale],
    labels = goalFilterMessages[locale]
  const { rulesetIdentity } = initial
  const preview = useQuery({
    queryKey: ['goalPreview', rulesetIdentity, context, goal],
    queryFn: async ({ signal }) => {
      const response = await craftingFetch(
        '/api/v1/crafting/support/goal-filters/preview',
        {
          method: 'POST',
          signal,
          headers: rulesetHeaders(rulesetIdentity),
          body: JSON.stringify({ context, goal }),
        },
      )
      if (response.ok) verifyRulesetResponse(response, rulesetIdentity)
      const result = await readGoalFilterResponse<Preview>(response)
      if (
        !Array.isArray(result.cards) ||
        !result.modifiers ||
        result.cards.some(
          ({ item, sources }) =>
            item.snapshotId !== context.snapshotId ||
            item.baseItemId !== context.baseItemId ||
            item.itemLevel !== context.itemLevel ||
            [...item.implicits, ...item.explicits].some(
              ({ modifierId }) =>
                !result.modifiers[modifierId] ||
                !sources[modifierId]?.length ||
                sources[modifierId]!.some(
                  (s) => !s.id && s.kind !== 'IMPLICIT' && s.kind !== 'NORMAL',
                ),
            ),
        )
      )
        throw new Error('Invalid goal preview')
      await loadModifierDefinitions(Object.keys(result.modifiers))
      signal.throwIfAborted()
      return result
    },
    retry: false,
    staleTime: Infinity,
  })
  const presentationCatalog = {
    ...catalog,
    sourceModifiers: catalog.sourceModifiers ?? initial.modifiers,
  }
  const present = createItemPresentation(
    { name: base[2], itemClass: base[1] },
    preview.data?.modifiers ?? {},
  )
  return (
    <section className="goal-preview-results" aria-label={copy.conditions}>
      <h2>{copy.conditions}</h2>
      <p>
        {name(context.baseItemId, base[2])} · {labels.level} {context.itemLevel}{' '}
        ·{' '}
        {goal.general.rarities
          .map((rarity) => rarityLabel(rarity, locale))
          .join(' / ')}
        {goal.general.quality
          ? ` · ${qualityGoalMessages[locale].label} ${goal.general.quality}%`
          : ''}
      </p>
      {goal.groups.filter(hasActiveStatFilters).map((group) => {
        const range =
          group.type === 'COUNT'
            ? preview.data?.resolvedCountRanges?.[group.id]
            : group.range
        return (
          <div key={group.id}>
            <strong>{setupMessages[locale].groups[group.type]}</strong>
            {' · '}
            {labels.operations[group.type]}
            {range && (range.min !== null || range.max !== null) && (
              <>
                {' '}
                {group.type === 'COUNT' ? labels.count : labels.score}{' '}
                {bounds(range, locale)}
              </>
            )}
            <ul>
              {group.entries
                .filter((entry) => !entry.disabled)
                .map((entry) => {
                  const stat = catalog.stats.find(
                    (s) => s.statId === entry.statId,
                  )
                  return (
                    <li key={entry.id}>
                      {stat
                        ? presentGoalStat(stat, presentationCatalog, locale)
                            .label
                        : entry.statId}
                      {' · '}
                      {bounds(entry.range, locale, entry.unit)}
                      {weighted(group.type) && (
                        <>
                          {' '}
                          · {labels.weight} {entry.weight ?? 1}
                        </>
                      )}
                    </li>
                  )
                })}
            </ul>
          </div>
        )
      })}
      <h2>{copy.examples}</h2>
      {preview.isPending && <p role="status">{copy.loading}</p>}
      {preview.isError && (
        <p role="alert">
          {labels.error}{' '}
          <button type="button" onClick={() => void preview.refetch()}>
            {copy.retry}
          </button>
        </p>
      )}
      {preview.data?.cards.length === 0 && (
        <p role="status">
          {preview.data.reasonCode === 'QUALITY_PROJECTION_NOT_IMPLEMENTED'
            ? copy.qualityProjection
            : copy.empty}
        </p>
      )}
      <div className="goal-preview-cards">
        {preview.data?.cards.map(({ item, sources }) => {
          const card = present(item, locale)
          const quality = item.catalystQuality
          const augments = item.installedAugments ?? []
          return (
            <ItemCard
              key={JSON.stringify(item)}
              baseItemId={item.baseItemId}
              item={{
                ...card,
                properties: [
                  { id: 'rarity', text: rarityLabel(item.rarity, locale) },
                  ...(quality
                    ? [
                        {
                          id: 'quality',
                          text: localizedCatalystQuality(
                            quality,
                            item.baseItemId,
                            locale,
                          ),
                        },
                      ]
                    : []),
                  ...augments.map((a, i) => ({
                    id: 'socket-' + i,
                    text: augmentName(a.augmentId),
                  })),
                ],
                modifiers: card.modifiers.map((line) => ({
                  ...line,
                  sourceLabel:
                    copy.sources +
                    ' · ' +
                    sources[line.id]!.map((source) =>
                      sourceLabel(source, locale),
                    ).join(' / '),
                })),
              }}
            />
          )
        })}
      </div>
    </section>
  )
}
