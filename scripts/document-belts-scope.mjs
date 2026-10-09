import fs from 'node:fs'
const prefix = '최신 base 확장: [Belt distinct implicit 13종](workbench-belts-bundle-2026-10-05.md). Workbench **91 bases**. 기존78종과 Rawhide film·canonical rolls·source IDs를 보존한다. 신규 ordinary135 + special4, source-backed selection weights, six locales, Divine/Blessed·기존 currency/Essence/Omen 경로를 검증한다. Belt Catalyst와 game quality cap은 지원하지 않는다. 공통 Charm-slot source range와 Breach fixed stat을 보존하며 최종 slot 합산/확률은 미모델링이다. Support/Explorer는 Solar-only, registry220/deferred50는 유지한다. [다음 Crossbow/melee/Shields/Bucklers/Foci/Quivers 및 Staff/Talisman 후보](workbench-next-equipment-plan-2026-10-05.md)를 계속 남긴다. whole-game complete가 아니다. 아래 dated checkpoint는 과거 범위다.\n\n'
for (const path of ['docs/item-state.md','docs/supported-mechanics.md','docs/workbench-simulator.md']) {
  const old = fs.readFileSync(path)
  fs.writeFileSync(path,Buffer.concat([Buffer.from(prefix),old]))
}
console.log('Documented91 base scope while preserving dated documentation bytes')
