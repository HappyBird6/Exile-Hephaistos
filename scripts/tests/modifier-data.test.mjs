import test from 'node:test'
import assert from 'node:assert/strict'
import { expandModifierData } from '../modifier-data.mjs'

function fixture() {
  return {
    schemaVersion: 1,
    normal: {
      groups: {
        hybrid: {
          layer: 'EXPLICIT',
          affixType: 'PREFIX',
          familyIds: ['HybridDefence'],
          tags: ['defences'],
          statIds: ['armour', 'energy_shield'],
          tiers: {
            lesser: {
              tier: 2,
              requiredItemLevel: 1,
              weight: 100,
              ranges: [
                [2, 5],
                [1, 3],
              ],
              sourceRef: 'src1',
            },
            greater: {
              tier: 1,
              requiredItemLevel: 20,
              weight: 50,
              ranges: [
                [6, 9],
                [4, 7],
              ],
              sourceRef: 'src2',
            },
          },
        },
      },
      variants: { 'lesser@old': { modifierId: 'lesser', tier: 3, weight: 80 } },
    },
    essence: {
      definitions: {
        special: {
          layer: 'EXPLICIT',
          affixType: 'PREFIX',
          familyIds: ['HybridDefence'],
          tags: ['defences'],
          tier: 1,
          requiredItemLevel: 72,
          weight: 0,
          stats: [{ id: 'armour', min: 10, max: 15 }],
          sourceRef: 'src3',
        },
      },
    },
  }
}

test('shared fields expand across tiers while IDs, composite ranges and special options stay distinct', () => {
  const source = fixture()
  const original = structuredClone(source)
  const { definitions, variants } = expandModifierData(source)
  assert.deepEqual(Object.keys(definitions), ['lesser', 'greater', 'special'])
  assert.deepEqual(definitions.lesser.stats, [
    { id: 'armour', min: 2, max: 5 },
    { id: 'energy_shield', min: 1, max: 3 },
  ])
  assert.deepEqual(definitions.greater.stats, [
    { id: 'armour', min: 6, max: 9 },
    { id: 'energy_shield', min: 4, max: 7 },
  ])
  assert.equal(definitions.special.tier, definitions.greater.tier)
  assert.equal(definitions.special.weight, 0)
  assert.equal(variants['lesser@old'].modifierId, 'lesser')
  assert.deepEqual(source, original, 'Expansion must not modify authoring data')
  source.normal.groups.hybrid.tags.push('armour')
  const edited = expandModifierData(source).definitions
  assert.deepEqual(edited.lesser.tags, ['defences', 'armour'])
  assert.deepEqual(edited.greater.tags, ['defences', 'armour'])
  assert.deepEqual(edited.special.tags, ['defences'])
})

test('range mismatches, nonfinite values and reversed bounds fail before generating wrong stats', () => {
  for (const ranges of [
    [[2, 5]],
    [
      [2, 5],
      [7, 4],
    ],
    [
      [2, Infinity],
      [1, 3],
    ],
    [
      [2, 5, 6],
      [1, 3],
    ],
  ]) {
    const source = fixture()
    source.normal.groups.hybrid.tiers.lesser.ranges = ranges
    assert.throws(() => expandModifierData(source), /range/i)
  }
})

test('duplicate or unknown tiers cannot silently replace an outcome', () => {
  const source = fixture()
  source.normal.groups.hybrid.tiers.greater.tier = 2
  assert.throws(() => expandModifierData(source), /duplicate tier/)
  source.normal.groups.hybrid.tiers.greater.tier = 0
  assert.throws(() => expandModifierData(source), /Invalid or duplicate tier/)
})

test('duplicate IDs across categories and variants in the wrong category are rejected', () => {
  const source = fixture()
  source.essence.definitions.lesser = source.essence.definitions.special
  assert.throws(
    () => expandModifierData(source),
    /Duplicate modifier definition/,
  )
  delete source.essence.definitions.lesser
  source.essence.variants = source.normal.variants
  delete source.normal.variants
  assert.throws(
    () => expandModifierData(source),
    /Variant changes modifier category/,
  )
})

test('unknown fields cannot override common fields or redirect an ID', () => {
  const source = fixture()
  source.normal.groups.hybrid.tiers.lesser.tags = ['crafted']
  assert.throws(() => expandModifierData(source), /Invalid tier lesser fields/)
  delete source.normal.groups.hybrid.tiers.lesser.tags
  source.essence.definitions.special.id = 'other'
  assert.throws(() => expandModifierData(source), /ID belongs in its key/)
})

test('source-specific tags cannot be filed as ordinary modifiers', () => {
  for (const tag of ['crafted', 'augment_pool', 'reveal_only']) {
    const source = fixture()
    source.normal.groups.hybrid.tags.push(tag)
    assert.throws(() => expandModifierData(source), /Wrong modifier category/)
  }
})
