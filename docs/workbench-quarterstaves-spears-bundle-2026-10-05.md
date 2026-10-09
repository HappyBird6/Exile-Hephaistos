# Quarterstaves와 Spears 대표 최상위 6종

기준 `2fe3f86d101ef8f3f35636470a9e4789ae7c7edf`, branch `workbench/top-bases-20261004`. 기존 119 bases에 Quarterstaves3와 Spears3을 추가하여 **125 bases**를 제공한다.

| Base | Metadata suffix | 요구 level | 요구 능력치 | implicit |
|---|---|---:|---|---|
| Aegis Quarterstaff | FourQuarterstaff8Endgame | 79 | Dex127, Int50 | +(12–18)% to Block chance |
| Bolting Quarterstaff | FourQuarterstaff4Endgame | 78 | Dex127, Int50 | hidden added Lightning Damage 1–100 |
| Dreaming Quarterstaff | FourQuarterstaff10Endgame | 78 | Dex127, Int50 | 없음; source Critical Hit Chance 0% |
| Grand Spear | FourSpear8Endgame | 79 | Str68, Dex109 | Spear Throw 제공; Melee Strike Range25% |
| Flying Spear | FourSpear5Endgame | 78 | Str50, Dex127 | Spear Throw 제공; Projectile Speed(25–35)% |
| Akoyan Spear | FourSpear10Endgame | 78 | Str50, Dex127, Int90 | Spear Throw 제공 |

[Quarterstaves](https://poe2db.tw/us/Quarterstaves) class58 / [Spears](https://poe2db.tw/us/Spears) class79의 ordinary 목록, 각 NormalPopup, `normal, magic, rare, unique` rarity flags, Endgame Metadata ID를 [6locale source bundle](evidence/quarterstaves-spears-source-bundle-2026-10-05/)에 보존한다. 이는 현 source catalog에서 ordinary 사용 가능한 근거이며 exact drop 위치·확률의 검증은 아니다. 요구 능력치는 popup 값이다. 하위 공통 metadata의 AttributeRequirements 기본값으로 이를 덮어쓰지 않는다. Equip level과 generation itemLevel은 별개이므로 ilvl1 초기 상태도 허용한다.

Quarterstaves ordinary158 / Spears ordinary162의 **전체 행**을 canonical stats, Local/Global locality, requiredItemLevel, family, craft tags, published DropChance weight, ordered spawn tags로 검증한다. 기존 definition과 layer/affix/level/family/stats/text가 모두 같은 경우에만 ID를 재사용한다. 클래스 pool은 독립하며 Bow/Mace pool alias가 아니다. Fixed Essence24와 replacement7은 클래스 source의 exact 대상이다. Perfect Battle은 Quarterstaff +3 / Spear +2이다. Abyss는 기존 full Mark definition을 유지한다. 제외된 Alloy와 deferred50은 확장하지 않는다. Published ordinary weight를 사용하며 미검증 special 선택 weight에만 명시된1/N simulator 가정을 적용한다. 알 수 없는 eligibility를 생성하지 않는다.

Aegis와 Flying의 variable implicit은 Omen of the Blessed + Divine reroll 대상이며 fixed/no implicit에는 거부된다. Spear Throw 제공 canonical stat과 source의 6locale granted skill 문구를 보존한다. Bolting hidden stat도 삭제하지 않는다. Block·조건·skill·무기 속성은 표시용이며 combat simulation을 제공하지 않는다. Source weapon properties에는 계산하지 않은 원본이라는 안내를 표시한다.

Quality source cap20을 적용한다. Catalyst는 기존 Ring/Amulet 적용 범위를 유지한다. ordinary socket maximum은 Quarterstaff2 / Spear1이지만 실행 가능한 socket 모델은 Stocky Mitts에 한정된다. 이들 무기의 Artificer 및 supplied socket state는 명시적으로 거부한다. 일반 무기 socket execution을 지원한다고 주장하지 않는다.

Solar-only Support/Explorer, 기존119 IDs와 films, canonical rolls, quality overflow/HALF_UP, Shift/Alt, orange preview와 6locale를 유지한다. 격리 QA project는 `exile-quarterstaves-spears-20261005`, output은 `codex/quarterstaves-spears-qa-20261005`, localhost20880/20881이다. live18080/18081, 원본 repo, DB volumes, 사용자 browser storage와 unrelated 서비스는 보존한다. 기존 evidence를 덮어쓰지 않는다. local commit만 수행하며 remote push/merge/deploy는 하지 않는다.

## 다음 범위와 coverage audit

다음 대상은 Staves6(Permafrost, Reflecting, Dark, Ravenous, Perching, Sanctified) / Talismans3(Maji, Fungal, Jade)이다. ordinary availability, distinct 미지원 variant, current class ID, complete pool, innate skill/shapeshift 의미를 별도로 검증한 뒤 추가한다. 전체 coverage audit은 기존 source class 목록과 지원 IDs를 대조하여 ordinary 최고 tier·distinct implicit/skill sidegrade·unique-only·미지원 범위를 구분한다. Quarterstaff/Spear의 다른 ordinary sidegrades도 이 audit에서 기록한다. deferred50과 제외 mechanics는 그대로 유지한다.

## 검증

Backend `check generateJooq bootJar` 통과: unit488 / Docker integration6, 실패·오류·skip0. Frontend `npm ci`, lint, typecheck, format:check, test, build 통과: 68files / 1915tests. API2902, browser1088, filled legacy films25 검사가 통과했으며 page errors0이다. 6locale·1440/390 화면192장과 contact sheets32장을 보존했고 전체 contact sheets와 Flying Spear mobile6장, English/Korean orange·Solar 원본을 직접 검토했다.

[완료 증거](evidence/quarterstaves-spears-source-bundle-2026-10-05/completion.json), [source audit](evidence/quarterstaves-spears-source-bundle-2026-10-05/source-audit.json), [기존119 보존](evidence/quarterstaves-spears-source-bundle-2026-10-05/preservation-final.json), [시각 검토](evidence/quarterstaves-spears-source-bundle-2026-10-05/visual-review.json)에 결과와 SHA를 기록했다. 전용 QA 서비스4개만 정상 종료하고 슬롯을 해제했다. 기존 live 서비스의 ID·시작 시각·mount가 그대로임을 확인했다. remote push/merge/deploy는 수행하지 않았다.
