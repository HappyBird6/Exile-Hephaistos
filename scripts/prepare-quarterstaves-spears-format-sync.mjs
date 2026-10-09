import fs from 'node:fs'
let s=fs.readFileSync('scripts/copy-maces-format-results.mjs','utf8').replaceAll('maces-qa-20261005','quarterstaves-spears-qa-20261005').replaceAll('ReviewedMaces.java','ReviewedQuarterstavesSpears.java').replaceAll('ReviewedMacesTest.java','ReviewedQuarterstavesSpearsTest.java').replaceAll('maces.test.ts','quarterstavesSpears.test.ts')
fs.writeFileSync('scripts/copy-quarterstaves-spears-format-results.mjs',s,{flag:'wx'})
