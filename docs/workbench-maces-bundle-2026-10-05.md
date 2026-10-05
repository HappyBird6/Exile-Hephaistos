# Mace 대표 최상위 6종

기준 `3c40950098146be6b2b80fda3ec54c380f833927`, branch `workbench/top-bases-20261004`. 기존 113 bases에 6종을 추가하여 **119 bases**를 제공한다. One Hand Maces는 class12, Two Hand Maces는 class17이며 각각 독립 source pool을 사용한다.

| Base | Metadata suffix | 요구 level | 기본 implicit |
|---|---|---:|---|
| Fortified Hammer | FourOneHandMace8Endgame | 79 | 40% chance to Daze on Hit |
| Strife Pick | FourOneHandMace5Endgame | 78 | +(5–10)% to Critical Damage Bonus |
| Akoyan Club | FourOneHandMace10Endgame | 78 | Always Hits |
| Ruination Maul | FourTwoHandMace8Endgame | 79 | Causes Enemies to Explode on Critical kill, for 10% of their Life as Physical Damage |
| Fanatic Greathammer | FourTwoHandMace5Endgame | 78 | Strikes deal Splash Damage |
| Tawhoan Greatclub | FourTwoHandMace10Endgame | 78 | Warcries Empower 1 additional Attacks |

모두 요구 Strength163이며 equip requirement와 generation itemLevel1은 별개이다. 선택은 source class 목록의 일반 Endgame 단계 중 level78/79 대표와 서로 다른 implicit에 근거한다. 모든 낮은 tier를 포함한다는 의미가 아니다. One Hand의 Molten Hammer, Marauding Mace, Crown Mace, Flanged Mace와 Two Hand의 Ironwood Greathammer, Massive Greathammer, Sacred Maul, Anvil Maul, Aberrant Sledge는 이번에 선택하지 않았다. Runeforged/Runemastered와 unique-only 변형은 추가하지 않는다.

[One Hand Maces](https://poe2db.tw/us/One_Hand_Maces), [Two Hand Maces](https://poe2db.tw/us/Two_Hand_Maces)의 ordinary 목록과 각 NormalPopup, `normal, magic, rare, unique` rarity flags 및 Endgame Metadata ID를 [6locale 원본 bundle](evidence/maces-source-bundle-2026-10-05/)에 보존한다. 이는 현재 공개 catalog의 ordinary 상태 근거이며 exact drop 위치·확률까지 검증한 것은 아니다.

## Pool과 표시

각 클래스 ordinary150행 전체에 canonical stats, tier, requiredItemLevel, craft tags, family, 공개 DropChance weight 및 ordered spawn을 보존한다. 기존 definition은 layer/affix/level/family/stats/text가 모두 일치할 때만 재사용한다. One Hand와 Two Hand의 Physical/Elemental Damage 범위와 Attack Skill Level은 해당 source를 따르며 Bow pool alias를 사용하지 않는다. Fixed Essence24종과 replacement7종은 클래스별 exact target이다. Perfect Battle은 One Hand +2, Two Hand +3이다. Abyss는 기존 full Mark definition을 재사용한다. 제외된 Alloy 및 deferred50의 scope는 확장하지 않는다. 일반 후보는 공개 weight를 사용하며 미공개 special 선택에 한해서만 기존 disclosed1/N 모델을 사용한다.

Local weapon Attack Speed/Physical Damage/Accuracy/Critical stats와 Global 조건부 효과를 canonical stat으로 유지한다. 기본 weapon properties는 원본 값임을 UI의 기존 source-property 설명으로 표시하고 전투·명중·Splash·Daze·Warcry·Explosion을 계산하지 않는다. Akoyan `local_always_hit`와 Fanatic `melee_splash`는 source의 고정 `Unscalable Value`1을 보존한다. Strife만 variable implicit이며 Blessed+Divine은 해당 base에서만 가능하다. 나머지 고정 implicit에서 Blessed는 거부된다.

Quality source cap20을 제공한다. typed Catalyst는 Ring/Amulet 전용 정책에 따라 거부된다. ordinary socket maximum은 One Hand1/Two Hand2를 원본대로 기록하지만 현재 socket 실행 구현은 Stocky Mitts만 지원하므로 Mace Artificer 및 supplied socket 상태는 원자적으로 거부한다. socketed Augment 효과를 일반 weapon 속성으로 추정하지 않는다.

## 보존과 QA

Solar-only Support/Explorer, 기존 113 IDs/films, quality overflow/HALF_UP, Shift/Alt, orange preview 및 6locale를 보존한다. 격리 QA project는 `exile-maces-20261005`, output은 `codex/maces-qa-20261005`, localhost20780/20781이다. live18080/18081, original repo, DB volumes, browser storage 및 unrelated 서비스는 유지한다. 기존 evidence를 덮어쓰지 않는다. 최종 검사 결과는 아래에 기록했다. local branch commit만 허용하며 push/merge/deploy는 하지 않는다.

### 최종 검증 결과

- Backend `spotlessApply check generateJooq bootJar`: unit482 + Docker integration6, failure/error/skip0, BUILD SUCCESSFUL.
- Frontend `npm ci`, lint, typecheck, format:check, test, build: 67 files / 1909 tests 통과.
- Runtime API2773, browser1058, 기존 filled film25 checks 통과. page error0. 새 클래스의 positive/negative path, low itemLevel과 equip level 분리, implicit/range 및 기존113 초기 film을 검증했다.
- 6locale desktop1440/mobile390 screenshot192개와 contact sheet32개를 픽셀 검토했다. orange/Solar original도 직접 확인했다. source weapon properties는 `not computed` 번역 표기를 적용한 최종 build로 재검증했다.
- 기존113 catalog/Essence/name/template/registry 항목과 deferred50 보존 비교를 통과했다. raw48 SHA 및 최종 QA 입력733개·runtime build SHA를 기록했다.

[완료 증거](evidence/maces-source-bundle-2026-10-05/completion.json), [보존 비교](evidence/maces-source-bundle-2026-10-05/preservation-completion.json), [source audit](evidence/maces-source-bundle-2026-10-05/source-audit.json), [pixel review](evidence/maces-source-bundle-2026-10-05/visual-review.json)에 결과와 QA 원본 위치를 남겼다. 첫 typecheck의 누락 union, importer metadata counter, formatter 비교 및 첫 screenshot의 source-property 표시는 범위 내에서 수정했으며 이전 log/output은 보존했다. 원인과 수정은 `scripts/maces-source-provenance.json`에 기록했다. assertion을 완화하거나 실패를 skip하지 않았다.

전용 Compose 서비스4개를 정상 `stop`했고 QA slot을 반환했다. live18080/18081 서비스의 ID·시작 시각·mount는 작업 전후 동일하다. volume 삭제·remote push·merge·deploy는 수행하지 않았다.

## 다음 범위

다음 순서는 Quarterstaves3(Aegis, Bolting, Dreaming), Spears3(Grand, Flying, Akoyan), Staves6(Permafrost, Reflecting, Dark, Ravenous, Perching, Sanctified), Talismans3(Maji, Fungal, Jade)이다. 각 단계에서 ordinary availability와 distinct 미선택 variant, class ID, complete pool 및 innate skill/shapeshift 의미를 검증한 뒤 추가한다. 기존 제외 항목과 deferred50은 유지한다.
