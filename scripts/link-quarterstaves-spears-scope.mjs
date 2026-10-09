import fs from 'node:fs'
import assert from 'node:assert/strict'
const message='최신 base 확장: [Quarterstaves/Spears 대표 6종](workbench-quarterstaves-spears-bundle-2026-10-05.md). Workbench **125 bases**. Quarterstaves class58 ordinary158 / Spears class79 ordinary162의 complete source pool, canonical stats·weights·implicit·6locale·클래스별 Essence를 제공한다. Block·conditional·granted skill·source weapon properties는 display-only이다. 기존119 IDs/films, quality overflow/HALF_UP, Shift/Alt/orange, deferred50과 Solar-only Support/Explorer를 유지한다. socket execution은 Stocky에 한정되며 이들 무기는 Artificer/supplied socket을 거부한다. 다음 Staves6/Talismans3와 coverage audit은 bundle에 기록한다. 아래 dated checkpoint는 당시 기록이다.\n\n'
for(const path of ['docs/TECHNICAL_SPEC.md','docs/item-state.md','docs/supported-mechanics.md']) {
 const s=fs.readFileSync(path,'utf8');assert(!s.startsWith(message));fs.writeFileSync(path,message+s)
}
