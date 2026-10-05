import fs from 'node:fs'
import path from 'node:path'
import crypto from 'node:crypto'
const q = 'E:/WORK/Exile-Hephaistos/codex/base-registry-qa-20261005'
const evidence = 'docs/evidence/base-registry-refactor-2026-10-05'
const write = (p, s) => fs.writeFileSync(p, s, { flag: 'wx' })
let negative = fs.readFileSync(`${q}/qa-crossclass-filled-1.cjs`, 'utf8')
negative = negative.replace(/for\(const base of \[.*?\]\)/, 'for(const base of ["sapphire","time-lost-sapphire"])')
negative = negative.replace("check(base+' '+l+' legacy film and screenshot',true)", "check(base+' '+l+' unsupported fractured Jewel rejected visibly',await page.locator('[role=alert]').count() === 1)")
negative = negative.replace('old-filled-results.json', 'negative-jewel-results.json')
write(`${q}/qa-negative-jewel-2.cjs`, negative)
fs.mkdirSync(`${q}/browser-negative-1`)
for (const f of ['api-initials.json', 'baseline125-api-initials.json']) fs.copyFileSync(`${q}/browser-attempt-1/${f}`, `${q}/browser-negative-1/${f}`)
for (const [src, target] of [
  ['api-attempt-1/api-results.json', 'api-results.json'],
  ['api-registry-results.json', 'api-registry-results.json'],
  ['browser-attempt-1/browser-results.json', 'browser-results.json'],
  ['browser-attempt-1/old-filled-results.json', 'filled-attempt-1-results.json'],
  ['browser-attempt-2/old-filled-results.json', 'filled-results.json'],
]) write(`${evidence}/${target}`, fs.readFileSync(`${q}/${src}`))
const images = []
for (const folder of ['browser-attempt-1', 'browser-attempt-2']) {
  const walk = dir => {
    for (const f of fs.readdirSync(dir, { withFileTypes: true })) {
      const p = path.join(dir, f.name)
      if (f.isDirectory()) walk(p)
      else if (f.name.endsWith('.png')) images.push({ path: p.replaceAll('\\', '/'), sha256: crypto.createHash('sha256').update(fs.readFileSync(p)).digest('hex') })
    }
  }
  walk(`${q}/${folder}`)
}
write(`${evidence}/screenshots.json`, JSON.stringify({ images }, null, 2) + '\n')
fs.mkdirSync(`${evidence}/screenshots`)
for (const base of ['sapphire', 'time-lost-sapphire', 'bone']) for (const locale of ['en', 'ko', 'zh-CN', 'zh-TW', 'ja', 'es']) {
  const f = `legacy-${base}-${locale}.png`
  write(`${evidence}/screenshots/${f}`, fs.readFileSync(`${q}/browser-attempt-2/cards/${f}`))
}
console.log('Preserved first attempt; saved strengthened film evidence and screenshot hashes')
