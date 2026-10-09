import fs from 'node:fs'
const q = 'E:/WORK/Exile-Hephaistos/codex/belts-qa-20261005'
const copy = (from, to, transform) => fs.writeFileSync(`${q}/${to}`, transform(fs.readFileSync(`${q}/${from}`, 'utf8')), { flag: 'wx' })
copy('qa-api-2.mjs', 'qa-api-3.mjs', t => t.replace('http://host.docker.internal:20380', 'http://app:8080').replaceAll('/qa/api-attempt-2', '/qa/api-attempt-3'))
copy('qa-browser.cjs', 'qa-browser-private.cjs', t => t.replaceAll('http://host.docker.internal:20381', 'http://frontend:8080'))
copy('qa-old-filled.cjs', 'qa-old-filled-private.cjs', t => t.replaceAll('http://host.docker.internal:20381', 'http://frontend:8080'))
