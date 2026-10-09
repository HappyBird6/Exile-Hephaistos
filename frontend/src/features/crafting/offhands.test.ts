import { describe, expect, it } from 'vitest'
import { topBase, topBaseKey, reviewedArmourKeys } from './topBases'
import { useItemDraft } from './draft'
import terms from '../../shared/i18n/gameTerms.json'
import { maximumQuality } from './qualityLimit'

describe('independent reviewed offhand classes', () => {
  it.each([
    ['tawhoan-tower-shield', 'shields', 'Shields', 264, 0, 0, 'Raise Shield'],
    ['golden-targe', 'shields', 'Shields', 145, 132, 0, 'Raise Shield'],
    [
      'blacksteel-crest-shield',
      'shields',
      'Shields',
      145,
      0,
      40,
      'Raise Shield',
    ],
    ['desert-buckler', 'bucklers', 'Bucklers', 0, 192, 0, 'Parry'],
    ['tasalian-focus', 'foci', 'Foci', 0, 0, 91, null],
  ] as const)(
    'preserves source identity, defence and skill for %s',
    (key, family, itemClass, armour, evasion, energyShield, skill) => {
      const base = topBase(key)!
      expect(base.family).toBe(family)
      expect(base.id).toContain('/Armours/')
      expect(base.id).not.toContain('/Weapons/')
      expect(topBaseKey(base.id)).toBe(key)
      expect(reviewedArmourKeys).toContain(key)
      expect(base.armour).toBe(armour)
      expect(base.evasion).toBe(evasion)
      expect(base.energyShield).toBe(energyShield)
      expect(base.requiredLevel).toBe(80)
      expect(base.implicitStats).toEqual([])
      expect(base.sourceProperties?.en).toEqual(
        expect.arrayContaining(
          skill ? ['Grants Skill: ' + skill] : ['Energy Shield: 91'],
        ),
      )
      for (const locale of [
        'en',
        'ko',
        'ja',
        'zh-CN',
        'zh-TW',
        'es',
      ] as const) {
        expect(base.requirements[locale]).toContain('80')
        const term = (
          terms[locale] as Record<
            string,
            { itemKey?: string; lines: string[]; name: string }
          >
        )[base.slug]!
        expect(term.itemKey).toBe(base.id)
        expect(term.lines).toEqual(base.sourceProperties?.[locale])
        expect(term.name).not.toBe('')
      }
      useItemDraft.getState().setBase(82, key)
      expect(useItemDraft.getState().text).toContain('Item Class: ' + itemClass)
      expect(useItemDraft.getState().text).toContain(base.name)
      expect(
        maximumQuality(
          {
            snapshotId: '',
            baseItemId: base.id,
            itemLevel: 82,
            rarity: 'NORMAL',
            implicits: [],
            explicits: [],
            conditions: [],
          },
          {},
        ),
      ).toBe(20)
    },
  )
})
