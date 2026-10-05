# Wand skill-family bundle

기준 HEAD: `25131b190fd242f90b51051bc87ae1e65936ae29`. 전용 branch `workbench/top-bases-20261004`, sole writer. 기존 62개에 아래 ordinary skill-family 대표 9개를 더해 **총 71개**를 제공한다. 검증 결과는 [runtime evidence](evidence/wands-runtime-bundle-2026-10-05/validation.json)에 기록했다.

| Base | Metadata suffix | Granted skill | Skill source Id | Character requirement | Ordinary pool |
|---|---|---|---|---|---|
| Bone Wand | FourWand2 | Bone Blast | BoneBlast | Level 2 | 118 |
| Siphoning Wand | FourWand4 | Power Siphon | PowerSiphon | Level 11, Int 23 | 185 |
| Volatile Wand | FourWand5 | Volatile Dead | VolatileDead | Level 16, Int 31 | 123 |
| Galvanic Wand | FourWand6 | Galvanic Field | GalvanicField | Level 25, Int 46 | 123 |
| Acrid Wand | FourWand7 | Decompose | CorpseCloud | Level 33, Int 60 | 185 |
| Offering Wand | FourWand8 | Exsanguinate | Exsanguinate | Level 38, Int 68 | 118 |
| Critical Wand | FourWand11 | Chaos Bolt | Chaosbolt | Level 52, Int 92 | 185 |
| Primordial Wand | FourWand12 | Wither | Wither | Level 56, Int 99 | 118 |
| Dueling Wand | FourWand13 | Spellslinger | Spellslinger | Level 65, Int 114 | 185 |

각 ID의 prefix는 `Metadata/Items/Weapons/OneHandWeapons/Wands/`이다. 원본은 [source bundle](evidence/wands-source-bundle-2026-10-05/applicability.json)에 있다. 공개 English class 일반 185행은 기존 Attuned snapshot과 완전히 일치한다. 각 base의 태그와 상세 modifier의 ordered Spawn Tags 중 첫 일치 weight가 양수인 행을 유지한다. `no_*_spell_mods`가 있는 base는 해당 금지 행을 제외한다. 별도 Perfect Sorcery/Alacrity suffix 2개를 포함한다. 모든 유지된 definition은 기존 Attuned ID·family·range·tier·craft tag·required item level·published weight를 보존한다. 소스 DropChance가 있는 pool은 `POE2DB_AS_PUBLISHED`이며 없는 numeric roll distribution은 기존 UNVERIFIED source-unit uniform 가정을 유지한다.

Grants Skill은 affix가 아닌 표시용 source property이다. 숫자 implicit를 만들지 않는다. skill level scaling·damage·reservation·trigger·combat 계산을 추가하지 않는다. Decompose source 페이지에는 Corpsewade 전용 `CorpsewadeCorpseCloud`도 있으며 이를 Acrid skill로 선택하지 않는다. source HTML과 skill 페이지의 전체 fields를 별도로 보존한다.

Six-locale 이름·skill·요구치를 수집했다. Spanish Bone의 Int7 및 Offering의 Int69는 current English card와 불일치한다. 원본을 evidence에 유지하고 표시에는 English canonical 값(각각 Int 요구 없음, Int68)과 Spanish source vocabulary를 사용한다. 기존 번역을 변경하지 않는다.

Twisted Wand(`FourWandUnique1`, Coiling Bolts), Runic Fork(`FourWandUnique2`), Runemastered variants는 이번 ordinary 후보에 포함하지 않는다. `Mods.enable_rarity`의 normal/magic/rare 값만으로 실제 ordinary 획득 가능성을 확정할 수 없으며 unique/runeforged provenance의 추가 증거가 필요하다. 이름의 Unique suffix만으로 availability를 판정하지 않는다. Withered/Frigid/Torture는 Chaos Bolt의 낮은 요구치 대안으로 이번 최고 대표 후보가 아니다. 기존 Attuned film identity는 유지한다.

남은 정확한 Sceptre 7종은 Stoic(Discipline), Omen(Malice), Shrine `FourSceptre6a/6b/6c`(Purity of Fire/Ice/Lightning), Clasped(Heart of Ice), Wrath(Fulmination)이다. Shrine `FourSceptreUnique1` Impurity의 ordinary availability는 별도 확인한다. Belt는 Rawhide 외 Linen/Wide/Long/Plate/Ornate/Mail/Double/Heavy/Utility/Fine의 distinct implicit 및 Invoking/Sinew/Forking의 Breach identity를 검증할 다음 묶음이다. Golden Obi의 Demigod provenance, Stalking socket transfer, Runemastered Heavy4는 별도 source/mechanic 검토가 필요하다. 이후 Crossbow·melee·Shields/Foci/Quivers는 각각 complete class pool·requirements·properties·implicit/skill·socket constraints를 확인한 후 확장한다. all-low-tier 확장은 하지 않는다.

QA는 새 `codex/wands-qa-20261005` copy, Compose `exile-wands-20261005`, localhost 20180/20181을 사용한다. heavy checks는 sequential이다. live18080/18081·DB volumes·원본 repo·사용자 browser storage는 보존한다. 이전 실패 로그와 evidence는 덮어쓰지 않는다. local commit만 허용하며 push/merge/deploy는 하지 않는다.

Backend `check generateJooq bootJar`는 unit/architecture441·integration6, Frontend 필수 aggregate/build는1820 tests, API539·browser719 assertions가 모두 통과했다. 검증 copy와 실제 source556 files가 일치한다. 6locale×2viewport×9base의 카드108장·전체 화면108장 및 orange/Solar regression12장, 총228장을38개 contact sheet로 pixels 검사했다. 기존62 initial 응답과 기존 base/Essence 데이터·모든 이전 번역·registry220 entries·deferred50 보존을 별도 비교했다. Support/Explorer Solar-only, Shift/Alt·local orange·Solar quality40·HALF_UP·old films도 확인했다.

실패 이력은 삭제하지 않았다. 첫 Backend 실행은 새 테스트의 잘못된 accessor를 수정하여 해결했다. 첫 Frontend 실행은 이전 QA script의 Node22가 현재 engine 요구사항과 맞지 않아 설치 전에 거부되었다. Node24로 필수 검증을 통과했으며 engine 조건을 완화하지 않았다. 성공 로그는 QA root의 `backend-check-2.log`, `frontend-check-2.log`이다. Source Spanish 수치 불일치는 Bone/Offering2건이며 원본 증거를 보존했다. Twisted/Runic Fork/Runemastered ordinary availability는 아직 별도 확인 대상이다.

Infinite Essence는 class source에 Strength/Dexterity/Intelligence targets가 실제로 존재한다. 이번 묶음은 기존 Attuned의 Sorcery/Seeking/Alacrity 및 Perfect Sorcery/Alacrity 범위를 그대로 확장한다. Infinite의 특수 target 지원을 추가하지 않았다는 의미이며, source가 없다고 판단한 것이 아니다.

추가 [filled legacy film 검증](evidence/wands-runtime-bundle-2026-10-05/old-filled-results.json)7개도 통과했다. Attuned/Rattling/Hallowed의 compound 값·fracture·snapshot을 가진 저장 film을 정확하게 복원하고 Alt 후에도 값을 유지했다. 자체 QA4 services를 정상 종료하고 exclusive slot을 해제했다. [QA 종료 증거](evidence/wands-runtime-bundle-2026-10-05/qa-release.json)의 live4 ID·시작시간·mount는 검증 전후 동일하다. 기존 volume 삭제·remote 작업은 없다. 자체 리뷰에서 runtime/source556 files 일치·scope·pool filtering·old translations/registry 보존을 확인했다.

구현 local commit은 `3d0620e6ecd75c9d4fa38a470b31a3ac362cc71a`이다. 전체 `git diff --check`는 변경하지 않고 보존한 공개 raw HTML의 trailing whitespace를 보고한다. 필수 Backend/Frontend formatter 검증은 모두 통과했다. 원본 HTML 증거를 보정하거나 whitespace 검사 설정을 완화하지 않았다.
