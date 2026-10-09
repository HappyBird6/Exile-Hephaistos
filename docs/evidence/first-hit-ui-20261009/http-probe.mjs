import fs from 'node:fs'
import assert from 'node:assert/strict'
import { fileURLToPath } from 'node:url'
import ts from '../../../frontend/node_modules/typescript/lib/typescript.js'
const root = fileURLToPath(new URL('../../../', import.meta.url))
const data = (s) => 'data:text/javascript;base64,' + Buffer.from(s).toString('base64')
const compile = (p) => ts.transpileModule(fs.readFileSync(root + p, 'utf8'), { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.ESNext } }).outputText
const rules = compile('/frontend/src/features/crafting/rulesetIdentity.ts').replace(/import \{ translate \} from ['"]\.\.\/\.\.\/shared\/i18n\/i18n['"];?/, 'const translate = (key) => key;')
const source = compile('/frontend/src/features/crafting/basic-paths/api.ts').replace(/['"]\.\.\/rulesetIdentity['"]/, JSON.stringify(data(rules)))
const api = await import(data(source))
const nativeFetch = globalThis.fetch
globalThis.fetch = (url, options) => nativeFetch(new URL(url, 'http://127.0.0.1:18089'), options)
const signal = new AbortController().signal
const initialResponse = await fetch('/api/v1/crafting/initial?itemLevel=82')
assert.equal(initialResponse.status, 200)
const initial = await initialResponse.json()
const provenance = await api.loadProvenance(signal)
assert.equal(initial.rulesetIdentity, provenance.rulesetIdentity)
const target = initial.modifiers['amulet:prefix:apprentice-s']
assert.ok(target.text.includes('Spell Damage'))
const first = Object.values(initial.modifiers).find((d) => d.layer === 'EXPLICIT' && d.id !== target.id && d.stats?.length === 1 && d.requiredItemLevel <= 82)
const instance = (d) => ({ modifierId: d.id, values: { [d.stats[0].id]: d.stats[0].min }, fractured: false })
const item = { snapshotId: initial.state.snapshotId, baseItemId: initial.state.baseItemId, itemLevel: 82, rarity: 'RARE', implicits: initial.state.implicits, explicits: [instance(first)], conditions: initial.state.conditions, augmentSockets: initial.augmentSockets ?? null, catalystQuality: null }
const request = { start: { item, provenance }, target: { checkpoint: null, explicitModifierIds: [target.id] }, policy: { actions: ['CHAOS'], mode: 'REPEAT_CYCLE' }, activeOmens: [], observations: [100, 300, 500] }
const complete = await api.evaluatePath(request, false, signal)
assert.ok(complete.points.every((p) => p.status === 'COMPLETE' && p.unresolved.numerator === '0'))
const second = Object.values(initial.modifiers).find((d) => d.layer === 'EXPLICIT' && d.affixType !== first.affixType && d.stats?.length === 1 && d.requiredItemLevel <= 82)
const partialRequest = structuredClone(request)
partialRequest.start.item.explicits.push(instance(second))
const partial = await api.evaluatePath(partialRequest, false, signal)
assert.ok(partial.points.some((p) => p.status !== 'COMPLETE'))
const unsupportedRequest = structuredClone(request)
unsupportedRequest.activeOmens = ['Omen_of_Sinistral_Annulment']
const unsupported = await api.evaluatePath(unsupportedRequest, false, signal)
assert.ok(unsupported.blockers.some((b) => b.status === 'UNSUPPORTED'))
assert.ok(unsupported.points.every((p) => p.status === 'UNKNOWN'))
const recoveryRequest = structuredClone(request)
recoveryRequest.policy.mode = 'SINGLE_PASS'
recoveryRequest.start.item.explicits = [instance(second)]
recoveryRequest.target = { checkpoint: request.start, explicitModifierIds: [] }
const recovery = await api.evaluatePath(recoveryRequest, true, signal)
assert.equal(recovery.purpose, 'CONDITIONAL_RECOVERY')
assert.equal(recovery.recoveryIncludedInMain, false)
const summary = (r) => ({ purpose: r.purpose, policy: r.request.policy, points: r.points.map((p) => ({ attempts: p.attempts, status: p.status, lower: api.percent(p.lower), upper: api.percent(p.upper), unresolved: api.percent(p.unresolved), numeratorDigits: p.lower.numerator.length, denominatorDigits: p.lower.denominator.length })), reason: r.reason, blockers: r.blockers.map((b) => ({ action: b.action, status: b.status, reason: b.reason })), recoveryIncludedInMain: r.recoveryIncludedInMain })
const evidence = { type: 'HTTP API + production frontend adapter probe; not a browser test', sourceBaseSha: '8e56fee71b73f15935466c7fc124bf097e07aa11', target: { id: target.id, name: target.name, text: target.text }, provenance, complete: summary(complete), partial: summary(partial), unsupported: summary(unsupported), recovery: summary(recovery) }
fs.mkdirSync(root + '/docs/evidence/first-hit-ui-20261009', { recursive: true })
fs.writeFileSync(root + '/docs/evidence/first-hit-ui-20261009/http-probe.json', JSON.stringify(evidence, null, 2) + '\n')
console.log(JSON.stringify({ complete: evidence.complete.points, partial: evidence.partial.points, unsupported: evidence.unsupported.points, recovery: evidence.recovery.points }, null, 2))
