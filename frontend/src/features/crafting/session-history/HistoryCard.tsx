import basePolicies from '../basePolicies.json'
import type { WorkbenchBaseKey } from '../baseRegistry'
import type { ItemCardLine } from '../itemCardData'
import { supportsConcreteStateShape } from '../workbenchStateShape'
import { useQuery } from '@tanstack/react-query'
import { loadInitial } from '../craftingApi'
import { topBaseKey, topBase, basePropertyLines } from '../topBases'
import { baseClass, baseSlugs } from '../baseRegistry'
import { useI18n } from '../../../shared/i18n/i18n'
import { ItemCard } from '../ItemCard'
import { verifiedHistoryState, filmRulesetStatus } from '../workbenchHistory'
import type { Film, Frame } from '../workbenchHistory'
import { localizedModifierText } from '../localizedModifiers'
import { catalystProjection } from '../catalystQuality'
import { preciseModifierProjection } from '../sanctification/preciseModifierProjection'
import { augmentName, augmentEffect, socketText } from '../socketAugments'
import { defenceDisplay } from '../defenceDisplay'
import { BaseImage } from '../base-picker/BaseImage'
import { historyMessages } from './messages'

export function HistoryCard({ film, frame }: { film: Film; frame: Frame }) {
  const { locale, name, t } = useI18n()
  const text = historyMessages[locale]
  const state = frame.state
  const base =
    topBaseKey(state.baseItemId) ??
    (Object.entries(basePolicies.legacy).find(
      ([, v]) => v.id === state.baseItemId,
    )?.[0] as WorkbenchBaseKey | undefined)
  const initial = useQuery({
    queryKey: ['history-catalog', base, state.itemLevel],
    enabled: !!base,
    queryFn: ({ signal }) => loadInitial(state.itemLevel, signal, base!),
    retry: false,
  })
  const supported = supportsConcreteStateShape(state)
  const valid =
    supported &&
    initial.data &&
    filmRulesetStatus(film, initial.data.rulesetIdentity) === 'current' &&
    verifiedHistoryState(state, initial.data)
  const definitions = initial.data?.modifiers
  const defences =
    valid && definitions && !state.installedAugments?.length
      ? defenceDisplay(state, definitions, locale)
      : { properties: [], replacedSourceIndexes: new Set<number>() }
  if (!supported)
    return (
      <>
        <p>{text.unknown}</p>
      </>
    )
  return (
    <>
      <BaseImage baseItemId={state.baseItemId} label={state.baseItemId} />
      {!valid && <p className="history-warning">{text.unknown}</p>}
      <ItemCard
        baseItemId={state.baseItemId}
        item={{
          rarity: state.rarity,
          name: base
            ? name(
                baseSlugs[base],
                topBase(base)?.name ?? baseSlugs[base].replaceAll('_', ' '),
              )
            : state.baseItemId,
          base: null,
          itemClass: base ? baseClass(base) : '',
          itemLevel: state.itemLevel,
          properties: [
            ...(valid && base
              ? basePropertyLines(base, locale)
                  .filter((_, i) => !defences.replacedSourceIndexes.has(i))
                  .map((text, i) => ({ id: `base-${i}`, text }))
              : []),
            ...defences.properties,
            ...(state.observations?.augmentEffects ?? []).map((text, i) => ({
              id: `observed-augment-${i}`,
              text,
            })),
            ...(state.observations?.grantedSkills ?? []).map((skill, i) => ({
              id: `observed-skill-${i}`,
              text:
                skill.name + (skill.level == null ? '' : ` (${skill.level})`),
            })),
            ...(state.augmentSockets == null &&
            state.observations?.socketCount != null
              ? [
                  {
                    id: 'observed-sockets',
                    text: `${socketText().socket}: ${state.observations.socketCount}`,
                  },
                ]
              : []),
            ...(state.equipmentQuality != null
              ? [
                  {
                    id: 'quality',
                    text: t('quality.equipment', {
                      value: state.equipmentQuality,
                    }),
                  },
                ]
              : []),
            ...(state.catalystQuality
              ? [
                  {
                    id: 'catalyst',
                    text: `${state.catalystQuality.type}: ${state.catalystQuality.amount}%`,
                  },
                ]
              : []),
            ...(state.augmentSockets != null
              ? [
                  {
                    id: 'sockets',
                    text: `${socketText().socket}: ${state.augmentSockets}`,
                  },
                ]
              : []),
            ...(state.installedAugments ?? []).map((a) => ({
              id: `augment-${a.slotIndex}`,
              text: `${a.slotIndex + 1}. ${augmentName(a.augmentId)}: ${(valid ? augmentEffect(a.augmentId, state.baseItemId) : undefined) ?? a.augmentId}`,
            })),
          ],
          requirements:
            valid && base
              ? [
                  {
                    id: 'requirements',
                    text: topBase(base)?.requirements[locale] ?? '',
                  },
                ]
              : [],
          flags: state.conditions.map((text, i) => ({ id: `flag-${i}`, text })),
          modifiers: [...state.implicits, ...state.explicits]
            .map((m, i): ItemCardLine => {
              const definition = valid ? definitions?.[m.modifierId] : undefined
              const values = definition
                ? state.conditions.includes('SANCTIFIED')
                  ? preciseModifierProjection(
                      state,
                      definition,
                      m,
                      i < state.implicits.length ? 'implicits' : 'explicits',
                    ).values
                  : catalystProjection(
                      definition,
                      m.values,
                      state.catalystQuality,
                      state,
                    ).values
                : m.values
              return {
                id: `${i}`,
                text: definition
                  ? localizedModifierText(definition, values, locale)
                  : `${m.modifierId}: ${Object.entries(m.values)
                      .map(([key, value]) => `${key} ${value}`)
                      .join(', ')}`,
                fractured: m.fractured,
                kind:
                  i < state.implicits.length
                    ? ('implicit' as const)
                    : ('explicit' as const),
              }
            })
            .concat(
              (state.desecrated ?? [])
                .filter((s) => s.modifierId === null)
                .map((s) => ({
                  id: s.id,
                  text: s.side,
                  unrevealedDesecrated: true,
                  kind: 'desecrated' as const,
                })),
            ),
        }}
      />
    </>
  )
}
