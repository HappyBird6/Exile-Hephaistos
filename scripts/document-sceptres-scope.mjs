import fs from 'node:fs'
import assert from 'node:assert/strict'
const checkpoint='최신 base 확장: [Sceptre skill-family 7종](workbench-sceptres-bundle-2026-10-05.md), 이전 [Wand 9종](workbench-wands-bundle-2026-10-05.md)을 포함하여 Workbench **78 bases**. Shrine Fire/Ice/Lightning은 distinct source ID로 선택한다. 전체 Sceptre ordinary150+special1, source-backed weights, six locales와 기존71개 film identity를 보존한다. Granted skill은 display-only이며 Support/Explorer는 Solar-only, deferred50은 유지한다. 다음 범위는 Belt distinct implicit/Breach roster와 source-reviewed broader equipment classes다. 아래 dated checkpoint의 이전 수치는 당시 범위다.\n\n'
for(const p of ['docs/TECHNICAL_SPEC.md','docs/item-state.md','docs/supported-mechanics.md']) {
 const source=fs.readFileSync(p,'utf8')
 assert(!source.includes('Sceptre skill-family 7종'),'Preserve prior checkpoint')
 fs.writeFileSync(p,checkpoint+source)
}
