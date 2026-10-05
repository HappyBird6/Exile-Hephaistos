# Quiver distinct-implicit core 11종

기준 `aa741d572e23067e74bb17aaf6daf3a3949fb87b`, branch `workbench/top-bases-20261004`. 기존102개에 Quiver11개를 추가하여 **113 bases**를 제공한다. class20 / `Quivers`는 독립 class이며 Bow pool alias가 아니다.

| Base | Metadata suffix | 요구 level | 기본 implicit |
|---|---|---:|---|
| Visceral Quiver | FourQuiver11 | 65 | (20—30)% increased Critical Hit Chance for Attacks |
| Volant Quiver | FourQuiver10 | 61 | (20—30)% increased Arrow Speed |
| Penetrating Quiver | FourQuiver9 | 55 | 100% chance to Pierce an Enemy |
| Primed Quiver | FourQuiver8 | 51 | (7—10)% increased Attack Speed |
| Serrated Quiver | FourQuiver7 | 44 | Attacks have (20—30)% chance to cause Bleeding |
| Toxic Quiver | FourQuiver6 | 39 | (20—30)% chance to Poison on Hit with Attacks |
| Blunt Quiver | FourQuiver5 | 33 | (25—40)% increased Stun Buildup |
| Two-Point Quiver | FourQuiver4 | 24 | (20—30)% increased Accuracy Rating |
| Sacral Quiver | FourQuiver3 | 16 | Gain (2—3) Life per Enemy Hit with Attacks |
| Fire Quiver | FourQuiver2 | 8 | Adds 3 to 5 Fire damage to Attacks |
| Broadhead Quiver | FourQuiver1 | 없음 | Adds 1 to 3 Physical Damage to Attacks |

ID prefix는 모두 `Metadata/Items/Quivers/`다. attribute 요구는 없다. Broadhead의 source 요구사항은 빈 문자열이며 character requirement0을 보존한다. 생성 프리셋 itemLevel1은 character requirement와 별개이다. 각 distinct implicit의 현재 ordinary 대표를 선택한 것이며 저레벨 반복 tier를 채우는 목표가 아니다. 수집한 [현재 Quivers class 페이지](https://poe2db.tw/us/Quivers)의 ordinary base list11종과 일치하고 같은 implicit의 더 높은 ordinary replica는 이 source 목록에 없다. Quiver 안의 추가 의미 있는 미선택 ordinary variant는 확인되지 않았다. unique-only base나 unreleased category를 추가하지 않는다.

[6locale source bundle](evidence/quivers-source-bundle-2026-10-05/)은 원본 HTML, URL·조회 시각·SHA256, canonical implicit와 modifier stats·locality·families·ordered spawn을 보존한다. 각 normal popup과 normal/magic/rare flags, class base list는 ordinary 형태를 확인한다. 독립 GGG trade2 item 목록 요청은403이므로 공식 trade 목록을 읽었다고 주장하지 않는다. exact drop 장소·확률은 미확인이다. [source review](evidence/quivers-source-bundle-2026-10-05/source-review-final.json)에 이 근거 경계를 기록한다.

## Pool과 제작 제한

ordinary100행 전부를 게시 weight로 보존한다. 기존 정확한 definition70개는 ID·stats·text·binding을 재사용하고 새 ordinary30개·implicit11개의 binding을 추가한다. implicit craft tags도 source에서 읽는다. conditional/projectile/Bow Skill Damage/추가 화살의 Surpassing chance는 표시와 canonical roll만 제공한다. 전투·최종 피해·적중·최종 발사체 수를 계산하지 않는다.

일반100행의 Attack Speed 첫 행에는 같은 이름 weapon fallback이 연결될 수 있었다. 원본 `normal-57.json`은 보존하고 별도 `normal-57.corrected.json`에서 `IncreasedAttackSpeed1`, Global `attack_speed_+%`5~7와 `gloves:1, quiver:1, default:0`를 검증하였다. Local weapon stat을 Quiver에 넣거나 행을 제외하지 않는다. 일본어 추가 화살의 `1個`와 Sacral의 `敵1体`는 literal 의미이며 variable roll token으로 오인하지 않는다. canonical range와 localized literal을 함께 유지한다.

fixed Essence는 Lesser/일반/Greater Infinite의 실제 Dexterity target3개와 Greater Battle의 Global Accuracy target1개이다. Strength·Intelligence·Local Accuracy source variants는 ordered spawn이 Quiver를 허용하지 않아 선택하지 않는다. Hysteria는 ordinary Bow Skill Damage43~50% target을 재사용하며 ordinary weight를0으로 바꾸지 않는다. Abyss replacement2개는 기존 검증한 전체 Mark definition을 사용한다. 게시 ordinary weight가 없는 special candidate 선택에만 기존 승인된 disclosed1/N을 사용하며 ordinary weights를 균등화하지 않는다. Mystic Alloy는 기존 deferred 범위로 유지한다.

기존 일반·Greater/Perfect currencies와 Crystallisation/Coronation/Alchemy/Exaltation/Annulment/Whittling Omen의 generic affix 경로를 연결한다. Blessed는 variable implicit이 있는8종에만 적용하며 fixed Penetrating/Fire/Broadhead에는 적용하지 않는다. registry는 실제 canonical stats로 판정한다.

`Quality.max_quality`는 source에 없으므로 quality cap을 부여하지 않는다. Catalyst typed quality와 supplied socket state/Artificer는 미지원으로 거부한다. generic `Sockets.socket_info`의 positive socket에는 level9999가 있으므로 ordinary usable maximum0을 기록하며 Bow/Shield의 socket 정책을 복사하지 않는다. 기본 문구 “Can only be equipped if you are wielding a Bow.”는6locale source 표시로 보존하며 equipment loadout simulation은 하지 않는다.

## 보존과 격리 검증

[Preservation evidence](evidence/quivers-source-bundle-2026-10-05/preservation.json)는 기존102 manifest·Essence target·modifier templates·6locale terms와 registry deferred50의 동등성을 확인한다. Solar-only Support/Explorer, 기존 films/IDs, quality overflow/HALF_UP, Shift/Alt와 orange preview를 유지한다.

전용 QA 경로 `codex/quivers-qa-20261005`, Compose project `exile-quivers-20261005`, localhost20680/20681을 사용한다. 이전 QA output은 덮어쓰지 않으며 heavy checks는 exclusive sequential로 실행한다. live18080(master7d27d6c)/18081(UI18a2a3d), original repo와 DB volumes·사용자 browser storage·unrelated services를 보존한다. 구현·검증·자체 리뷰·local commit 완료는 최종 QA 증거로 별도 기록한다. push/merge/deploy는 수행하지 않는다.

## 다음 source-backed core roster

[앞선 source inventory](evidence/belts-source-bundle-2026-10-05/next-equipment/reviewed-candidates.json)와 [Offhand bundle의 남은 roster](workbench-offhands-bundle-2026-10-05.md)를 유지한다. 이 목록은 이번에 구현했다는 뜻이 아니며 ordinary availability와 전체 class pool/detail을 후속 단계에서 검증한다.

- One Hand Maces3: Fortified Hammer, Strife Pick, Akoyan Club. distinct 후보 Molten Hammer, Marauding Mace, Crown Mace.
- Two Hand Maces3: Ruination Maul, Fanatic Greathammer, Tawhoan Greatclub. distinct 후보 Ironwood Greathammer, Massive Greathammer, Sacred Maul.
- Quarterstaves3: Aegis, Bolting, Dreaming Quarterstaff. distinct 후보 Striking, Razor, Skullcrusher.
- Spears3: Grand, Flying, Akoyan Spear. distinct 후보 Stalking, Spiked, Guardian.
- Staves6: Permafrost, Reflecting, Dark, Ravenous, Perching, Sanctified Staff. innate-skill 후보 Icicle, Gelid, Voltaic, Pyrophyte, Chiming, Rending, Reaping, Roaring, Paralysing. Icicle의 source card는 Firebolt이며 이름으로 skill을 추정하지 않는다.
- Talismans3: Maji, Fungal, Jade Talisman. distinct 후보 Fang, Thunder, Alpha, Ashbark, Spiny, Condemned, Wingbeat; shapeshift family/detail/ordinary availability 확인 후 선택한다.

Shield sidegrade Venerable Defender와 Crossbow Stout/Dedalian/Cumbrous는 앞선 bundle의 미선택 이유를 유지한다. Claws/Daggers/One and Two Hand Swords/Axes/Flails/Traps는 release-unverified다. whole-game 완료를 의미하지 않는다.

## 최종 검증과 자체 리뷰

격리 Linux 복사본에서 `sh gradlew --no-daemon spotlessApply check generateJooq bootJar`가 통과했다. **Backend unit476 + integration6**, failures/errors/skipped0이며 formatter·ArchUnit·임시 DB migration/codegen·bootJar를 포함한다. Frontend는 `npm ci`, `npm run lint`, `npm run typecheck`, `npm run format:check`, `npm run test -- --run`, `npm run build` 모두 통과했다. **66 files / 1903 tests**다. Windows 앱 실행 스크립트·의존성·lockfile·원래 Compose 설정은 변경하지 않았다. 전용 Compose `config --quiet`도 통과했다.

[API877](evidence/quivers-source-bundle-2026-10-05/api-results.json), [browser1112](evidence/quivers-source-bundle-2026-10-05/browser-results.json), [legacy25](evidence/quivers-source-bundle-2026-10-05/old-filled-results.json)가 통과했다. API는 old102 full initial DTO, complete ordinary100 source stats/weights/families/tags, 지원 Essence 전체 target/minimum level, 일반·Greater/Perfect 화폐, Omen, invalid class/socket/quality, itemLevel1과 character requirement 구분, Solar-only Support/Explorer를 확인했다. browser는 Transmutation/Infinite/Abyss 성공과 Command/Body 거부의 film 보존, Omen of the Blessed + Divine의 variable8 허용/fixed3 거부, exact implicit 값, Shift·Alt, 6locale names/properties/requirements, desktop1440/mobile390 overflow, local orange preview, old102 film 복원, Solar overflow40/cap20/HALF_UP을 확인했다. page errors0이다.

신규11×6locale×desktop/mobile card132·full132, 기존 Wand/Sceptre/Hallowed×6locale card18·full18, orange6·Solar6으로 **312 original screenshots**를 보존했다. card contact25·full contact27 모두 직접 시각 검토했고 핵심 원본도 추가 확인했다. [Screenshot hash 목록](evidence/quivers-source-bundle-2026-10-05/screenshots.json)은 전용 QA `browser-attempt-4`를 가리킨다. [QA 완료 근거](evidence/quivers-source-bundle-2026-10-05/qa-completion.json)에 검사 결과·실패 이력·live 보존·QA 해제를 기록했다.

첫 Frontend pretest의 Broadhead 요구 조건 없음과 두 번째 테스트의 historical binding count 실패를 보존했다. historical234와 신규38을 분리해 검사하며 모든 값·6locale 렌더링 검사를 유지했다. 초기 browser에서는 정상 API `qualityLimit:null`을 Frontend allowlist가 거부한 실제 문제를 발견했다. Quivers를 명시하고 실제 서버 응답11종의 정상 수용·임의 cap20 거부 회귀22개를 추가했다. 이후 전체 Frontend 검사를 다시 통과했다. 세 번째 browser의 Essence ID 오타는 검증 helper만 교정하고 모든 browser 검사를 새 경로에서 재실행했다. [수집·수정 이력](../scripts/quivers-source-provenance.json)과 각 실패 log·진단 화면은 보존했다. 기대값 완화·skip·원본 삭제는 하지 않았다.

자체 리뷰에서 class20/Metadata ID, Quiver ordered spawn과 전체 pool, Global Attack Speed, source weight·canonical stat·family/tag, fixed/replacement scope와 6locale literal을 대조했다. [최종 보존 검사](evidence/quivers-source-bundle-2026-10-05/preservation-completion-final.json)는 old102와 deferred50의 의미를 확인한다. [최종 입력 동등성](evidence/quivers-source-bundle-2026-10-05/final-qa-inputs.json)은 검증 복사본과 최종 source/config703개, runtime jar와 build artifact SHA를 대조한다. 소스·test 입력의 후속 변경은 없다. 독립 GGG trade 자료의403과 exact drop 위치·확률 미확인은 위의 근거 한계로 남긴다.

전용 `exile-quivers-20261005` 네 서비스만 `docker compose stop`으로 정상 종료했다. app exit143(SIGTERM), 나머지 exit0, OOM0이며 down/volume 삭제는 하지 않았다. live18080/18081의 네 서비스 ID·시작 시각·mounts는 전후 정확히 같다. 자기 `heavy-qa-owner.json`은 active:false로 해제했다. 기존 QA 출력·stopped QA·실패 증거는 다음 fresh chat을 위해 보존한다. local commit만 수행하며 push/merge/deploy는 하지 않는다.

72개 checksum-bound raw HTML에는 `.gitattributes -text`를 적용한다. staged 원문과 source SHA 일치는 [Git index checksum proof](evidence/quivers-source-bundle-2026-10-05/git-index-proofs.json)로 확인한다. immutable 원문의 trailing whitespace는 보존하며, code·JSON·문서 등 non-raw staged whitespace 검사를 통과했다.
