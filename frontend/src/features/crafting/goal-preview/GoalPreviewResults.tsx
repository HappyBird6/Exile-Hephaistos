import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { craftingFetch } from '../../../shared/gameIdTransport'
import { loadModifierDefinitions } from '../../../shared/i18n/localeData'
import { uiText, useI18n, type Locale } from '../../../shared/i18n/i18n'
import { ItemCard } from '../ItemCard'
import type { Definition } from '../craftingApi'
import { localizedAction, localizedCatalystQuality } from '../localizedCrafting'
import {
  localizedModifierName,
  localizedModifierText,
} from '../localizedModifiers'
import {
  createItemPresentation,
  type ItemPresentation,
} from '../path-tree/presentation'
import type { Item } from '../path-tree/types'
import type { ConcreteItem } from '../workbenchApi'
import { qualityShape } from '../catalystQuality'
import { pathTreeMessages } from '../path-tree/messages'
import { modifierMagnitudeValues } from '../modifierMagnitudes'
import { augmentName } from '../socketAugments'
import { rulesetHeaders, verifyRulesetResponse } from '../rulesetIdentity'
import { goalFilterMessages } from '../goal-filter/i18n'
import { tradeGroupNames } from '../goal-filter/setupMessages'
import { hasActiveStatFilters } from '../goal-filter/useGoalFilter'
import { qualityGoalMessages } from '../goal-filter/qualityGoalMessages'
import { readGoalFilterResponse } from '../goal-filter/api'
import { presentGoalStat } from '../goal-filter/statPresentation'
import type { GoalPreviewInput } from '../goal-filter/CraftStart'
import type { Range } from '../goal-filter/types'
import { weighted } from '../goal-filter/types'
import { previewResultMessages } from './previewResultMessages'
import {
  acquisitionSourceLabel,
  acquisitionSourceLabels,
  type AcquisitionSource,
} from './acquisitionSources'
import './previewResults.css'

type PreviewItem = Item & ConcreteItem
type Example = {
  projectedValues?: Record<string, Record<string, number>>
  minimumQuality?: number | null
  occupancy?: {
    prefixes: number
    suffixes: number
    emptyPrefixes: number
    emptySuffixes: number
  }[]
  item: PreviewItem
  tierVariants: PreviewItem[]
  optionTiers: Record<string, string[]>
  sources: Record<string, AcquisitionSource[]>
}
type Preview = {
  cards: Example[]
  modifiers: Record<string, Definition>
  resolvedCountRanges?: Record<string, Range>
  reasonCode?: string | null
}

function validCard(
  card: Example,
  definitions: Preview['modifiers'],
  context: GoalPreviewInput['context'],
) {
  if (
    !Array.isArray(card.tierVariants) ||
    !card.tierVariants.length ||
    (card.occupancy != null &&
      (!Array.isArray(card.occupancy) ||
        !card.occupancy.every((o) =>
          [o.prefixes, o.suffixes, o.emptyPrefixes, o.emptySuffixes].every(
            (v) => Number.isInteger(v) && v >= 0,
          ),
        ))) ||
    !qualityShape(card.item.catalystQuality) ||
    !card.optionTiers
  )
    return false
  const { sources } = card
  const allowed = new Set<string>()
  for (const { modifierId } of [
    ...card.item.implicits,
    ...card.item.explicits,
  ]) {
    const options = card.optionTiers[modifierId]
    if (!Array.isArray(options) || !options.includes(modifierId)) return false
    for (const id of options) {
      if (!definitions[id] || !sources[id]?.length || allowed.has(id))
        return false
      if (card.item.catalystQuality) {
        const values = card.projectedValues?.[id]
        if (
          !values ||
          Array.isArray(values) ||
          typeof values !== 'object' ||
          !Object.values(values).every(Number.isFinite)
        )
          return false
      }
      allowed.add(id)
    }
  }
  return (
    Object.values(sources).every(
      (entries) => entries.length && entries.every(acquisitionSourceLabel),
    ) &&
    [card.item, ...card.tierVariants].every(
      (item) =>
        item.snapshotId === context.snapshotId &&
        item.baseItemId === context.baseItemId &&
        item.itemLevel === context.itemLevel &&
        item.catalystQuality?.type === card.item.catalystQuality?.type &&
        item.catalystQuality?.amount === card.item.catalystQuality?.amount &&
        ['NORMAL', 'MAGIC', 'RARE', 'UNIQUE'].includes(item.rarity) &&
        (item.installedAugments == null ||
          (Array.isArray(item.installedAugments) &&
            item.installedAugments.every(
              (a) => typeof a.augmentId === 'string',
            ))) &&
        [...item.implicits, ...item.explicits].every(
          ({ modifierId, values }) =>
            allowed.has(modifierId) &&
            values != null &&
            typeof values === 'object' &&
            Object.values(values).every(Number.isFinite),
        ),
    )
  )
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

function tierRanges(options: Definition[]) {
  const tiers = [
    ...new Set(options.map((d) => d.tier).filter((tier) => tier > 0)),
  ].sort((a, b) => b - a)
  const ranges: string[] = []
  for (let start = 0; start < tiers.length;) {
    let end = start
    while (tiers[end + 1] === tiers[end]! - 1) end++
    ranges.push(
      start === end ? 'T' + tiers[start] : `T${tiers[start]}~T${tiers[end]}`,
    )
    start = end + 1
  }
  return ranges.join(', ')
}

function sourcesForOptions(
  options: Definition[],
  sources: Example['sources'],
  locale: Locale,
) {
  const bySource = new Map<string, Definition[]>()
  for (const option of options) {
    for (const label of acquisitionSourceLabels(sources[option.id]!)) {
      const members = bySource.get(label)
      if (members) members.push(option)
      else bySource.set(label, [option])
    }
  }
  return [...bySource]
    .map(([source, members]) => {
      if (members.length === options.length) return source
      // Two affixes can have the same tier number. Name them if they have different sources.
      const ambiguous = options.some(
        (other) =>
          !members.includes(other) &&
          members.some((m) => m.tier === other.tier),
      )
      const label = ambiguous
        ? members
            .map((m) => `T${m.tier} ${localizedModifierName(m, locale)}`)
            .join(', ')
        : tierRanges(members)
      return `${source} [${label}]`
    })
    .join(', ')
}

type QueryInputs = Pick<GoalPreviewInput, 'context' | 'goal' | 'fracture'> & {
  rulesetIdentity: string
}
function previewOptions(
  { context, goal, fracture, rulesetIdentity }: QueryInputs,
  selectedTier?: string,
) {
  return {
    queryKey: [
      'goalPreview',
      rulesetIdentity,
      context,
      goal,
      fracture,
      selectedTier,
    ],
    queryFn: async ({ signal }: { signal: AbortSignal }) => {
      const response = await craftingFetch(
        '/api/v1/crafting/support/goal-filters/preview',
        {
          method: 'POST',
          signal,
          headers: rulesetHeaders(rulesetIdentity),
          body: JSON.stringify({ context, goal, fracture, selectedTier }),
        },
      )
      if (response.ok) verifyRulesetResponse(response, rulesetIdentity)
      const result = await readGoalFilterResponse<Preview>(response)
      if (!Array.isArray(result.cards) || !result.modifiers)
        throw new Error('Invalid goal preview')
      const cards = result.cards.filter((card) => {
        try {
          return validCard(card, result.modifiers, context)
        } catch {
          return false
        }
      })
      await loadModifierDefinitions([
        ...new Set(cards.flatMap((card) => Object.keys(card.sources))),
      ])
      signal.throwIfAborted()
      return { ...result, cards, omitted: result.cards.length - cards.length }
    },
    retry: false,
    staleTime: Infinity,
  }
}

function PreviewCard({
  example: original,
  inputs,
  definitions: originalDefinitions,
  present,
  locale,
}: {
  example: Example
  inputs: QueryInputs
  definitions: Record<string, Definition>
  present: ItemPresentation
  locale: Locale
}) {
  const [selected, setSelected] = useState<string>()
  const conditional = useQuery({
    ...previewOptions(inputs, selected),
    enabled: !!selected,
  })
  const slots = Object.values(original.optionTiers)
  const match =
    selected &&
    conditional.data?.cards.find((c) => {
      const ids = Object.keys(c.optionTiers)
      return (
        c.item.rarity === original.item.rarity &&
        c.item.catalystQuality?.type === original.item.catalystQuality?.type &&
        c.item.catalystQuality?.amount ===
          original.item.catalystQuality?.amount &&
        ids.includes(selected) &&
        ids.length === slots.length &&
        slots.every((slot) => ids.some((id) => slot.includes(id)))
      )
    })
  const definitions =
    (match && conditional.data?.modifiers) || originalDefinitions
  const example = match || original
  const { item, tierVariants, optionTiers, sources } = example
  const copy = previewResultMessages[locale]
  const card = present(item, locale)
  const quality = item.catalystQuality
  const rolls = new Map<string, Record<string, number>>()
  for (const variant of [item, ...tierVariants])
    for (const mod of [...variant.implicits, ...variant.explicits]) {
      if (!rolls.has(mod.modifierId))
        rolls.set(
          mod.modifierId,
          example.projectedValues?.[mod.modifierId] ??
            modifierMagnitudeValues(
              variant,
              definitions[mod.modifierId]!,
              mod.values,
            ),
        )
    }
  function optionText(d: Definition, values: Record<string, number>) {
    return quality && d.stats?.length && Object.keys(values).length === 0
      ? localizedModifierName(d, locale) +
          ' · ' +
          pathTreeMessages[locale].unavailable
      : localizedModifierText(d, values, locale)
  }
  return (
    <div>
      <ItemCard
        baseItemId={item.baseItemId}
        item={{
          ...card,
          properties: [
            { id: 'rarity', text: rarityLabel(item.rarity, locale) },
            ...(example.minimumQuality
              ? [
                  {
                    id: 'quality-goal',
                    text:
                      qualityGoalMessages[locale].label +
                      ' ≥ ' +
                      example.minimumQuality +
                      '%',
                  },
                ]
              : []),
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
            ...(item.qualityInfused
              ? [
                  {
                    id: 'quality-infuser',
                    text: localizedAction('VAAL_CATALYSING_INFUSER'),
                  },
                ]
              : []),
            ...(example.occupancy?.length
              ? [
                  {
                    id: 'occupancy',
                    text:
                      copy.occupancy +
                      ' · ' +
                      example.occupancy
                        .map(
                          (o) =>
                            `${o.prefixes}/${o.suffixes} (${o.emptyPrefixes}/${o.emptySuffixes})`,
                        )
                        .join(', '),
                  },
                ]
              : []),
            ...(item.installedAugments ?? []).map((a, i) => ({
              id: 'socket-' + i,
              text: augmentName(a.augmentId),
            })),
          ],
          modifiers: card.modifiers.map((line) => {
            const options = optionTiers[line.id]!.map(
              (id) => definitions[id]!,
            ).sort((a, b) => b.tier - a.tier)
            return {
              ...line,
              text: optionText(definitions[line.id]!, rolls.get(line.id)!),
              affixLabel: tierRanges(options),
              sourceLabel:
                copy.sources +
                ' · ' +
                sourcesForOptions(options, sources, locale),
              ...(options.length > 1 && {
                tierRows: options.flatMap((d) => {
                  const values = rolls.get(d.id)
                  return values
                    ? [
                        {
                          label: `T${d.tier} · ${localizedModifierName(d, locale)}`,
                          onSelect: () => setSelected(d.id),
                          value: optionText(d, values),
                        },
                      ]
                    : []
                }),
              }),
            }
          }),
        }}
      />
      {selected && (
        <div role="status">
          {conditional.isFetching
            ? copy.loading
            : match
              ? 'T' +
                definitions[selected]!.tier +
                ' · ' +
                localizedModifierName(definitions[selected]!, locale)
              : conditional.isError
                ? goalFilterMessages[locale].error
                : copy.empty}
          <button onClick={() => setSelected(undefined)} type="button">
            {copy.allTiers}
          </button>
        </div>
      )}
    </div>
  )
}

export function GoalPreviewResults({
  base,
  initial,
  fracture,
  context,
  catalog,
  goal,
}: GoalPreviewInput) {
  const { locale, name } = useI18n()
  const copy = previewResultMessages[locale],
    labels = goalFilterMessages[locale]
  const { rulesetIdentity } = initial
  const inputs = { context, goal, fracture, rulesetIdentity }
  const preview = useQuery(previewOptions(inputs))
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
            <strong lang="en">{tradeGroupNames[group.type]}</strong>
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
      {preview.data && preview.data.omitted > 0 && (
        <p role="alert">
          {copy.omitted} {preview.data.omitted}
        </p>
      )}
      {preview.data?.cards.length === 0 && !preview.data.omitted && (
        <p role="status">
          {preview.data.reasonCode === 'QUALITY_PROJECTION_NOT_IMPLEMENTED'
            ? copy.qualityProjection
            : copy.empty}
        </p>
      )}
      <div className="goal-preview-cards">
        {preview.data?.cards.map((example) => (
          <PreviewCard
            key={JSON.stringify([example.item, goal])}
            inputs={inputs}
            example={example}
            definitions={preview.data.modifiers}
            locale={locale}
            present={present}
          />
        ))}
      </div>
    </section>
  )
}
