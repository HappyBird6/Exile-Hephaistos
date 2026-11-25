import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import { isDeepStrictEqual } from 'node:util'
import { catalogResolver, dataFiles, jsonBytes } from './catalog-data.mjs'
import { expandModifierData } from './modifier-data.mjs'

const own = (value, key) => Object.hasOwn(value, key)
const ordered = (value, keys) =>
  Object.fromEntries(
    [...new Set([...keys, ...Object.keys(value)])]
      .filter((key) => own(value, key))
      .map((key) => [key, value[key]]),
  )
const modifierFields = [
  'id',
  'name',
  'layer',
  'affixType',
  'familyIds',
  'requiredItemLevel',
  'weight',
  'tier',
  'text',
  'stats',
  'tags',
  'sourceUrl',
]
const baseFields = [
  'id',
  'name',
  'sourceUrl',
  'implicitModifierId',
  'magicPrefixes',
  'magicSuffixes',
  'rarePrefixes',
  'rareSuffixes',
]
const policyFields = [
  'itemClass',
  'ruleVersion',
  'ledgerVersion',
  'qualityLimit',
  'ordinaryCatalyst',
  'refinedCatalyst',
  'catalystQuality',
  'maximumQualityBreach',
  'initializeImplicit',
  'socketExecutionMaximum',
  'divine',
  'sourcePropertyUnscaled',
  'snapshotDate',
  'snapshotRetrievedAt',
]

// Concrete compatibility projections. Files under docs are the only editable source.
export function gameDataOutputs(directory) {
  const outputs = new Map()
  const consumed = new Set()
  const document = (file) => {
    consumed.add(file)
    return JSON.parse(fs.readFileSync(file, 'utf8'))
  }
  const raw = (relative) => document(path.join(directory, relative))
  const sources = raw('game-data/sources.json')
  const knownRoots = new Set([
    'catalog',
    'modifiers',
    'crafting',
    'assets',
    'sources.json',
  ])
  for (const entry of fs.readdirSync(path.join(directory, 'game-data')))
    assert(knownRoots.has(entry), `Unknown game-data directory: ${entry}`)
  const hydrate = (value) => {
    if (Array.isArray(value)) return value.map(hydrate)
    if (!value || typeof value !== 'object') return value
    return Object.fromEntries(
      Object.entries(value).map(([key, child]) => {
        if (key !== 'sourceRef') return [key, hydrate(child)]
        assert(own(sources, child), `Unknown source reference: ${child}`)
        return ['sourceUrl', sources[child].url]
      }),
    )
  }
  const read = (relative) => hydrate(raw(relative))
  const emit = (file, value, bytes = jsonBytes(value)) => {
    assert(!outputs.has(file), `Duplicate generated resource: ${file}`)
    outputs.set(file, { value, bytes })
  }
  const backend = (file, value) =>
    emit('backend/src/main/resources/' + file, value)
  const browser = (file, value) =>
    emit('frontend/src/features/crafting/' + file, value)

  const text = raw('i18n/catalog/modifiers.en.json')
  const registry = { definitions: {}, variants: {} }
  for (const file of dataFiles(path.join(directory, 'game-data/modifiers'))) {
    const data = hydrate(expandModifierData(document(file)))
    for (const [id, definition] of Object.entries(data.definitions)) {
      assert(
        !own(registry.definitions, id),
        `Duplicate modifier definition: ${id}`,
      )
      assert(
        own(text, id) &&
          typeof text[id].name === 'string' &&
          typeof text[id].text === 'string',
        `Missing modifier text: ${id}`,
      )
      registry.definitions[id] = ordered(
        { ...definition, ...text[id] },
        modifierFields,
      )
    }
    for (const [ref, variant] of Object.entries(data.variants)) {
      assert(!own(registry.variants, ref), `Duplicate modifier variant: ${ref}`)
      registry.variants[ref] = { ...variant, ...(text[ref] ?? {}) }
    }
  }
  const catalogRoot = path.join(directory, 'game-data/catalog')
  const expand = catalogResolver(catalogRoot, registry, (file) =>
    hydrate(document(file)),
  )
  const resolve = (ref) => {
    if (own(registry.definitions, ref)) return registry.definitions[ref]
    assert(own(registry.variants, ref), `Unknown modifier reference: ${ref}`)
    const { modifierId, ...changes } = registry.variants[ref]
    return { ...registry.definitions[modifierId], ...changes }
  }
  const refs = (ids) => {
    assert(Array.isArray(ids), 'Modifier reference list is required')
    const definitions = ids.map(resolve)
    assert.equal(
      new Set(definitions.map((d) => d.id)).size,
      definitions.length,
      'Duplicate modifier ID in projection',
    )
    return definitions
  }

  const names = raw('i18n/catalog/bases.json')
  const initials = raw('i18n/catalog/legacy-initial-text.json')
  const classes = read('game-data/catalog/classes.json')
  const support = read('game-data/crafting/policies/base-support.json')
  const compatibility = read(
    'game-data/crafting/policies/catalog-compatibility.json',
  )
  const bases = new Map(),
    registrations = [],
    legacy = []
  for (const file of dataFiles(catalogRoot).filter((file) =>
    file.endsWith('/base.json'),
  )) {
    const { registration, legacy: old, ...identity } = hydrate(document(file))
    assert(!bases.has(identity.id), `Duplicate base: ${identity.id}`)
    assert(own(names, identity.id), `Missing base text: ${identity.id}`)
    const display = names[identity.id]
    const base = ordered({ ...identity, name: display.name }, baseFields)
    bases.set(identity.id, { file, base })
    if (registration) {
      const { order, catalogPool, ...entry } = registration
      assert(Number.isInteger(order), `Missing base order: ${identity.id}`)
      assert(own(classes, entry.family), `Unknown item class: ${entry.family}`)
      if (entry.implicitModifierId?.fromBase)
        entry.implicitModifierId = base.implicitModifierId
      if (entry.affixLimits?.fromBase)
        entry.affixLimits = Object.fromEntries(
          baseFields.slice(4).map((key) => [key, base[key]]),
        )
      const { name, ...localized } = display
      registrations.push({
        order,
        entry: ordered(
          {
            ...entry,
            pool: catalogPool ?? path.basename(path.dirname(file)),
            id: base.id,
            name,
            sourceUrl: entry.sourceUrl ?? base.sourceUrl,
            ...localized,
          },
          ['key', 'family', 'pool', 'slug', 'id', 'name', 'sourceUrl'],
        ),
      })
    }
    if (old) {
      assert(own(initials, old.key), `Missing legacy initial text: ${old.key}`)
      legacy.push({
        order: old.order,
        key: old.key,
        value: {
          id: base.id,
          slug: old.slug,
          initialText: initials[old.key],
          ...support.legacy[old.key],
          ...compatibility.legacy[old.key],
        },
      })
    }
  }
  const top = Object.fromEntries(
    registrations
      .sort((a, b) => a.order - b.order)
      .map(({ entry }) => [entry.key, entry]),
  )
  assert.equal(
    Object.keys(top).length,
    registrations.length,
    'Duplicate base registration key',
  )
  const oldBases = Object.fromEntries(
    legacy.sort((a, b) => a.order - b.order).map((row) => [row.key, row.value]),
  )
  const baseKeys = Object.fromEntries(
    [...Object.entries(top), ...Object.entries(oldBases)].map(([key, base]) => [
      base.id,
      key,
    ]),
  )
  const policies = {
    schemaVersion: support.schemaVersion,
    families: Object.fromEntries(
      Object.entries(classes).map(([key, facts]) => [
        key,
        ordered(
          {
            ...facts,
            ...support.families[key],
            ...compatibility.families[key],
          },
          policyFields,
        ),
      ]),
    ),
    baseOverrides: Object.fromEntries(
      [
        ...new Set([
          ...Object.keys(support.baseOverrides),
          ...Object.keys(compatibility.baseOverrides),
        ]),
      ].map((key) => [
        key,
        { ...support.baseOverrides[key], ...compatibility.baseOverrides[key] },
      ]),
    ),
    legacy: oldBases,
  }
  backend('catalog/top-bases.json', top)
  backend('catalog/base-policies.json', policies)

  const poolModifiers = new Map()
  const jewelDefinitions = {}
  for (const file of dataFiles(catalogRoot)) {
    const relative = path.relative(catalogRoot, file).replaceAll('\\', '/')
    if (
      relative === 'classes.json' ||
      file.endsWith('/base.json') ||
      file.endsWith('/modifier-pools.json')
    )
      continue
    assert(
      relative.split('/').length === 3 &&
        (path.basename(file) === 'catalog.json' ||
          file.endsWith('.catalog.json')),
      `Unexpected catalog source; archive evidence separately: ${relative}`,
    )
    let value = hydrate(document(file))
    if (path.basename(file) === 'catalog.json') {
      const identity = [...bases.values()].find(
        (row) => path.dirname(row.file) === path.dirname(file),
      )
      assert(identity, `Missing base identity: ${relative}`)
      value = {
        metadata: value.metadata,
        base: identity.base,
        ...Object.fromEntries(
          Object.entries(value).filter(([key]) => key !== 'metadata'),
        ),
      }
    }
    value = expand(relative, value)
    const pool = path.basename(path.dirname(file))
    if (!poolModifiers.has(pool)) poolModifiers.set(pool, new Map())
    for (const definition of value.modifiers)
      poolModifiers.get(pool).set(definition.id, definition)
    backend('catalog/' + relative.split('/').slice(1).join('/'), value)
    if (value.base?.id?.includes('/Jewels/')) {
      for (const modifier of value.modifiers.filter(
        (d) => d.layer === 'EXPLICIT',
      ))
        jewelDefinitions[modifier.id] = {
          baseItemId: value.base.id,
          affixType: modifier.affixType,
          familyIds: modifier.familyIds,
          stats: modifier.stats,
          crafted: modifier.tags.includes('crafted'),
        }
    }
  }
  for (const base of Object.values(top))
    assert(
      outputs.has(
        `backend/src/main/resources/catalog/${base.pool}/catalog.json`,
      ),
      `Unknown base catalog: ${base.pool}`,
    )

  // The same groups/bases representation serves essences, alloys, liquids and manual reveal.
  const bindings = (file) => {
    const { groups, bases: membership } = read(file)
    for (const [group, actions] of Object.entries(groups)) {
      for (const table of Object.values(actions))
        for (const ids of Object.values(table)) refs(ids)
      assert(
        Object.values(membership).includes(group),
        `Unused material binding group: ${group}`,
      )
    }
    return Object.fromEntries(
      Object.entries(membership).map(([base, group]) => {
        assert(bases.has(base), `Unknown binding base: ${base}`)
        assert(own(groups, group), `Unknown binding group: ${group}`)
        return [base, groups[group]]
      }),
    )
  }
  const essenceBindings = bindings('game-data/crafting/essences/targets.json')
  const alloyBindings = bindings('game-data/crafting/alloys/targets.json')
  const essences = Object.fromEntries(
    Object.entries(essenceBindings)
      .filter(
        ([id, row]) =>
          row.fixed &&
          Object.values(top).some(
            (base) =>
              base.id === id &&
              !(
                compatibility.baseOverrides[base.key]?.legacyCatalog ??
                compatibility.families[base.family]?.legacyCatalog
              ),
          ),
      )
      .map(([id, row]) => [
        baseKeys[id],
        { fixed: row.fixed, replacements: row.replacements },
      ]),
  )
  backend('catalog/top-base-essences.json', essences)
  const equipmentTargets = {}
  // Essence entries precede alloys: the legacy loader hashes their insertion order.
  for (const data of [essenceBindings, alloyBindings])
    for (const [id, row] of Object.entries(data))
      if (row.additional)
        equipmentTargets[id] = { ...equipmentTargets[id], ...row.additional }
  backend('catalog/equipment-currencies.json', {
    definitions: refs([
      ...new Set([
        ...read('game-data/crafting/alloys/modifier-refs.json'),
        ...Object.values(essenceBindings).flatMap((row) =>
          Object.values(row.additional ?? {}).flat(),
        ),
      ]),
    ]),
    targets: equipmentTargets,
    previousSnapshots: compatibility.previousEquipmentSnapshots,
  })
  browser(
    'basicJewelLiquidTargets.json',
    Object.fromEntries(
      Object.entries(bindings('game-data/crafting/liquids/targets.json')).map(
        ([id, row]) => [baseKeys[id], row.fixed],
      ),
    ),
  )

  const augmentText = raw('i18n/augments/items.json')
  const augments = read('game-data/crafting/augments/items.json').map(
    (item) => {
      assert(own(augmentText, item.id), `Missing augment text: ${item.id}`)
      return ordered({ ...item, ...augmentText[item.id] }, [
        'id',
        'names',
        'effects',
        'sources',
        'image',
        'selectionEnabled',
        'kind',
        'properties',
        'sourceUrl',
        'limit',
        'legacyLimitExempt',
      ])
    },
  )
  backend('catalog/augments.json', augments)
  backend(
    'catalog/augment-effect-templates.json',
    raw('i18n/augments/effect-templates.json'),
  )
  const { modifierRefs, ...augmentRules } = read(
    'game-data/crafting/augments/rules.json',
  )
  const ruleText = raw('i18n/augments/rule-descriptions.en.json')
  augmentRules.modifiers = refs(modifierRefs)
  augmentRules.magnitudeRules = augmentRules.magnitudeRules.map((rule) => {
    assert(
      own(ruleText, rule.stat),
      `Missing magnitude description: ${rule.stat}`,
    )
    return ordered({ ...rule, description: ruleText[rule.stat] }, [
      'stat',
      'tags',
      'description',
      'consumers',
    ])
  })
  backend(
    'catalog/augment-crafting.json',
    ordered(augmentRules, [
      'schemaVersion',
      'socketBound',
      'craftedModifierIds',
      'modifiers',
      'pools',
      'effects',
      'magnitudeRules',
      'poolWeightPolicy',
    ]),
  )

  const {
    modifierRefs: revealRefs,
    selectionPolicy,
    groups,
    bases: membership,
    ...revealSource
  } = read('game-data/crafting/reveal/options.json')
  assert.equal(selectionPolicy.mode, 'manual-only')
  assert.equal(
    selectionPolicy.probability,
    null,
    'Manual reveal has no reviewed probability distribution',
  )
  const reveal = {
    ...revealSource,
    selectionPolicy: selectionPolicy.eligibilityNote,
    definitions: refs(revealRefs),
    bases: Object.fromEntries(
      Object.entries(bindings('game-data/crafting/reveal/options.json')).map(
        ([id, row]) => [id, row.reveal.modifierRefs],
      ),
    ),
  }
  assert(
    reveal.definitions.every(
      (row) => row.weight === 0 && row.tags.includes('reveal_only'),
    ),
    'Invalid manual reveal definition',
  )
  backend('catalog/manual-reveal.json', reveal)
  browser('manualRevealDefinitions.json', {
    definitions: Object.fromEntries(reveal.definitions.map((d) => [d.id, d])),
    bases: reveal.bases,
  })
  browser('basicJewelDefinitions.json', jewelDefinitions)

  const materialText = raw('i18n/materials/en.json')
  browser(
    'materialTooltips.json',
    Object.fromEntries(
      Object.entries(read('game-data/crafting/materials/items.json')).map(
        ([id, facts]) => {
          assert(
            own(materialText, id) && Array.isArray(materialText[id].lines),
            `Missing material text: ${id}`,
          )
          return [id, { ...materialText[id], ...facts }]
        },
      ),
    ),
  )
  browser(
    'materialTargetClasses.json',
    read('game-data/crafting/materials/target-classes.json'),
  )
  browser(
    'base-picker/baseImageSources.json',
    read('game-data/assets/base-images.json'),
  )

  const materialPolicies = read('game-data/crafting/materials/execution.json')
  const sourceOnly = new Set([
    'policies',
    'essences',
    'alloys',
    'liquids',
    'augments',
    'reveal',
    'materials',
  ])
  for (const file of dataFiles(path.join(directory, 'game-data/crafting'))) {
    const relative = path
      .relative(path.join(directory, 'game-data/crafting'), file)
      .replaceAll('\\', '/')
    if (sourceOnly.has(relative.split('/')[0])) continue
    const value = read('game-data/crafting/' + relative)
    if (relative === 'workbench-definitions-v1.json') {
      assert.deepEqual(
        Object.keys(materialPolicies.actions).sort(),
        value.currencies.map((c) => c.id).sort(),
        'Material policies must cover registered actions',
      )
      for (const currency of value.currencies) {
        const profile = materialPolicies.actions[currency.id]
        assert(
          own(materialPolicies.profiles, profile),
          `Unknown material profile: ${profile}`,
        )
        currency.materialPolicy = materialPolicies.profiles[profile]
      }
      // Resolve the same base overrides and catalog membership as WorkbenchMaterials.
      // The browser loads just the selected base's row through catalogDataPlugin.
      const actions = Object.fromEntries(
        Object.entries(materialPolicies.actions).filter(
          ([, profile]) => materialPolicies.profiles[profile].mode !== 'NONE',
        ),
      )
      const rows = [],
        byBase = {},
        rowIds = new Map()
      for (const [id, key] of Object.entries(baseKeys)) {
        const pool =
          top[key]?.pool ?? path.basename(path.dirname(bases.get(id).file))
        const catalog = new Map(poolModifiers.get(pool))
        const equipment = Object.values(equipmentTargets[id] ?? {}).flat()
        const augments = Object.values(augmentRules.pools[id] ?? {}).flatMap(
          Object.keys,
        )
        for (const option of [...equipment, ...augments])
          if (!catalog.has(option)) catalog.set(option, resolve(option))
        const liquidTargets = new Map()
        for (const d of catalog.values()) {
          if (!d.tags.includes('crafted')) continue
          if (!liquidTargets.has(d.sourceUrl))
            liquidTargets.set(d.sourceUrl, [])
          liquidTargets.get(d.sourceUrl).push(d.id)
        }
        const targets = {},
          details = {}
        const row = {
          targets,
          details,
          coupled: [...catalog.values()].some(
            (d) =>
              (own(top, key) || d.layer === 'EXPLICIT') && d.stats.length > 1,
          ),
        }
        for (const currency of value.currencies) {
          const action = currency.id
          if (!own(actions, action)) continue
          const policy = currency.materialPolicy
          const additional = equipmentTargets[id]?.[action]
          const ids =
            policy.kind === 'LIQUID'
              ? (liquidTargets.get(currency.replacementSource) ?? [])
              : policy.mode === 'ADD'
                ? (essenceBindings[id]?.fixed?.[action] ??
                  currency.essenceModifierIds)
                : (additional ??
                  essenceBindings[id]?.replacements?.[action] ??
                  currency.replacementModifiers)
          if (!ids.length) continue
          const options = ids.map((option) => catalog.get(option))
          if (!options.every(Boolean)) continue
          targets[action] = {
            ids,
            level: Math.max(
              currency.minimumModifierLevel,
              ...options.map((d) => d.requiredItemLevel),
            ),
          }
          for (const d of options) {
            if (additional || d.weight === 0)
              details[d.id] = { sourceUrl: d.sourceUrl, stats: d.stats }
          }
        }
        const signature = JSON.stringify(row)
        if (!rowIds.has(signature)) {
          rowIds.set(signature, rows.length)
          rows.push(row)
        }
        byBase[key] = rowIds.get(signature)
      }
      browser('materialVerification.json', {
        actions,
        profiles: materialPolicies.profiles,
        byBase,
        rows,
      })
      value.baseTargets = Object.fromEntries(
        Object.entries(essenceBindings)
          .filter(([, row]) => row.fixed || row.replacements)
          .map(([id, row]) => [
            id,
            { fixed: row.fixed ?? {}, replacements: row.replacements ?? {} },
          ]),
      )
    }
    if (relative === 'registry-v2.json')
      for (const entry of value.entries) {
        assert(
          own(materialText, entry.id),
          `Missing registry name: ${entry.id}`,
        )
        entry.name = materialText[entry.id].name
      }
    // Historical readonly resources retain their original bytes and digests.
    const sourceBytes = fs.readFileSync(file)
    emit(
      'backend/src/main/resources/crafting/' + relative,
      value,
      isDeepStrictEqual(JSON.parse(sourceBytes), value)
        ? sourceBytes
        : jsonBytes(value),
    )
  }
  for (const file of dataFiles(path.join(directory, 'i18n'))) {
    const relative = path
      .relative(path.join(directory, 'i18n'), file)
      .replaceAll('\\', '/')
    if (['catalog', 'augments', 'materials'].includes(relative.split('/')[0]))
      continue
    consumed.add(file)
    const bytes = fs.readFileSync(file)
    emit('frontend/src/shared/i18n/' + relative, JSON.parse(bytes), bytes)
  }
  for (const folder of ['game-data', 'i18n']) {
    for (const file of dataFiles(path.join(directory, folder)))
      assert(
        consumed.has(file),
        `Source has no generation rule: ${path.relative(directory, file)}`,
      )
  }
  return outputs
}
