import fs from 'node:fs'
import path from 'node:path'
import { execFileSync } from 'node:child_process'
const q = 'E:/WORK/Exile-Hephaistos/codex/belts-qa-20261005'
const files = execFileSync('git', ['status', '--porcelain', '--untracked-files=all'], { encoding: 'utf8' }).trimEnd().split('\n').map(l => l.slice(3))
for (const file of files.filter(p => p.startsWith('frontend/src/') || p.endsWith('.java') && p.startsWith('backend/src/'))) {
  const mirror = `${q}/${file.startsWith('frontend/') ? 'frontend' : 'backend'}-check/${file.replace(/^[^/]+\//, '')}`
  if (fs.existsSync(mirror)) fs.copyFileSync(mirror, file)
}
console.log('Copied formatter output only for this branch’s changed source files')
