import fs from 'node:fs'
import assert from 'node:assert/strict'
const q='E:/WORK/Exile-Hephaistos/codex/base-registry-qa-20261005'
assert(!fs.existsSync(`${q}/checkout`),'Preserve source snapshots')
fs.mkdirSync(`${q}/checkout`)
for(const dir of ['docs','scripts'])fs.cpSync(dir,`${q}/checkout/${dir}`,{recursive:true})
for(const dir of ['backend','frontend'])fs.cpSync(`${q}/${dir}-check/src`,`${q}/checkout/${dir}/src`,{recursive:true})
fs.copyFileSync('frontend/package.json',`${q}/checkout/frontend/package.json`,fs.constants.COPYFILE_EXCL)
fs.writeFileSync(`${q}/importer-check.sh`,'#!/bin/sh\nset -eu\ncd /source\nnode scripts/sync-base-registry.mjs --check > /qa/importer-check.log 2>&1\nnode --test scripts/tests/reviewed-catalog-importer.test.mjs >> /qa/importer-check.log 2>&1\n',{flag:'wx'})
console.log('Read-only source snapshot prepared')
