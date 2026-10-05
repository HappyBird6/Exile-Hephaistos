import fs from 'node:fs'
import assert from 'node:assert/strict'
for(const path of ['docs/TECHNICAL_SPEC.md','docs/supported-mechanics.md','docs/item-state.md']) {
  const text=fs.readFileSync(path,'utf8')
  assert(!text.includes('workbench-maces-bundle-2026-10-05.md'))
  const prefix='최신 base 확장: [Mace 최상위 대표 6종](workbench-maces-bundle-2026-10-05.md). Workbench **119 bases**. One Hand Maces class12 / Two Hand Maces class17의 독립 complete ordinary150, source weights·canonical Local/Global stats·implicit·6locale와 클래스별 Essence를 제공한다. 기본 weapon properties는 source 표시이며 combat은 계산하지 않는다. Quality cap20, Catalyst/socket restrictions, 기존113 IDs/films·deferred50·Solar-only Support/Explorer·overflow/HALF_UP·Shift/Alt/orange preview를 보존한다. 미선택 variants와 다음 Quarterstaves3/Spears3/Staves6/Talismans3 계획은 bundle에 기록한다. 아래 dated checkpoint는 역사 기록이다.\n\n'
  fs.writeFileSync(path,prefix+text)
}
