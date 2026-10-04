import fs from 'node:fs'
const root='frontend/src/features/crafting/'
const file=root+'ServiceMessage.tsx',s=fs.readFileSync(file,'utf8'),pos=s.indexOf('export function ServiceMessage')
fs.writeFileSync(root+'serviceMessages.ts',s.slice(0,pos).replace(', useI18n',''))
fs.writeFileSync(file,"import { useI18n } from '../../shared/i18n/i18n'\nimport { serviceText } from './serviceMessages'\n"+s.slice(pos))
for(const p of ['CraftingPage.tsx','remainingDisplay.test.tsx']) {
 const f=root+p,text=fs.readFileSync(f,'utf8').replace("import { ServiceMessage, serviceText } from './ServiceMessage'","import { ServiceMessage } from './ServiceMessage'\nimport { serviceText } from './serviceMessages'")
 fs.writeFileSync(f,text)
}
