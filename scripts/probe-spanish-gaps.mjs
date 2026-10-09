import fs from 'node:fs'
const root=process.argv[2]
for(const id of ['Refined_Necrotic_Catalyst','Necrotic_Catalyst','Liquid_Verisium','Yaomacs_Orb_of_Sacrifice','Kopecs_Orb_of_Sacrifice','Kamasas_Orb_of_Sacrifice','Yuguls_Orb_of_Sacrifice']) {
 const url='https://poe2db.tw/sp/'+id
 const r=await fetch(url),html=await r.text()
 fs.writeFileSync(root+'/es-'+id+'.html',html)
 console.log(id,r.status,html.match(/<title>(.*?)<\/title>/s)?.[1],html.match(/<div class="(?:explicitMod|descrText)">(.*?)<\/div>/s)?.[1]?.replace(/<[^>]*>/g,''))
}
