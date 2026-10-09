# Belt distinct implicit bundle

시작 HEAD `e5d7c4e663049565de869e0aed826cfa714c8eb5`, branch `workbench/top-bases-20261004`, sole writer. 기존 78종에 ordinary 10종과 Breach identity 3종을 추가하여 **91 bases**를 제공한다. 기존 Rawhide snapshot·modifier ID·film과 기존 78종의 metadata·6locale 번역은 보존한다.

| Key | Metadata suffix | Source implicit | Character Level |
|---|---|---|---|
| linen-belt | FourBelt2 | Mana Recovery from Flasks 20–30% | 없음 |
| wide-belt | FourBelt3 | Flask Charges gained 20–30% | 14 |
| long-belt | FourBelt4 | Charm Effect Duration 15–20% | 20 |
| plate-belt | FourBelt5 | Armour +140–180 | 25 |
| ornate-belt | FourBelt6 | Charm Charges used reduced 10–15% | 31 |
| mail-belt | FourBelt7 | Flask Charges used reduced 10–15% | 40 |
| double-belt | FourBelt8 | Charm Charges gained 20–30% | 44 |
| heavy-belt | FourBelt9 | Stun Threshold 20–30% | 50 |
| utility-belt | FourBelt10 | Flask Recovery applied Instantly 20% | 55 |
| fine-belt | FourBelt11 | Flasks gain 0.17 charges per Second | 62 |
| invoking-belt | FourBeltB2 | Cast Speed 8–12%; local Charm Slots 1 | 32 |
| sinew-belt | FourBeltB3 | Strength +15–20; local Charm Slots 1 | 32 |
| forking-belt | FourBeltB4 | Attack Lightning Damage 1 to 20–30; local Charm Slots 1 | 32 |

Metadata prefix는 `Metadata/Items/Belts/`다. [Source verification](evidence/belts-source-bundle-2026-10-05/verification.json)은 exact ID·Class·tags·family·stat range·locality를 보존한다. Linen에는 source requirement가 없다. `DropLevel 1`을 장착 requirement로 옮기지 않는다. Fine의 canonical stat은 `generate_x_charges_for_any_flask_per_minute = 10`이며 고정 표시값은 source의 `0.17`을 유지한다. Ornate와 Mail의 canonical reduction stat은 `-15..-10`이다. 표시 양수와 저장 음수를 구분한다.

모든 신규 base의 tag는 `belt`다. fresh 6locale class ordinary 135행은 기존 Rawhide raw와 일치하며, 각 hover detail의 ordered Spawn Tags로 전부 eligible임을 확인한다. 기존 modifier ID·family·range·tag와 별도로 게시된 class `DropChance`를 재사용한다. selection은 기존 `POE2DB_AS_PUBLISHED`, numeric roll은 UNVERIFIED source-unit integer model이다. Spawn Tags weight와 class selection weight를 같다고 가정하지 않는다. 다음 class에서 실제 selection weight가 없으면 사용자 승인된 valid-candidate 1/N model을 명시한다.

지원되는 fixed Essence action 21개, replacement action 3개(Perfect Insulation, Abyss, Hysteria)의 source target을 연결한다. ordinary 135종에 weight0 special 4종을 보존한다. Infinite의 Dexterity·Intelligence class rows는 Belt spawn tag가 없고 ordinary Belt pool에도 없으므로 target으로 넣지 않는다. Strength 경로를 유지한다. Insanity와 Alloy를 포함한 제외/deferred mechanics는 추가하지 않는다.

[Catalysts source](https://poe2db.tw/us/Catalysts)는 적용 class를 ring 또는 amulet로 명시한다. Belt를 jewelry로 추정하여 Catalyst를 연결하지 않는다. typed Catalyst quality는 거절하며 runtime quality cap은 `null`이다. manifest의 `maximumQuality: 0`은 지원되는 quality action이 없음을 나타내며 새로운 game cap 계산이 아니다. Essence of the Breach의 Amulet/Ring cap modifier도 Belt에 넣지 않는다.

모든 source popup의 공통 Charm-slot range 1–3은 표시 property다. 신규 rolled Charm-slot state, slot 확률, item-level threshold, 최종 slot 합산을 만들지 않는다. Breach 3종의 별도 `local_charm_slots=1`은 fixed implicit stat으로 보존한다. 공통 range와 fixed stat의 합산/override 의미는 이 snapshot만으로 확정할 수 없다. 권장 경계는 원문 두 정보를 유지하고 실제 slot editor는 증거 확보 뒤 별도 요청으로 구현하는 것이다. variable implicit에는 기존 Divine/Blessed 모델을 연결하고 fixed-only implicit은 Blessed를 거절한다. 기존 Rawhide Divine 거절은 그대로 유지한다.

Golden Obi의 `BeltDemigods1`은 Demigod provenance로 제외한다. Stalking의 socket bonus transfer와 Runemastered Heavy의 `VerisiumUnique1..4` 및 Runic Ward/Alloy mechanics도 제외한다. `Mods.enable_rarity`만으로 ordinary availability를 판정하지 않는다. source HTML의 라이선스·출처를 보존하고 raw trailing whitespace를 수정하지 않는다. `.gitattributes`는 HTML byte fidelity를 위해 newline conversion만 끈다.

QA copy는 `codex/belts-qa-20261005`, Compose project는 `exile-belts-20261005`, localhost ports는 20380/20381이다. heavy checks는 sequential exclusive다. live18080/18081, 사용자 browser storage, 기존 DB volumes와 원래 repository는 보호한다. Support/Explorer는 Solar-only, registry220/deferred50와 quality overflow/HALF_UP·Shift/Alt·orange preview를 유지한다. local commit만 수행하며 push/merge/deploy는 하지 않는다.

Frontend 첫 coverage 실패는 source requirement가 없는 Linen을 신규 manifest에서 Level1로 취급한 문제였다. metadata를 0으로 수정하고 exact source identity와 6locale requirement 부재를 검사한다. 첫 log를 보존하며 검사를 skip하거나 source 없는 requirement를 만들지 않는다. 후보 inventory의 Penetrating Quiver에서는 HTML을 평문으로 합친 뒤 Level55와 implicit100%가 붙는 parsing 오류를 자체 리뷰로 발견했다. original inventory를 보존하고 [reviewed candidates](evidence/belts-source-bundle-2026-10-05/next-equipment/reviewed-candidates.json)는 HTML 경계에서 Level55를 확인한다.

다음 범위는 [bounded equipment plan](workbench-next-equipment-plan-2026-10-05.md)이다. 이 bundle은 whole-game complete가 아니다.

최종 검증: Backend unit/architecture **454**, Docker integration **6**; Frontend **1859**; API **1248**; implicit/slot **84**; browser **1027**; filled legacy **7**. failures/errors/skips와 browser page errors는 0이다. Backend의 check/generateJooq/bootJar, Frontend npm ci/lint/typecheck/format/test/build를 격리 복사본에서 완료했다. 마지막 Frontend는 log4의 npm ci/lint/typecheck와 log5의 format/test/build를 합쳐 검증하며, 새 method chain의 고정 Prettier second pass를 기록한다.

6locale desktop/mobile screenshot **324장**, contact sheet **54장**을 모두 시각 검토했다. [Runtime validation](evidence/belts-runtime-bundle-2026-10-05/validation.json), [API checks](evidence/belts-runtime-bundle-2026-10-05/api-results.json), [browser checks](evidence/belts-runtime-bundle-2026-10-05/browser-results.json), [screenshot SHA index](evidence/belts-runtime-bundle-2026-10-05/screenshots.json), [failure recovery](evidence/belts-runtime-bundle-2026-10-05/failure-history.json)에 검증 범위와 실패 이력을 보존한다. 검증된 backend/frontend src 621개와 작업 source의 bytes가 일치한다.

첫 browser 시도는 성공한 HTTP200 결과의 null quality cap을 Frontend 검증기가 거절하는 회귀를 발견했다. reviewed Belt class만 null-cap 허용에 추가하고 실제 응답 수용13건/가짜 cap 거절13건을 추가했다. 다른 class 규칙과 기존78 identity, registry220/deferred50, old/new film, Solar quality overflow/HALF_UP, Shift/Alt/orange 검증은 유지했다. API의 host 경유 timeout은 QA health UP 확인 뒤 전용 Compose network에서 동일 assertions로 검증했으며 이전 output을 덮어쓰지 않았다.

QA는 전용 project만 `docker compose down`으로 정상 종료했고 heavy QA slot을 반환했다. [release evidence](evidence/belts-runtime-bundle-2026-10-05/qa-release.json)의 live container ID/start time/mount 비교는 일치한다. 기존 volumes와 사용자 browser storage를 보존했으며 push/merge/deploy는 수행하지 않았다.
