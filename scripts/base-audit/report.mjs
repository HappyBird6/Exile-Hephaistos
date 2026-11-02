import fs from 'node:fs'
const read = (path) => JSON.parse(fs.readFileSync(path,'utf8'))
const root = 'docs/evidence/base-audit-20261102'
const bases = read('backend/src/main/resources/catalog/top-bases.json')
const legacy = read('backend/src/main/resources/catalog/base-policies.json').legacy
const imported = read(`${root}/import-summary.json`)
const inventories = [
  ...read('docs/evidence/top-base-inventory-2026-10-04/inventory.json'),
  ...read('docs/evidence/belts-source-bundle-2026-10-05/next-equipment/inventory.json'),
  ...read('docs/evidence/staves-talismans-source-bundle-2026-10-05/roster-review.json').inventory ?? [],
]
const registrySlugs = new Set([...Object.values(bases).map((base) => base.sourceUrl.split('/').at(-1)), ...Object.values(legacy).map((base) => base.slug.replaceAll(' ','_'))])
const inventoryRows = inventories.filter((page) => Array.isArray(page.cards)).map((page) => ({ page: page.page, source: page.url, retrievedAt: page.retrievedAt, sha256: page.sha256, rows: page.cards.map((row) => ({ ...row, status: registrySlugs.has(row.slug) ? 'REGISTERED_IDENTITY_OR_VARIANT' : /^Rune(forged|mastered)_/.test(row.slug) ? 'UNREGISTERED_RUNIC_WARD_ALLOY_VARIANT' : 'UNREGISTERED_PROGRESSION_OR_RELEASE_CANDIDATE' })) }))
const pendingSlugs = ['Golden_Hoop','Abyssal_Signet','Biostatic_Ring','Oneiric_Ring','Grasping_Ring','Ring','Dusk_Ring','Gloam_Ring','Penumbra_Ring','Tenebrous_Ring','Breach_Ring','Refined_Breach_Ring','Veridical_Chain','Runemastered_Veridical_Chain','Golden_Obi','Runemastered_Heavy_Belt','Stalking_Belt']
const candidateRows = pendingSlugs.map((slug) => {
  const source = read(`${root}/${slug}.us.json`), korean = read(`${root}/${slug}.kr.json`)
  const reason = /Demigods|Unique/.test(source.variants.map((base) => base.fields.Type).join(' ')) ? 'Demigod/Unique identity or Runic Ward/Alloy variants: excluded from ordinary crafting release scope; rarity-enable flags are not drop provenance.'
    : /Breach/.test(slug) ? 'Sourced implicit maximum-quality bonus needs a dedicated 40/45 base cap and interaction review; current quality boundary assumes an ordinary base cap of 20.'
    : /Grasping|Stalking/.test(slug) ? 'Socketed-item bonus transfer is not modeled; only the explicitly requested Corona Amulet was added as a display-only source fact.'
    : ['Ring','Abyssal_Signet'].includes(slug) ? 'Generic/Unique provenance needs a non-Unique acquisition proof; do not infer craftability from engine rarity flags.'
    : 'Additional exact source candidate found by the audit; ordinary acquisition/explicit-pool release review remains outside the explicitly requested accessory additions.'
  return { slug, source: source.url, sourceSha256: source.sha256, names: source.variants.map((base) => base.name), korean: korean.variants.map((base) => base.name), ids: source.variants.map((base) => base.fields.Type), engineRarityFlags: source.variants.map((base) => base.fields['Mods.enable_rarity']), effects: source.proofs.map((proof) => proof.text), reason }
})
const resolvedAdditionalCandidates = candidateRows.filter(row => registrySlugs.has(row.slug)).map(row => ({ ...row, status: 'SUPPORTED', reason: 'Official GGG item-filter identity and captured class/stat proof; existing reviewed class pool retained.' }))
const pending = candidateRows.filter(row => !registrySlugs.has(row.slug))
const images = read('frontend/src/features/crafting/base-picker/baseImageSources.json')
const missingImages = Object.entries(images).filter(([,image]) => !image.file && !image.image.startsWith('/')).map(([id,source]) => {
  const base = Object.values(bases).find((base) => base.id === id)
  return { id, name: base.name, korean: read('frontend/src/shared/i18n/gameTerms.json').ko[base.slug].name, url: source.image, status: source.cacheBlocked ?? source.status }
})
const report = { baseline: '8524b6b781651379a6cff9108525282a0691df25', imported, excludedFromNewSelectionOnly: ['kinetic','mnemonic','vitalic','two-stone-fire-cold'], rareOnlyUserPolicy: ['dusk-amulet','gloam-amulet','penumbra-amulet','tenebrous-amulet','corona-amulet','sinew-belt','invoking-belt','forking-belt'], inventoryRows, resolvedAdditionalCandidates, additionalPendingCandidates: pending, missingImages }
fs.writeFileSync(`${root}/audit.json`,JSON.stringify(report,null,2)+'\n')
const lines = [
  '# 베이스 감사 및 제작 정책', '',
  '기준: 8524b6b781651379a6cff9108525282a0691df25. 기존 저장 기록·원문·카탈로그 identity를 삭제하지 않았다. 신규 45개 identity는 반지 12종, 목걸이 13종, 갑옷 고정 옵션별 20개이다. 현재 top registry 162개, legacy 포함 179개이며 새 작업대 선택은 171개이다.', '',
  '## 신규 지원 목록', '', '| key | 한국어 | English | 희귀 전용 | P/S (Rare) |', '|---|---|---|---|---|',
  ...imported.map((row) => `| ${row.key} | ${row.korean} | ${row.name} | ${row.rareOnly ? '예' : '아니오'} | ${row.affixLimits.rarePrefixes}/${row.affixLimits.rareSuffixes} |`), '',
  '## 근거와 범위', '',
  '- identity·한영 이름·고정 옵션 stat ID/범위는 PoE2DB가 노출하는 게임 추출 필드와 6개 언어 원문을 대조했다. 각 수집 파일은 원문 페이지 SHA-256, 추출 시각, metadata Type, proof HTML을 포함한다. 공식 운영사 사이트의 드롭 보증이나 최신 실제 게임 확률로 과장하지 않는다.',
  '- 일반 옵션 풀은 기존 검토된 class/능력치 계열의 봉인 스냅샷을 그대로 재사용했다. 신규 가중치·확률을 생성하지 않았다. 각 신규 details.raw.json에 정확한 template/pool/snapshot ID와 기존 원문 해시를 기록했다. 초기 고정 옵션은 기존 작업대 정책대로 검토된 최대값을 사용하며 드롭된 실물 아이템을 관측했다고 주장하지 않는다.',
  '- 갑옷은 기존 능력치 계열 안에서도 고정 옵션 효과가 다르면 별도 상위 대표로 유지한다. 연막의 사슬옷(Shrouded Mail)은 화염/냉기/번개 세 metadata ID를 모두 보존한다. 공통 이름만 주면 모호하게 남기고, 정확한 고정 옵션 원문을 추가로 확인한 경우에만 한 ID로 매핑한다.',
  '- 역학/기억/활력/쌍보석 반지 제외 및 목걸이 5종·허리띠 3종 Rare 전용은 사용자 제작 범위 정책이다. 추출 데이터의 Mods.enable_rarity에는 normal/magic/rare/unique가 함께 있으므로, 이 플래그만으로 일반 드롭 존재나 고유 전용 여부를 단정하지 않는다. 기존 베이스/currency 등록은 남기고 새 선택만 제외했다.',
  '- Rare 전용은 초기 API, 클라이언트 응답/상태 검증, 가져오기 서버 검증, 기록 복원 후 제작 경계에서 적용한다. 과거 Normal/Magic 기록의 저장 바이트를 지우거나 Rare로 자동 변환하지 않는다.',
  '- Lament/Portent/Absent는 원문 후보 중 첫 유효 스킬 하나를 사용자 허용 임시 예시로 표시한다. 6개 언어 표시 앞에 임시 정책 문구를 붙이며, 실제 확정된 roll/관측/확률로 저장하지 않는다. 대표값은 Arctic Armour 11, Wolf Pack 11, Cast on Elemental Ailment 12이다. 해당 스킬 효과의 전투 계산은 지원하지 않는다.',
  '- 목걸이 생성/미리보기에는 사용자 허용 임시 패시브 예시 Heavy Ammunition을 별도 표기한다. 실제 베이스 고정 옵션이나 수치로 추가하지 않으며 계산·실제 패시브 할당·액체 감정 소비를 수행하지 않는다. Lament/Portent/Absent의 원문 스킬 예시와 구분한다.', '',
  '## 전체 목록 대조와 추가 보류', '',
  `기존 전체 장비 roster ${inventoryRows.length}개 페이지의 각 행을 현재 registry와 대조하여 audit.json에 남겼다. 낮은 진행 단계·같은 고정 옵션·Runeforged/Runemastered 변형도 누락시키지 않고 등록 여부를 기록했다. 아래는 추가로 6개 언어의 정확한 이름/ID/옵션을 수집했지만 이번 신규 선택에 넣지 않은 장신구 후보이다.`, '',
  '| 한국어 | English/source slug | 보류 사유 |', '|---|---|---|',
  ...pending.map((row) => `| ${[...new Set(row.korean)].join(', ')} | ${row.slug} | ${row.reason} |`), '',
  '위 추가 후보는 명칭 미확인이 아니라 일반 제작 획득 근거 또는 미지원 socket/Runic Ward/quality-cap 상호작용이 보류된 것이다. 추출 플래그만 근거로 출시/제작 가능으로 채우지 않았다.', '',
  '## 추가 이미지 요청', '',
  '이미지 첨부 채팅: 베이스 선택·세션 History 메뉴. 아래 URL의 HTTP 403 거부는 재시도하지 않았으며 접근성 이름이 있는 대체 표시를 쓴다.', '',
  ...missingImages.map((row) => `- ${row.korean} / ${row.name} (${row.id})`), '',
]
fs.writeFileSync(`${root}/BASE_AUDIT.md`,lines.join('\n'))
console.log(JSON.stringify({ imported: imported.length, auditedPages: inventoryRows.length, additionalPendingCandidates: pending.length, missingImages },null,2))
