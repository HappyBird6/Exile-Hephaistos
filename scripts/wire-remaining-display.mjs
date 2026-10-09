import fs from 'node:fs'
for(const p of ['CraftingPage.tsx','CraftSupport.tsx','CraftingExplorer.tsx']) {
 const f='frontend/src/features/crafting/'+p
 let s=fs.readFileSync(f,'utf8')
 if(!s.includes("from './ServiceMessage'"))s=(p==='CraftingPage.tsx'?"import { ServiceMessage, serviceText } from './ServiceMessage'\n":"import { ServiceMessage } from './ServiceMessage'\n")+s
 if(p==='CraftingPage.tsx')s=s.replace("reason: result.reason ?? ''","reason: serviceText(result.reason ?? '')").replace('{issue.message}','<ServiceMessage text={issue.message} />').replace('{uiText(announcement)}','<ServiceMessage text={announcement} />')
 if(p==='CraftSupport.tsx')s=s.replace('{error && <p role="alert">{error}</p>}','{error && <p role="alert"><ServiceMessage text={error} /></p>}').replaceAll('{issue}','<ServiceMessage text={issue} />').replace('{reason}','<ServiceMessage text={reason} />')
 if(p==='CraftingExplorer.tsx')s=s.replace('{error.message}','<ServiceMessage text={error.message} />').replace('{a.reason}','<ServiceMessage text={a.reason} />').replace('{transitions.data.reason}','<ServiceMessage text={transitions.data.reason} />')
 fs.writeFileSync(f,s)
}
