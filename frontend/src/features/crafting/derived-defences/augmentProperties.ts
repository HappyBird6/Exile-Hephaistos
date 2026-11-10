import type { Definition } from '../craftingApi'
import {
  installedAugmentEffects,
  observedAugmentEffects,
} from '../augmentEffects'
import type { ConcreteItem } from '../workbenchApi'

/** Item-local effects only: conditional combat and character stats do not change equipment totals. */
export function augmentPropertyBonuses(
  state: ConcreteItem,
  bonded: boolean,
  definitions: Record<string, Definition> = {},
) {
  const values = {
    armour: 0,
    evasion: 0,
    energyShield: 0,
    ward: 0,
    armourPercent: 0,
    evasionPercent: 0,
    energyShieldPercent: 0,
    wardPercent: 0,
    physicalPercent: 0,
    physical: [0, 0],
    fire: [0, 0],
    cold: [0, 0],
    lightning: [0, 0],
    chaos: [0, 0],
  }
  const lines = installedAugmentEffects(
    state,
    'en',
    bonded,
    definitions,
  ).flatMap((e) => e.english.split('\n'))
  // Imported unidentified Runes still carry their observed numeric effects.
  lines.push(
    ...observedAugmentEffects(state, bonded, definitions).map((line) =>
      line.replace(/\s*\(rune\)$/, '').trim(),
    ),
  )
  for (const line of lines) {
    let m =
      /^(\d+(?:\.\d+)?)% increased Armour, Evasion and Energy Shield$/.exec(
        line,
      )
    if (m) {
      for (const key of [
        'armourPercent',
        'evasionPercent',
        'energyShieldPercent',
      ] as const)
        values[key] += Number(m[1])
      continue
    }
    m = /^(\d+(?:\.\d+)?)% increased Physical Damage$/.exec(line)
    if (m) {
      values.physicalPercent += Number(m[1])
      continue
    }
    m =
      /^Adds (\d+(?:\.\d+)?) to (\d+(?:\.\d+)?) (Physical|Fire|Cold|Lightning|Chaos) Damage$/.exec(
        line,
      )
    if (m) {
      const key = m[3]!.toLowerCase() as
        'physical' | 'fire' | 'cold' | 'lightning' | 'chaos'
      values[key][0]! += Number(m[1])
      values[key][1]! += Number(m[2])
      continue
    }
    m =
      /^\+(\d+(?:\.\d+)?) to (Armour|Evasion Rating|maximum Energy Shield|maximum Runic Ward)$/.exec(
        line,
      )
    if (m) {
      const key = (
        {
          Armour: 'armour',
          'Evasion Rating': 'evasion',
          'maximum Energy Shield': 'energyShield',
          'maximum Runic Ward': 'ward',
        } as const
      )[m[2] as 'Armour']
      values[key] += Number(m[1])
      continue
    }
    m =
      /^(\d+(?:\.\d+)?)% increased (Armour|Evasion Rating|Energy Shield|Runic Ward)$/.exec(
        line,
      )
    if (m) {
      const key = (
        {
          Armour: 'armourPercent',
          'Evasion Rating': 'evasionPercent',
          'Energy Shield': 'energyShieldPercent',
          'Runic Ward': 'wardPercent',
        } as const
      )[m[2] as 'Armour']
      values[key] += Number(m[1])
    }
  }
  return values
}
