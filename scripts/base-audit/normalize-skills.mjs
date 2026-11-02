import fs from 'node:fs'
const root = 'docs/evidence/base-audit-20261102'
const bases = JSON.parse(fs.readFileSync('backend/src/main/resources/catalog/top-bases.json'))
for (const row of JSON.parse(fs.readFileSync(`${root}/import-summary.json`))) bases[row.key].implicitText = JSON.parse(fs.readFileSync(`${root}/${row.slug}.us.json`)).proofs[row.variantIndex].text
const labels = { en: 'Grants Skill:', ko: '스킬 부여:', ja: 'スキルを付与:', 'zh-CN': '获得技能:', 'zh-TW': '賦予技能:', es: 'Otorga la habilidad:' }
const remote = { en: 'us', ko: 'kr', ja: 'jp', 'zh-CN': 'cn', 'zh-TW': 'tw', es: 'sp' }
const policy = { en: 'Temporary example (user policy)', ko: '임시 예시 (사용자 정책)', ja: '仮の例（ユーザー方針）', 'zh-CN': '临时示例（用户策略）', 'zh-TW': '暫時範例（使用者方針）', es: 'Ejemplo temporal (política del usuario)' }
for (const slug of ['Lament_Amulet','Portent_Amulet','Absent_Amulet']) {
  const base = bases[slug.toLowerCase().replaceAll('_','-')]
  for (const locale of Object.keys(remote)) {
    const source = JSON.parse(fs.readFileSync(`${root}/${slug}.${remote[locale]}.json`)).variants[0]
    const parts = source.card.split(labels[locale])
    if (parts.length < 3) throw new Error(`${slug}/${locale}: no source skill alternatives`)
    const line = `${labels[locale]}${parts[1]}`.trim()
    base.skillLines[locale] = [line]
    base.sourceProperties[locale] = [`${policy[locale]}: ${line}`]
  }
}
for (const path of ['backend/src/main/resources/catalog/top-bases.json','frontend/src/features/crafting/topBases.json']) fs.writeFileSync(path, JSON.stringify(bases,null,2)+'\n')
