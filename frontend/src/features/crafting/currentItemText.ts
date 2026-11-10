import augments from './augments.json'
import bases from './topBases.json'
import { sanctifiedValues } from './sanctification/sanctification'
import { catalystTypes } from './catalystQuality'
import { modifierClipboardText } from './item-text/modifierClipboardText'
import type { ConcreteItem } from './workbenchApi'
import type { Definition } from './craftingApi'

// Game clipboard sentences; hidden history stays in the session, never in a private text payload.
export function currentItemText(
  state: ConcreteItem,
  definitions: Record<string, Definition>,
  base: string,
  itemClass: string,
): string {
  const desecrated = state.desecrated ?? []
  const lines = [
    `Item Class: ${itemClass}`,
    `Rarity: ${state.rarity[0]}${state.rarity.slice(1).toLowerCase()}`,
  ]
  if (state.rarity === 'RARE') lines.push('Workbench Item')
  lines.push(base, '--------', `Item Level: ${state.itemLevel}`)
  if (state.equipmentQuality != null)
    lines.push(`Quality: +${state.equipmentQuality}%`)
  const labels = {
    FLESH: 'Life',
    NEURAL: 'Mana',
    CARAPACE: 'Defence',
    UUL_NETOL: 'Physical',
    XOPH: 'Fire',
    TUL: 'Cold',
    ESH: 'Lightning',
    CHAYULA: 'Chaos',
    REAVER: 'Attack',
    SIBILANT: 'Caster',
    SKITTERING: 'Speed',
    ADAPTIVE: 'Attribute',
    NECROTIC: 'Minion',
  }
  if (state.catalystQuality) {
    lines.push(
      `Quality (${labels[state.catalystQuality.type]} Modifiers): +${state.catalystQuality.amount}% (augmented)`,
    )
  }
  const sockets = state.augmentSockets ?? state.observations?.socketCount
  if (sockets)
    lines.push('--------', `Sockets: ${Array(sockets).fill('S').join(' ')}`)
  const effects = (state.installedAugments ?? []).flatMap((installed) => {
    const entry = augments.find((a) => a.id === installed.augmentId)
    const effect = (
      entry?.effects as Record<string, { en: string }> | undefined
    )?.[itemClass]?.en
    if (!effect) throw new Error('Unknown installed rune effect')
    return effect.split('\n').map((line) => `${line} (rune)`)
  })
  effects.push(...(state.observations?.augmentEffects ?? []))
  if (effects.length) lines.push('--------', ...effects)
  const recordedSkills = state.observations?.grantedSkills
  const selectedBase = Object.values(bases).find(
    (candidate) => candidate.id === state.baseItemId,
  )
  const baseSkills =
    selectedBase && 'skillLines' in selectedBase
      ? selectedBase.skillLines.en
      : []
  const sourceSkills =
    selectedBase && 'sourceProperties' in selectedBase
      ? selectedBase.sourceProperties.en.filter((line) =>
          line.startsWith('Grants Skill:'),
        )
      : []
  const exampleOnly =
    selectedBase &&
    'temporaryBaseOption' in selectedBase &&
    selectedBase.temporaryBaseOption.kind ===
      'SOURCE_FIRST_GRANTED_SKILL_EXAMPLE'
  const skills = recordedSkills?.length
    ? recordedSkills.map(
        (skill) =>
          `Grants Skill: ${skill.level == null ? '' : `Level ${skill.level} `}${skill.name}`,
      )
    : exampleOnly
      ? []
      : [...new Set([...baseSkills, ...sourceSkills])]
  if (skills.length) lines.push('--------', ...skills)
  for (const [implicit, modifiers] of [
    [true, state.implicits],
    [false, state.explicits],
  ] as const) {
    if (modifiers.length) lines.push('--------')
    for (const mod of modifiers) {
      const d = definitions[mod.modifierId]
      if (!d) throw new Error('Unknown modifier')
      const text = modifierClipboardText(
        d,
        state.sanctification
          ? sanctifiedValues(state, mod, implicit ? 'implicits' : 'explicits')
          : mod.values,
        state.sanctification != null,
      )
      const slot = !implicit
        ? desecrated.find((slot) => slot.modifierId === mod.modifierId)
        : undefined
      const quality = state.catalystQuality
      const enhanced =
        quality &&
        quality.amount > 0 &&
        !d.tags?.includes('unscalable') &&
        catalystTypes[quality.type].tags.some((tag) => d.tags?.includes(tag))
      const scaling = enhanced
        ? ` — ${labels[quality.type]} — ${quality.amount}% Increased`
        : ''
      if (!implicit)
        lines.push(
          `{ ${slot ? 'Desecrated ' : `${mod.fractured ? 'Fractured ' : ''}${d.tags?.includes('crafted') ? 'Crafted ' : ''}`}${d.affixType === 'PREFIX' ? 'Prefix' : 'Suffix'} Modifier "${d.name}"${d.tier > 0 ? ` (Tier: ${d.tier})` : ''}${scaling} }`,
        )
      else if (enhanced) lines.push(`{ Implicit Modifier${scaling} }`)
      lines.push(
        ...text
          .split('\n')
          .filter(Boolean)
          .map(
            (line) =>
              line +
              (implicit
                ? ' (implicit)'
                : slot
                  ? ' (desecrated)'
                  : mod.fractured
                    ? ' (fractured)'
                    : ''),
          ),
      )
    }
  }
  const hidden = desecrated.filter((slot) => slot.modifierId === null)
  if (hidden.length) lines.push('--------')
  for (const slot of hidden)
    lines.push(`Desecrated ${slot.side === 'PREFIX' ? 'Prefix' : 'Suffix'}`)
  if (state.conditions.length)
    lines.push(
      '--------',
      ...state.conditions.map(
        (condition) => condition[0] + condition.slice(1).toLowerCase(),
      ),
    )
  return lines.join('\n')
}
