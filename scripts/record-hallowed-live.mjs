import fs from 'node:fs'
import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
const phase = process.argv[2]
assert(['before', 'after'].includes(phase))
const q = 'E:/WORK/Exile-Hephaistos/codex/hallowed-qa-20261004'
const path = `${q}/live-${phase}.json`
assert(!fs.existsSync(path), 'Preserve prior evidence')
const names = ['frontend', 'app', 'postgres', 'redis'].map(x => `exile-workbench-qa-20261002-${x}-1`)
const services = names.map(name => ({ name, ...JSON.parse(execFileSync('docker', ['inspect', '--format', '{"id":{{json .Id}},"startedAt":{{json .State.StartedAt}},"running":{{json .State.Running}},"mounts":{{json .Mounts}}}', name], { encoding: 'utf8' })) }))
assert(services.every(s => s.running))
if (phase === 'after') assert.deepEqual(services, JSON.parse(fs.readFileSync(`${q}/live-before.json`, 'utf8')).services)
fs.writeFileSync(path, JSON.stringify({ capturedAt: new Date().toISOString(), services }, null, 2) + '\n')
console.log('Live service IDs/start times/mounts preserved:', phase)
