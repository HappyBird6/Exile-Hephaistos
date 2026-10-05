import fs from 'node:fs'
import assert from 'node:assert/strict'
const q='E:/WORK/Exile-Hephaistos/codex/'+(process.env.CROSSBOW_REVIEW_NAME??'crossbows-source-review-20261005')
assert(!fs.existsSync(q),'Preserve prior output')
fs.mkdirSync(q)
for(const name of ['backend','frontend','scripts','docs/evidence/crossbows-source-bundle-2026-10-05'])fs.cpSync(name,`${q}/${name}`,{recursive:true,filter:p=>!p.split(/[\\/]/).some(s=>['node_modules','build','dist','.gradle','coverage'].includes(s))})
console.log(q)
