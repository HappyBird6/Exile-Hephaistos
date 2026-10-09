import fs from 'node:fs'
import assert from 'node:assert/strict'
const q='E:/WORK/Exile-Hephaistos/codex/sceptres-qa-20261005'
const prior=fs.readFileSync(`${q}/qa-api.mjs`,'utf8')
const anchor="  check(key + ' quality cap20'"
const index=prior.indexOf(anchor)
assert(index>0)
const checks=`  for (const [action,state] of [['GREATER_TRANSMUTATION',root],['PERFECT_TRANSMUTATION',root],['GREATER_AUGMENTATION',{...magic,explicits:magic.explicits.slice(0,1)}],['PERFECT_AUGMENTATION',{...magic,explicits:magic.explicits.slice(0,1)}],['GREATER_REGAL',magic],['PERFECT_REGAL',magic],['GREATER_EXALTED',pair],['PERFECT_EXALTED',pair]]) check(key + ' positive higher currency ' + action,(await apply(state,action)).applied)\n`
fs.writeFileSync(`${q}/qa-api-2.mjs`,prior.slice(0,index)+checks+prior.slice(index),{flag:'wx'})
