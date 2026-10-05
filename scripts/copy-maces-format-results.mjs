import fs from 'node:fs'
import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
import { createRequire } from 'node:module'
const area=process.argv[2]
assert(['backend','frontend'].includes(area))
const q='E:/WORK/Exile-Hephaistos/codex/maces-qa-20261005'
const ts=area==='frontend'?createRequire(import.meta.url)(`${q}/frontend-check/node_modules/typescript`):null
function semanticTree(text,path) {
  const emitted=ts.transpileModule(text,{fileName:path,compilerOptions:{target:ts.ScriptTarget.ESNext,module:ts.ModuleKind.ESNext,jsx:ts.JsxEmit.ReactJSX}}).outputText
  const source=ts.createSourceFile('formatted.js',emitted,ts.ScriptTarget.Latest,true,ts.ScriptKind.JS)
  const visit=node=>{
    if(ts.isParenthesizedExpression(node))return visit(node.expression)
    const children=[]
    ts.forEachChild(node,child=>{children.push(visit(child))})
    return {kind:node.kind,...(node.text!==undefined&&!ts.isSourceFile(node)?{text:node.text}:{}),...(node.escapedText!==undefined?{identifier:node.escapedText}:{}),children}
  }
  return visit(source)
}
const extra=area==='backend'?['backend/src/main/java/com/poe2craft/item/ReviewedMaces.java','backend/src/test/java/com/poe2craft/crafting/ReviewedMacesTest.java']:['frontend/src/features/crafting/maces.test.ts']
const paths=[...execFileSync('git',['diff','--name-only','--',area],{encoding:'utf8'}).trim().split('\n').filter(Boolean),...extra]
for(const path of paths) {
  const from=path.replace(area,`${q}/${area}-check`)
  if(area==='backend'&&!path.endsWith('.java'))continue
  assert(fs.existsSync(from),path)
  const before=fs.readFileSync(path,'utf8'),after=fs.readFileSync(from,'utf8')
  if(path.endsWith('.json'))assert.deepEqual(JSON.parse(before),JSON.parse(after),path)
  else if(ts)assert.deepEqual(semanticTree(before,path),semanticTree(after,path),`${path}: emitted semantic AST identical`)
  else assert.equal(before.replace(/\s/g,''),after.replace(/\s/g,''),`${path}: formatting only`)
  fs.copyFileSync(from,path)
}
console.log(area,'formatter results synchronized for own files')
