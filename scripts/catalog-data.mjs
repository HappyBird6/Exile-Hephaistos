import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'

export const jsonBytes = value => Buffer.from(JSON.stringify(value, null, 2) + '\n')
const read = file => JSON.parse(fs.readFileSync(file, 'utf8'))
export const dataFiles = directory => !fs.existsSync(directory) ? [] : fs.readdirSync(directory, { withFileTypes: true }).flatMap(entry => {
  const file = path.join(directory, entry.name)
  return entry.isDirectory() ? dataFiles(file) : [file]
}).sort()

// Resolve ordered shared pools and reviewed context variants for the single generator.
export function catalogResolver(directory, registry, readDocument = read) {
  const { definitions, variants } = registry
  assert.deepEqual(Object.keys(registry).sort(), ['definitions', 'variants'])
  for (const [id, definition] of Object.entries(definitions)) assert.equal(definition.id, id, `Modifier identity mismatch: ${id}`)
  const overrides = new Set(['modifierId', 'name', 'tags', 'tier', 'weight', 'sourceUrl'])
  for (const [ref, variant] of Object.entries(variants)) {
    assert(!Object.hasOwn(definitions, ref), `Ambiguous modifier reference: ${ref}`)
    assert(Object.hasOwn(definitions, variant.modifierId), `Unknown variant definition: ${ref}`)
    assert(Object.keys(variant).every(key => overrides.has(key)), `Unsupported variant field: ${ref}`)
  }
  const resolve = ref => {
    assert(typeof ref === 'string', 'Modifier references must be strings')
    if (Object.hasOwn(definitions, ref)) return definitions[ref]
    assert(Object.hasOwn(variants, ref), `Unknown modifier reference: ${ref}`)
    const { modifierId, ...changes } = variants[ref]
    return { ...definitions[modifierId], ...changes }
  }
  const pools = new Map()
  return (relative, value) => {
    const { modifierPool, implicitModifiers, modifierRefs, ...document } = value
    if (modifierPool === undefined && modifierRefs === undefined) return value
    assert(!Object.hasOwn(value, 'modifiers'), `Mixed inline and referenced modifiers: ${relative}`)
    assert(!(modifierPool !== undefined && modifierRefs !== undefined), `Ambiguous pool: ${relative}`)
    let modifiers
    if (modifierPool !== undefined) {
      const group = relative.split('/')[0]
      if (!pools.has(group)) pools.set(group, readDocument(path.join(directory, group, 'modifier-pools.json')))
      assert(Object.hasOwn(pools.get(group), modifierPool), `Unknown modifier pool: ${relative}:${modifierPool}`)
      modifiers = pools.get(group)[modifierPool].map(resolve)
      assert(modifiers.every(m => m.layer === 'EXPLICIT'), `Implicit definition in shared pool: ${relative}`)
      assert(Array.isArray(implicitModifiers), `Missing implicit placements: ${relative}`)
      let previous = -1
      for (const { index, ref } of implicitModifiers) {
        assert(Number.isInteger(index) && index > previous && index <= modifiers.length, `Invalid implicit position: ${relative}`)
        const implicit = resolve(ref)
        assert.equal(implicit.layer, 'IMPLICIT')
        modifiers.splice(index, 0, implicit)
        previous = index
      }
    } else modifiers = modifierRefs.map(resolve)
    assert.equal(new Set(modifiers.map(m => m.id)).size, modifiers.length, `Duplicate modifier identity: ${relative}`)
    if (document.base) {
      const prefixes = modifiers.filter(m => m.affixType === 'PREFIX')
      const suffixes = modifiers.filter(m => m.affixType === 'SUFFIX')
      document.metadata = { ...document.metadata,
        prefixCount: prefixes.length, suffixCount: suffixes.length,
        prefixWeight: prefixes.reduce((sum, m) => sum + m.weight, 0),
        suffixWeight: suffixes.reduce((sum, m) => sum + m.weight, 0),
      }
    }
    return { ...document, modifiers }
  }
}
