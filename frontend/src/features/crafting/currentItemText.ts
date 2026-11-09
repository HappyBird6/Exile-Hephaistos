import augments from './augments.json'
import { rolledText } from './workbenchApi'
import type { ConcreteItem } from './workbenchApi'
import type { Definition } from './craftingApi'

// Source rolls only; opaque metadata is retained by the edit endpoint.
export function currentItemText(
  state: ConcreteItem,
  definitions: Record<string, Definition>,
  base: string,
  itemClass: string,
): string {
  const desecrated = state.desecrated ?? []
  const attributes = (slot: NonNullable<ConcreteItem['desecrated']>[number]) =>
    `${slot.minimumModifierLevel != null ? ` (Minimum Modifier Level: ${slot.minimumModifierLevel})` : ''}${slot.revealPool ? ` (Reveal Pool: ${slot.revealPool})` : ''}`
  const lines = [
    `Item Class: ${itemClass}`,
    `Rarity: ${state.rarity[0]}${state.rarity.slice(1).toLowerCase()}`,
  ]
  if (state.rarity === 'RARE') lines.push('Workbench Item')
  lines.push(base, '--------', `Item Level: ${state.itemLevel}`)
  if (state.equipmentQuality != null)
    lines.push(`Quality: +${state.equipmentQuality}%`)
  if (state.augmentSockets)
    lines.push(`Sockets: ${Array(state.augmentSockets).fill('S').join(' ')}`)
  for (const installed of state.installedAugments ?? []) {
    const name = augments.find((a) => a.id === installed.augmentId)?.names.en
    lines.push(
      `Rune: Slot ${installed.slotIndex + 1}: ${name ?? installed.augmentId}`,
    )
  }
  for (const [implicit, modifiers] of [
    [true, state.implicits],
    [false, state.explicits],
  ] as const) {
    if (modifiers.length) lines.push('--------')
    for (const mod of modifiers) {
      const d = definitions[mod.modifierId]
      if (!d) throw new Error('Unknown modifier')
      const used = new Set<string>()
      let text = d.text.replace(
        /\((-?\d+(?:\.\d+)?)\s*[\u2014\u2013-]\s*(-?\d+(?:\.\d+)?)\)/g,
        (original, low: string, high: string) => {
          const matches =
            d.stats?.filter(
              (s) => s.min === Number(low) && s.max === Number(high),
            ) ?? []
          if (matches.length !== 1 || used.has(matches[0]!.id)) return original
          const id = matches[0]!.id
          used.add(id)
          return String(mod.values[id])
        },
      )
      if (d.stats?.some((s) => s.min !== s.max && !used.has(s.id)))
        text = rolledText(d, mod.values)
      const slot = !implicit
        ? desecrated.find((slot) => slot.modifierId === mod.modifierId)
        : undefined
      if (!implicit)
        lines.push(
          `{ ${slot ? 'Desecrated ' : mod.fractured ? 'Fractured ' : ''}${d.affixType === 'PREFIX' ? 'Prefix' : 'Suffix'} Modifier "${d.name}" (Tier: ${d.tier})${slot ? attributes(slot) : ''} }`,
        )
      lines.push(
        ...text
          .split('\n')
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
    lines.push(
      `Desecrated ${slot.side === 'PREFIX' ? 'Prefix' : 'Suffix'}${attributes(slot)}`,
    )
  if (desecrated.length && state.conditions.includes('CORRUPTED')) {
    lines.push('--------', 'Corrupted')
  }
  return lines.join('\n')
}
