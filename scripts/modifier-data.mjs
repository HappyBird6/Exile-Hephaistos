import assert from 'node:assert/strict'

export const modifierCategories = [
  'implicit',
  'normal',
  'essence',
  'desecrated',
  'augment',
  'alloy',
  'liquid',
]
const own = (value, key) => Object.hasOwn(value, key)
const fields = (value, expected, label) => {
  assert.deepEqual(
    Object.keys(value).sort(),
    [...expected].sort(),
    `Invalid ${label} fields`,
  )
}

// Authoring groups are expanded only at build time; runtime IDs and pools stay unchanged.
export function expandModifierData(source) {
  assert.equal(source.schemaVersion, 1, 'Unknown modifier source schema')
  assert(
    Object.keys(source).every(
      (key) => key === 'schemaVersion' || modifierCategories.includes(key),
    ),
    'Unknown modifier category',
  )
  const definitions = {},
    variants = {},
    categories = {},
    variantCategories = {}
  const add = (id, definition, category) => {
    assert(!own(definitions, id), `Duplicate modifier definition: ${id}`)
    assert(!own(definition, 'id'), `Modifier ID belongs in its key: ${id}`)
    assert.equal(
      definition.layer,
      category === 'implicit' ? 'IMPLICIT' : 'EXPLICIT',
      `Wrong modifier layer: ${id}`,
    )
    const specialTags = {
      augment_pool: 'augment',
      reveal_only: 'desecrated',
      crafted: 'liquid',
    }
    for (const [tag, expected] of Object.entries(specialTags)) {
      if (definition.tags.includes(tag))
        assert.equal(category, expected, `Wrong modifier category: ${id}`)
    }
    definitions[id] = { id, ...definition }
    categories[id] = category
  }
  for (const category of modifierCategories) {
    const section = source[category]
    if (section === undefined) continue
    assert(
      Object.keys(section).every((key) =>
        ['groups', 'definitions', 'variants'].includes(key),
      ),
      `Unknown modifier section field: ${category}`,
    )
    for (const [key, group] of Object.entries(section.groups ?? {})) {
      fields(
        group,
        ['layer', 'affixType', 'familyIds', 'tags', 'statIds', 'tiers'],
        `modifier group ${key}`,
      )
      const { statIds, tiers, ...common } = group
      assert(
        Array.isArray(statIds) &&
          statIds.length > 0 &&
          statIds.every((id) => typeof id === 'string') &&
          new Set(statIds).size === statIds.length,
        `Invalid stat IDs: ${key}`,
      )
      const tierNumbers = new Set()
      for (const [id, row] of Object.entries(tiers)) {
        fields(
          row,
          ['tier', 'requiredItemLevel', 'weight', 'ranges', 'sourceRef'],
          `tier ${id}`,
        )
        assert(
          Number.isInteger(row.tier) &&
            row.tier > 0 &&
            !tierNumbers.has(row.tier),
          `Invalid or duplicate tier: ${id}`,
        )
        tierNumbers.add(row.tier)
        assert(
          Number.isInteger(row.requiredItemLevel) && row.requiredItemLevel >= 1,
          `Invalid modifier level: ${id}`,
        )
        assert(
          Number.isFinite(row.weight) && row.weight >= 0,
          `Invalid modifier weight: ${id}`,
        )
        assert.equal(
          typeof row.sourceRef,
          'string',
          `Missing modifier source: ${id}`,
        )
        assert(
          Array.isArray(row.ranges) && row.ranges.length === statIds.length,
          `Stat range count differs: ${id}`,
        )
        const { ranges, ...values } = row
        const stats = ranges.map((range, index) => {
          assert(
            Array.isArray(range) &&
              range.length === 2 &&
              range.every(Number.isFinite) &&
              range[0] <= range[1],
            `Invalid stat range: ${id}`,
          )
          return { id: statIds[index], min: range[0], max: range[1] }
        })
        add(id, { ...common, ...values, stats }, category)
      }
      assert(tierNumbers.size > 0, `Empty modifier group: ${key}`)
    }
    for (const [id, definition] of Object.entries(section.definitions ?? {}))
      add(id, definition, category)
    for (const [ref, variant] of Object.entries(section.variants ?? {})) {
      assert(!own(variants, ref), `Duplicate modifier variant: ${ref}`)
      variants[ref] = variant
      variantCategories[ref] = category
    }
  }
  for (const [ref, variant] of Object.entries(variants)) {
    assert(!own(definitions, ref), `Ambiguous modifier reference: ${ref}`)
    assert(
      own(definitions, variant.modifierId),
      `Unknown variant definition: ${ref}`,
    )
    assert.equal(
      variantCategories[ref],
      categories[variant.modifierId],
      `Variant changes modifier category: ${ref}`,
    )
  }
  return { definitions, variants }
}
