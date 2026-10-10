// Run from repository root. The frozen shared schema is the only wire-shape source.
import { readFileSync, writeFileSync, copyFileSync } from 'node:fs'
const schema = JSON.parse(
  readFileSync('contracts/crafting-paths-v1/schema.json', 'utf8'),
)
copyFileSync(
  'contracts/crafting-paths-v1/schema.json',
  'frontend/src/features/crafting/path-tree/schema.generated.json',
)
const name = (s) => s[0].toUpperCase() + s.slice(1)
function type(s) {
  if (s.$ref) return name(s.$ref.split('/').at(-1))
  if ('const' in s) return JSON.stringify(s.const)
  if (s.enum) return s.enum.map(JSON.stringify).join(' | ')
  if (s.anyOf) return s.anyOf.map(type).join(' | ')
  if (s.type === 'array') return `Array<${s.items ? type(s.items) : 'never'}>`
  if (s.type === 'object') {
    if (!s.properties) return `Record<string, ${type(s.additionalProperties)}>`
    return `{ ${Object.entries(s.properties)
      .map(([k, v]) => `${k}${s.required?.includes(k) ? '' : '?'}: ${type(v)}`)
      .join('; ')} }`
  }
  return s.type === 'integer' ? 'number' : s.type
}
writeFileSync(
  'frontend/src/features/crafting/path-tree/types.ts',
  '// Generated from contracts/crafting-paths-v1/schema.json. Do not edit wire types by hand.\n' +
    Object.entries(schema.definitions)
      .map(([k, v]) => `export type ${name(k)} = ${type(v)}\n`)
      .join(''),
)
