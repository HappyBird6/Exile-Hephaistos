import fs from 'node:fs'
const q='E:/WORK/Exile-Hephaistos/codex/maces-qa-20261005'
for(const [from,to] of [['backend','backend-check'],['frontend','frontend-check']]) {
  fs.cpSync(from,`${q}/${to}`,{recursive:true,filter:p=>!p.split(/[\\/]/).some(s=>['node_modules','dist','build','.gradle'].includes(s))})
}
