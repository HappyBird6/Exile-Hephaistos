# Distinct implicit Ring 8종 — 2026-10-04

기준 HEAD `c030c6862eeed2b9ea82f2663044eab8feb7d006`, branch `workbench/top-bases-20261004`에서 기존 46개 base에 Ring 8종을 추가한다. Workbench는 **54개 base**가 된다. Iron Ring의 ID·snapshot·modifier·저장 film은 유지하며 Support/Explorer는 Solar-only, registry active170/deferred50도 유지한다. Ring은 단일 선형 tier 목록이 아니며 서로 다른 유용한 implicit category를 선택한 범위다.

## 정확한 identity와 implicit

| Base | Type suffix | Character level | Source implicit | Family |
|---|---|---:|---|---|
| [Kinetic Ring](https://poe2db.tw/us/Kinetic_Ring) | FourRingB4 | 40 | Adds (6–9) to (11–15) Physical Damage to Attacks | PhysicalDamage |
| [Vitalic Ring](https://poe2db.tw/us/Vitalic_Ring) | FourRingB2 | 40 | (4–6)% increased maximum Life | IncreasedLife |
| [Mnemonic Ring](https://poe2db.tw/us/Mnemonic_Ring) | FourRingB3 | 40 | (4–6)% increased maximum Mana | MaximumManaIncreasePercent |
| [Pearl Ring](https://poe2db.tw/us/Pearl_Ring) | FourRing8 | 32 | (7–10)% increased Cast Speed | IncreasedCastSpeed |
| [Amethyst Ring](https://poe2db.tw/us/Amethyst_Ring) | FourRing6 | 20 | +(7–13)% to Chaos Resistance | ChaosResistance |
| [Prismatic Ring](https://poe2db.tw/us/Prismatic_Ring) | FourRing9 | 35 | +(7–10)% to all Elemental Resistances | AllResistances |
| [Ruby Ring](https://poe2db.tw/us/Ruby_Ring) | FourRing3 | 8 | +(20–30)% to Fire Resistance | FireResistance |
| [Two-Stone Ring (Fire/Cold)](https://poe2db.tw/us/Two-Stone_Ring) | FourRing13a | 35 | +(12–16)% to Fire and Cold Resistances | FireAndColdResistance |

Type prefix는 `Metadata/Items/Rings/`다. Class `Rings`, source tag `ring`, attribute requirement 없음, Global stat locality를 원문대로 보존한다. Two-Stone 공유 페이지에서 첫 Fire/Cold section의 Type를 직접 검증하며 Fire/Lightning `FourRing13b`, Cold/Lightning `FourRing13c`를 alias하지 않는다. Kinetic의 source stat 순서는 maximum/minimum이지만 표시 순서는 minimum/maximum이며 각 range와 shared roll을 보존한다. 초기 implicit은 각 source maximum을 사용하며 Divine/Blessed는 원래 variable range 안에서 동작한다. 요구 character level과 crafting item level은 서로 다르다.

## 전체 source pool과 기존 mechanics

[Source bundle](evidence/rings-source-bundle-2026-10-04/import-summary.json)은 base 48개 locale snapshot, implicit proof 8개, class pool 6개를 포함한다. ordinary 203행은 기존 Iron Ring raw source와 전체 일치한다. 각 base의 ordered Spawn Tags에서 Ring eligibility를 확인하고 게시 class DropChance·family·tier·range·tags·source URL을 별도로 보존한다. detail의 Spawn Tags 값과 class의 게시 weight는 서로 다른 source domain이므로 동일한 숫자라고 가정하지 않는다. 게시 weight는 기존 simulator policy이며 실제 게임 확률을 새로 검증했다는 의미가 아니다.

각 catalog는 ordinary203 + source special5 + 고유 implicit1을 가진다. 일반 Essence23 action은 class source의 정확한 Code/Level/Family와 ordinary target을 연결한다. Perfect Mind1, Hysteria1, Breach1, Abyss prefix/suffix2는 같은 Code의 검증된 detail을 재사용하며 ordinary spawn은 0이다. Abyss의 class table 축약 Mark는 기존 full detail을 유지한다. Desecration/reveal을 추가하지 않는다. 일반 currency·상위 currency·Fracturing·Alchemy 및 source-valid Omen을 유지한다. 잘못된 class의 Essence, Horror, Artificer, refined Catalyst, Alloy 및 deferred mechanics는 입력을 보존하며 거절한다.

일반 Catalyst13은 신규 Ring 모두에서 기존 typed-quality engine을 사용한다. Kinetic은 Physical/Attack, Vitalic은 Life, Mnemonic은 Mana, Pearl은 Caster/Speed, 각 Resistance는 source Craft Tags에 따라 matching하며 여러 matching tag가 있어도 한 번만 scaling한다. 원래 roll·source range·weight·saved film은 scaling하지 않는다. 기존 integer rounding과 미검증 numeric semantics 표시를 유지한다. 기본 maximum20, source Breach의 정확한 +20 maximum quality prefix가 있으면40이다. cap 하락 뒤 기존 quality40은 보존하며 catalyst 재사용·type 전환은 기존 `max(existing,currentCap)` policy를 따른다. 40을 넘는 quality 입력은 거절한다. Solar의 class를 복제하지 않고 정확한 신규 Ring identity만 허용한다.

[6locale provenance](evidence/rings-source-bundle-2026-10-04/localization-provenance.json): en/ko/ja/zh-CN/zh-TW/es name·implicit는 해당 base source, ordinary template203×6은 새 class source와 정확히 일치한다. 기존 affix template ID는 보존하며 신규 binding은 고유 implicit8 + Ring Hysteria1이다. 신규 name/implicit의 언어 fallback은 없다. Abyss table/full-detail 표현 차이는 명시한 source 예외다.

## 격리 검증과 실패 이력

QA copy `codex/rings-qa-20261004`, Compose project `exile-rings-20261004`, localhost API/UI19880/19881을 사용한다. Heavy checks는 순차 실행하며 사용자 browser/localStorage, live UI18081(`18a2a3d`)/API18080(`7d27d6c`), 기존 DB volume·원본 repository·다른 services·기존 QA output을 보존한다. 이전 output overwrite 거절은 계속 유효하다. 새 attempt의 로그와 이전 실패 XML을 분리 보존한다. [실패 이력](evidence/rings-runtime-bundle-2026-10-04/failure-history.json)을 참조한다. Remote push/merge/deploy는 수행하지 않는다.

## 남은 범위

현재 roster는 Body7, Helmet7, Gloves7, Boots6, Bow6, Basic/Time-Lost Jewel8, Amulet1, Ring9, Belt1, Wand1, Sceptre1이다. Ring 전체 catalog를 완료했다는 의미가 아니다. Sapphire/Topaz, 나머지 Two-Stone 두 변형, Lazuli/Golden Hoop/Emerald/Gold/Unset, Biostatic/Oneiric, Breach/Refined Breach, Grasping/socket-transfer, Dusk/Gloam/Penumbra/Tenebrous의 capacity 변형 등은 별도의 source 검증·mechanic 경계 검토가 필요하다.

다음 확장은 Amulet7(Stellar, Amber, Bloodstone, Lunar, Azure, Crimson, Pearlescent), Wand/Sceptre의 distinct skill family, Belt의 distinct implicit category 순이다. Jade/Lapis와 다른 Amulet sidegrade도 전체 완료로 계산하지 않는다. Wand의 Runic Fork, Sceptre의 Purity/Impurity variant, Belt의 Charm Slot1–3와 Stalking/socket-transfer·Runic Ward는 source ID와 실제 mechanic 의미를 먼저 검증한다. 그 밖의 weapon/offhand/equipment class는 아직 전체 source pool과 대표 base를 runtime으로 검증하지 않은 broader gap이다. 다음 parent chat은 fresh work context에서 기존 bundle의 검증·commit을 확인하고 이어간다.

## 최종 검증·자체 리뷰·종료

Backend `check generateJooq bootJar` 437 tests, Frontend lint/typecheck/format/unit/build 1802 tests가 통과했다. API1815, browser1231, importer21 checks가 통과했으며 실패·skip은 없다. 6locale×desktop/mobile×8base 96개 원본 화면, 96개 card crop 및 old46 film·orange8·overflow8 화면과 contact35를 합쳐 PNG289개를 보존하고 contact35 전체를 직접 검토했다. 잘림·가로 overflow를 발견하지 않았다. 최종 runtime source 498개는 검사 copy와 byte-equal이다. [검증 요약](evidence/rings-runtime-bundle-2026-10-04/validation.json), [화면 manifest](evidence/rings-runtime-bundle-2026-10-04/screenshot-manifest.json), [종료 확인](evidence/rings-runtime-bundle-2026-10-04/qa-release.json)에 근거를 보존한다. 자체 리뷰에서 차단 결함은 없었다.

전용 Ring QA4 서비스를 `compose down`으로 정상 종료하고 heavy QA slot을 해제했다. volume 삭제는 하지 않았으며 기존 live4 container ID·포트·실행 상태는 유지됐다. 배포·master 반영·remote push는 수행하지 않았다. 초기 실패 로그와 fixture 수정 이유는 별도 failure-history에 남겼다. 세션 이름 변경 도구가 제공되지 않아 Worker 세션 이름은 자동 설정하지 못했다.
