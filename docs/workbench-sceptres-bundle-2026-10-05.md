# Sceptre ordinary skill-family bundle

기준 HEAD `5ae15bfbeaa29a134a422f8a84013928e688064a`, branch `workbench/top-bases-20261004`, sole writer. 기존 71개에 ordinary skill variant 7개를 추가하여 총 78개를 제공한다. Hallowed와 Rattling 및 기존 snapshot/modifier identity는 보존한다.

| Key | Metadata suffix | Granted skill | Skill source Id | Character requirements |
|---|---|---|---|---|
| stoic | FourSceptre2 | Discipline | Discipline | Level 6, Int 12 |
| omen | FourSceptre4 | Malice | Malice | Level 16, Str 12, Int 25 |
| shrine-fire | FourSceptre6a | Purity of Fire | PurityOfFire | Level 26, Str 17, Int 38 |
| shrine-ice | FourSceptre6b | Purity of Ice | PurityOfIce | Level 26, Str 17, Int 38 |
| shrine-lightning | FourSceptre6c | Purity of Lightning | PurityOfLightning | Level 26, Str 17, Int 38 |
| clasped | FourSceptre8 | Heart of Ice | HeartOfIce | Level 36, Int 65 |
| wrath | FourSceptre10 | Fulmination | Fulmination | Level 49, Int 87 |

Metadata prefix는 `Metadata/Items/Weapons/OneHandWeapons/Sceptres/`이다. [Source verification](evidence/sceptres-source-bundle-2026-10-05/verification.json)과 [skill identities](evidence/sceptres-source-bundle-2026-10-05/skill-identities.json)에 실제 linked skill page의 fields를 보존했다. Shrine은 동일 display name을 유지하되 ID와 key 및 locale term을 구분하며 selector에 granted skill을 표시한다. `FourSceptreUnique1`의 Impurity는 Palm of the Dreamer unique provenance를 가진 별도 variant이므로 ordinary roster에 포함하지 않는다. `Mods.enable_rarity`만으로 ordinary availability를 판단하지 않는다.

모든 7개 base의 Spirit은 100, 최대 quality는 20이다. Grants Skill은 display property이며 craftable implicit/explicit 또는 combat simulation으로 변환하지 않는다. reservation, damage, aura effect, skill-level scaling은 구현하지 않는다. 실제 base tag와 각 modifier의 ordered Spawn Tags에서 첫 일치 weight를 검증했으며 모두 ordinary 150행 전체가 eligible이다. 기존 Rattling/Hallowed modifier definitions와 Perfect Command special 1행을 재사용한다. eligible 미매핑 행은 0이다. published `DropChance`를 사용하며 기존 `POE2DB_AS_PUBLISHED` 정책과 numeric roll UNVERIFIED source-unit uniform 정책을 유지한다.

Six-locale class rows의 family와 기존 display template 대응을 모두 검증했다. Spanish Stoic(추가 Str7), Clasped(Str22/Int51), Wrath(Str28/Int68)의 카드 요구치는 English current source와 다르다. 원본 evidence를 보존하고 표시에는 current English numeric requirement와 source Spanish vocabulary를 사용한다. 다른 locale 값은 source 그대로 표시한다.

전용 QA copy는 `codex/sceptres-qa-20261005`, Compose project는 `exile-sceptres-20261005`, ports는 localhost 20280/20281이다. heavy checks는 sequential exclusive이다. live18080/18081, DB volumes, 원본 repo, 사용자 browser storage는 보존하며 remote push/merge/deploy는 수행하지 않는다. source 수집의 첫 sandbox 시도는 네트워크 EACCES로 실패하여 빈 evidence directory만 남겼다. network 승인 후 새 파일만 생성했으며 이전 output을 덮어쓰지 않았다.

다음 구체 범위는 Belt의 Rawhide 외 Linen/Wide/Long/Plate/Ornate/Mail/Double/Heavy/Utility/Fine distinct implicit roster와 Invoking/Sinew/Forking Breach identity이다. Golden Obi의 Demigod provenance, Stalking의 socket transfer, Runemastered Heavy4는 ordinary availability/mechanic 근거가 추가로 필요하다. 이후 Crossbow, melee, Shields/Foci/Quivers는 complete class pool, properties/requirements, implicit/skill, socket 제약을 source로 확인한 뒤 확장한다. all-low-tier 확장은 이번 범위에 포함하지 않는다.

Infinite Essence의 특별 attribute target은 기존 Sceptre 지원 경계를 유지한다. source class rows에 존재한다는 사실을 보존하며 이번 ordinary base 확장으로 지원 범위를 넓히지 않는다. 기존 Command 및 source-reviewed fixed Essence들과 Perfect Command가 새 base에 동일하게 적용된다.

최종 [runtime validation](evidence/sceptres-runtime-bundle-2026-10-05/validation.json)은 Backend unit/architecture441 + Docker integration6, Frontend 필수 검사 및1820 tests/build, API400, browser740, legacy filled film7을 모두 통과했다. 검증 copy와 최종 소스577개가 byte 단위로 일치한다. [Screenshot index](evidence/sceptres-runtime-bundle-2026-10-05/screenshots.json)의 180장과 contact sheets30장을 pixels 자체 리뷰했다. 카드84장/contact14장은 앞서 직접 검토한 원본과 byte 단위로 동일하며, 최종 full-page contact16장은 수정 후 직접 검토했다. [실패 이력](evidence/sceptres-runtime-bundle-2026-10-05/failure-history.json)을 보존한다. 독립 리뷰가 아닌 sole-writer 자체 리뷰다.

첫 pixels 자체 리뷰에서 신규 Sceptre도 기존 Skeletal Warrior scope 안내를 상속하는 표시 결함을 발견했다. 기존 Rattling/Hallowed 문구와 번역을 보존하고 신규 7종에는 source granted skill을 일반적으로 설명하는 6locale key를 추가했다. 첫 captures `browser-attempt-1`과 첫 성공 Frontend log를 보존했다. 수정 후 필수 Frontend 검사를 전체 재실행하고, 잘못된 legacy scope 안내가 없는지 추가 assertion으로 검증한다.

첫 API 실행은 app 시작 직후 `UND_ERR_SOCKET`으로 실패했다. 실패 output `api-attempt-1`은 보존하고 준비된 runtime에서 새 `api-attempt-2`로 동일 검증과 higher-tier positive 검증을 실행하여 400개 검사를 통과했다.

추가 source 검증에서 Spawn Tags 수치와 class `DropChance`를 동일 selection weight로 비교한 새 assertion이 실패했다(`Strength1`: Spawn Tags sceptre 1, class DropChance 250). 두 source field의 의미를 동일시하지 않는다. tag evidence는 applicability에 사용하고 selection weight는 source-backed class `DropChance`로 별도 엄격 검증한다. 실제 catalog 값이나 기존 테스트 기대값은 변경하지 않았다.

[QA release](evidence/sceptres-runtime-bundle-2026-10-05/qa-release.json)는 전용 QA4 services를 정상 종료하고 exclusive slot을 해제했음을 확인한다. live4 service ID·시작 시각·mount는 전후 동일하다. DB volume 제거와 remote 작업은 수행하지 않았다.

전체 staged `git diff --check`는 원본 HTML trailing whitespace 때문에 통과하지 않는다. 코드·데이터 formatter 필수 검사는 모두 통과했고 raw source의 공백은 수정하지 않았다. 해당 source evidence 폴더의 `.gitattributes`는 HTML의 Git 줄바꿈 변환만 막으며 whitespace 검사 규칙은 변경하지 않는다. [Staged source preservation](evidence/sceptres-runtime-bundle-2026-10-05/staged-source-preservation.json)은 captured HTML37개와 staged bytes의 정확한 일치를 확인한다.
