# Crossbow highest-tier 대표 6종

기준 `9fbb83da948d515756b6cea9ffb7a24155abba8e`, branch `workbench/top-bases-20261004`. 기존 Workbench 91종에 Crossbow 6종을 추가하여 **97 bases**를 제공한다. 이 묶음은 요청된 highest-tier/sidegrade 대표 범위다. 전체 Crossbow base나 모든 장비 class의 완료를 뜻하지 않는다.

| Base | Character level | Str / Dex | Physical damage | Crit | APS | Reload time | Built-in implicit |
|---|---:|---:|---|---:|---:|---:|---|
| Siege Crossbow | 79 | 89 / 89 | 29–115 | 5% | 1.65 | 0.75 | Grenade projectile +1 |
| Gemini Crossbow | 78 | 89 / 89 | 28–112 | 5% | 1.6 | 1.1 | Additional bolt +1 |
| Elegant Crossbow | 78 | 89 / 89 | 31–123 | 5% | 1.65 | 0.85 | Pierce chance 20–30% |
| Flexed Crossbow | 77 | 89 / 89 | 32–127 | 5% | 1.6 | 0.85 | Bolt Speed 20–30% |
| Desolate Crossbow | 77 | 89 / 89 | 33–132 | 5% | 1.6 | 0.8 | 없음 |
| Engraved Crossbow | 72 | 82 / 82 | 31–124 | 5% | 1.6 | 0.8 | 없음 |

[Source bundle](evidence/crossbows-source-bundle-2026-10-05/verification.json)은 6locale의 원문 HTML, URL·SHA·조회 시각, 정확한 metadata ID·class·tags·popup properties·requirements·canonical implicit stats·locality를 보존한다. Crossbow는 `crossbows` class이며 Bow class/pool alias가 아니다. Elegant의 추가 `karui_basetype`까지 실제 base별 ordered Spawn Tags를 검증한다. raw attribute component의 generic 값과 장비 popup 요구사항은 서로 다르므로 장착 requirements는 popup 값을 사용한다. 원문을 수정하거나 값이 같다고 가정하지 않는다.

[GGG Dawn of the Hunt item-filter announcement](https://www.pathofexile.com/forum/view-thread/3740346)은 Expert Tense/Varnished/Dyad/Bombard/Forlorn의 현재 Flexed/Engraved/Gemini/Siege/Desolate 이름을 명시한다. [Elegant 실제 소유 아이템 게시](https://www.reddit.com/r/PathOfExile2/comments/1u1efqt/which_crossbow_is_better/)는 일반 제작 아이템의 실제 base 사용을 보조 검증한다. class listing/normal flag만으로 출시 여부를 결론내리지 않는다. source DropLevel은 드롭률·특정 지역의 드롭 보장이 아니다.

전체 ordinary **146종**의 source selection weight를 보존한다. 84종은 기존 stat·family·affix·level·text와 정확히 일치하여 definition ID와 6locale binding을 재사용한다. 나머지 62종도 canonical stat을 모두 보존한다. Bow와 다른 TwoHand flat damage, Projectile Skill level, AdditionalAmmo +1/+2를 빠뜨리지 않는다. 현재 source ordinary pool에는 Reload Speed explicit이 없다. Reload Time은 base property로 보존하며 없는 modifier를 추정 추가하지 않는다. 공개 modifier code가 확인된 경우 보존하며 code가 없는 항목은 snapshot 내부 ID를 사용한다. hash를 game modifier code로 표현하지 않는다. numeric roll은 기존 UNVERIFIED source-unit integer model이며 combat 확률이 아니다.

고정 Essence action **24개**, replacement action **7개**, weight0 special definition **8종**을 연결한다. Infinite는 source-valid Strength/Dexterity 두 후보이며 Intelligence는 ordered spawn에서 거절된다. Greater Battle의 비무기 IncreasedAccuracy row도 제외하고 검증된 local weapon target만 사용한다. Perfect Essence의 default0 spawn은 ordinary 생성 금지이며 적용 target은 Crossbow class의 Essence table에서 검증한다. special을 ordinary pool에 넣지 않는다. 기존 Crystallisation·Coronation·Alchemy·Exaltation·Annulment·Whittling 등 Omen 경로를 사용한다. variable implicit에만 Blessed를 허용하며 fixed/absent implicit은 거절한다. 제외된 Hysteria/Insanity/Horror/Alloy mechanics를 새 class에 추정 연결하지 않는다. 선택 weight가 없는 fixed-choice Infinite·Abyss 경로는 검증된 후보에 한해서 기존 공개 1/N simulator model을 사용한다.

추가 bolt의 canonical `base_number_of_crossbow_bolts=1`과 grenade의 canonical `grenade_skill_number_of_additional_projectiles=1`을 유지한다. 숫자 대신 단어로 표현되는 locale의 fixed source text는 그대로 보존한다. Reload Time·Physical Damage·Crit·APS는 **unmodified base display**로 명시하며 modifier 적용 후 전투 성능·탄창·reload DPS·grenade 발사·Ballista 개수 등을 계산하지 않는다. 품질 최대20은 source `Quality.max_quality`와 일치한다. Catalyst typed quality는 미지원이며 거절한다. `Sockets.socket_info`의 ordinary maximum2는 metadata에 보존하지만 현재 상태 모델의 socket 지원은 기존 Stocky0/1뿐이다. Crossbow Artificer·supplied socket count·socketed Augment 및 exceptional socket/quality transfer를 추가하지 않는다. old null/absent socket 상태는 그대로 보존한다.

기존 91종 ID·catalog·canonical rolls·6locale terms/templates·film format을 보존한다. registry220/deferred50, Support/Explorer Solar-only, quality overflow/HALF_UP·Shift/Alt·orange preview·new UI/i18n 경계를 유지한다. [Preservation evidence](evidence/crossbows-source-bundle-2026-10-05/preservation-final.json)는 변경 전후 old manifest·definitions·terms·templates·registry와 6종 실제 tags에 대한 전체 ordinary eligibility를 비교한다. raw HTML whitespace는 byte fidelity를 위해 남길 수 있으며 authored Java/Frontend formatter는 통과해야 한다.

QA는 새 `codex/crossbows-qa-20261005`, project `exile-crossbows-20261005`, localhost20480/20481만 사용한다. heavy checks는 exclusive sequential이다. live18080/master7d27d6c, live18081/UI18a2a3d, DB volumes·사용자 browser storage·기존 QA output·원래 repo·다른 services는 보존한다. local commit만 허용하며 push/merge/deploy는 수행하지 않는다.

## 최종 검증과 자체 리뷰

- Backend 최종 `check generateJooq bootJar`: unit460, Docker integration6, formatter/ArchUnit 통과. codegen은 migration을 적용한 임시 DB에서 실행했다. `backend-check-3.log`에 최종 registry version을 포함한 성공 결과를 남겼다.
- Frontend `npm ci`, lint, typecheck, format:check, test, build: 63 files / 1865 tests 통과. 2675 catalog definitions, compound432의 6locale coverage를 확인했다. Vite의 기존 큰 bundle 경고는 남으며 code splitting을 범위 밖에서 추가하지 않았다.
- [API785 checks](evidence/crossbows-source-bundle-2026-10-05/api-results.json): 기존91 initial 전체 비교, 신규6 전체 definitions/source weights, ordinary/higher currency·fixed24·replacement7·Omens, wrong-class/deferred atomic refusal, low-ilvl/source minimum, Blessed·implicit·quality/socket 및 Solar-only Support/Explorer를 검증했다.
- [Browser780 checks](evidence/crossbows-source-bundle-2026-10-05/browser-results.json): 신규6 positive/negative class 경로, Shift/Alt/orange, 기존91 films의 exact state 복원, Solar overflow40/cap20/HALF_UP를 확인했다. page errors는0이다. [Legacy filled film7 checks](evidence/crossbows-source-bundle-2026-10-05/old-filled-results.json)로 Wand/Sceptre/Hallowed compound·fracture·snapshot을 추가 검증했다.
- 6 bases × 6 locales × desktop1440/mobile390의 카드72와 전체 화면72, orange6·Solar overflow6를 캡처했다. 카드12/full14 contact sheets를 모두 직접 시각 검토했고 name·source properties·requirements·implicit 및 줄바꿈/화면 경계에 문제가 없었다. 원본 PNG와 실행 로그는 전용 QA 경로에 보존한다.
- 첫 Backend 실패6은 source ordinary pool에 없는 Reload Speed를 기대한 새 테스트 오류였다. 실제 AdditionalAmmo canonical stat 검증으로 수정했다. 첫 Frontend 실패3은 신규 binding 수를 반영하지 않은 coverage assertion이었다. 기존1812 equipment / compound382 / numeric-single234 보존 assertion을 유지하고 신규71 /50 /18 및 총1883 /432 /252를 별도 검증하도록 확장했다. 실패 로그를 보존했으며 skip·기대값 완화는 하지 않았다.
- 자체 리뷰에서 registry 설명 version의 Bow 복사값을 독립 Crossbow version으로 수정하고 Backend aggregate를 다시 실행했다. 새 numeric binding68의 stat 연결은 모두 유일함을 확인했으며 importer는 ambiguous binding을 거절한다. authored formatter 결과만 scope에 맞게 복사했고 JSON 의미가 동일함을 확인했다.
- [최종 runtime 입력 동등성](evidence/crossbows-source-bundle-2026-10-05/qa-equivalence.json): 변경된 Backend/Frontend40 files가 검증한 QA 복사본과 byte 단위로 동일하다. SHA만 달라진 결과를 검증 완료로 대체하지 않았다.

구현·검증·자체 리뷰 완료 후 local commit하는 Worker 경계다. master 반영·remote push·merge·deploy는 별도 Git 절차이며 이 작업에서 실행하지 않는다. 전용 QA는 정상 `compose stop`으로 종료하고 QA slot을 해제하며, live 서비스 ID·startedAt·mounts는 전후 exact 비교로 확인한다.

## 실제 남은 representative coverage

Crossbow의 서로 다른 source profile 중 **Stout**(Level67, damage30–119, APS1.55, reload0.75), **Dedalian**(Level56, Crit7%, APS1.55), **Cumbrous**(Level52, Ballista limit+1)는 이 6종에 포함하지 않았다. whole-class complete로 표시하지 않는다. 이들의 추가 import는 ordinary provenance·complete class pool·6locale 검증을 포함한 다음 범위로 남긴다. 단순 low-tier 반복과 Runeforged/Runemastered/unique는 이 목록에 섞지 않는다.

정확한 후속 core roster는 [기존 bounded plan](workbench-next-equipment-plan-2026-10-05.md) 및 [source inventory](evidence/belts-source-bundle-2026-10-05/next-equipment/inventory.json)를 따른다.

- One Hand Maces: Fortified Hammer, Strife Pick, Akoyan Club. distinct profile 후보 Molten Hammer, Marauding Mace, Crown Mace도 유지한다.
- Two Hand Maces: Ruination Maul, Fanatic Greathammer, Tawhoan Greatclub. distinct profile 후보 Ironwood Greathammer, Massive Greathammer, Sacred Maul도 유지한다.
- Quarterstaves: Aegis, Bolting, Dreaming. distinct profile 후보 Striking, Razor, Skullcrusher도 유지한다.
- Spears: Grand, Flying, Akoyan. distinct profile 후보 Stalking, Spiked, Guardian도 유지한다.
- Staves: Permafrost, Reflecting, Dark, Ravenous, Perching, Sanctified. 나머지 distinct innate-skill 후보 Icicle, Gelid, Voltaic, Pyrophyte, Chiming, Rending, Reaping, Roaring, Paralysing을 생략하지 않는다. Icicle의 현재 source card는 Firebolt로 표시되므로 이름으로 Freezing Shards를 추정하지 않는다. family/skill identity와 실제 availability는 다음 import 전에 검증한다.
- Talismans: Maji, Fungal, Jade. distinct implicit/profile 후보 Fang, Thunder, Alpha, Ashbark, Spiny, Condemned, Wingbeat를 유지한다. shapeshift skill family는 이름에서 추정하지 않고 실제 detail로 검증한다.
- Shields: Tawhoan Tower Shield, Golden Targe, Blacksteel Crest Shield. Bucklers: Desert Buckler. Foci: Tasalian Focus. 현재 ordinary card inventory에서 Buckler는 공통 Parry/20% Block, Focus는 ES profile이며 별도 distinct implicit 후보 이름은 확인하지 못했다. 다음 detail 검증에서 실제 다른 family가 확인될 때만 추가하며 low-tier 반복을 임의 roster에 넣지 않는다.
- Quivers: Visceral, Volant, Penetrating, Primed, Serrated, Toxic, Blunt, Two-Point, Sacral, Fire, Broadhead. distinct implicit마다 대표1종이며 Penetrating Level55와100% Pierce를 분리한다.

Claws/Daggers/One and Two Hand Swords/Axes/Flails/Traps는 기존 release-unverified 범위를 유지한다. 공개 index 이름만으로 released/craftable로 승격하지 않는다. 다음 권장 bundle은 Shields3 + 최고 Buckler + Tasalian Focus이며, Quivers·melee·Staves/Talismans도 모두 남아 있다.
