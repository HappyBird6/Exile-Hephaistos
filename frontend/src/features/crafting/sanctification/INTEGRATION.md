# C: 방어 수치·축성·품질 경계

- 기준: `4fb6b89d0f2319444742aac336f9b1a1832b515c`.
- 작업: `codex/quality-sanctification-current-c-20261103`, `.codex/worktrees/df9a/Exile-Hephaistos`.
- 앞서 생성한 `task-5/quality-sanctification-c`는 기준 상태 그대로 보존했으며 구현하지 않았다.
- 모델/effort는 통합자가 지정한 gpt-6.1-sol/high. standard tier는 설정 필드가 없어 미확인. 설정을 임의 변경하지 않았다.

## 요구 12/13: 카드 연결 계약

`derived-defences/derivedDefences.ts`의 `derivedDefences(state, definitions)`를 호출한다. 반환의 `armour/evasion/energyShield.value`가 null이 아니면 기존 카드 property의 숫자로 사용한다. 기존 `base.armour`, `base.evasion`, `base.energy_shield` 번역과 기존 1자리 formatter를 재사용한다. 중간 결과를 반올림하거나 반환값을 `modifier.values`에 다시 저장하지 않는다.

계산은 `(base + local flat) * (1 + local increased / 100) * (1 + equipmentQuality / 100)`다. `source-stat-audit.json`에 실제 catalog의 9개 local stat ID, 출현 pool, 누락된 broad modifier tag를 기록했다. 일부 offhand modifier는 broad tags가 비어 있으므로 **정확한 local stat ID**로 해석한다. global defence·ring/jewel stat·이전 표시 합계는 합산하지 않는다. hybrid stat은 각 해당 방어 항목에서 한 번씩만 합산한다. frontend/backend top-base 원본 값은 동일하다. legacy Stocky Mitts armour 15는 이번 공개 캡처에서 확인했다. Helmet 29/Body 45는 기존 UI의 검토된 base 값을 유지한다.

WorkbenchService.Initial에 `equipmentQuality`를 추가했다. 새로 생성한 armour/martial 상태는 0이며, 기존/가져온 상태의 null은 0으로 추정하지 않는다. `concreteInitial`은 응답의 값을 보존한다. 기존 기록의 품질 미관측은 helper에서도 null이다.

가져온 원문의 Armour/ES/Evasion 합계는 관측이다. 이를 base/flat 입력으로 재사용하면 중복 계산된다. `defenceObservation(computed, observed)`는 양쪽 숫자를 그대로 반환하며 일치/충돌/품질미관측을 구분한다. 원문·기존 관측 줄은 저장·내보내기에서 보존하고, 재계산값으로 원문을 덮어쓰지 않는다. 최대 품질 문구 제거는 D/root 카드 소유 변경이며 cap 검증은 그대로 유지했다.

## 요구 16: 사용자 선택 +1%p 정책 — 추가 결정 반영

명칭/ID/action:

| material              | action                | 대상                  |
| --------------------- | --------------------- | --------------------- |
| Armourers_Scrap       | ARMOURERS_SCRAP       | 검토된 armour         |
| Blacksmiths_Whetstone | BLACKSMITHS_WHETSTONE | 검토된 martial weapon |

정확한 이름은 Armourer's Scrap / Blacksmith's Whetstone이다. 기존 6개 언어 `gameTerms.json`에 이미 있다. wand/staff/sceptre의 품질 화폐는 Arcanist's Etcher이므로 Whetstone으로 변경하지 않는다. ordinary 최대 품질 20과 이미 보유한 20 초과 품질 보존은 기존 정책을 따른다.

사용자가 직접 선택한 **희귀도와 무관하게 1회 +1%p**를 구현했다. 정책 버전은 Java `QualityCurrencyRules.VERSION` / TypeScript `equipmentQualityPolicy`의 `user-selected-equipment-quality-plus-one-v1`이다. Normal/Magic/Rare 모두 0→1, 19→20으로 적용한다. 20 이상에서는 거부하며 기존 20 초과 품질을 낮추지 않는다. null 품질은 추정하지 않고 거부한다. SANCTIFIED 등 수정 불가 condition·다른 대상 class도 거부한다. UNIQUE는 기존 전체 Workbench 범위 밖이다.

성공 응답은 `events: []`, `consumedOmens: []`, 기존 `remainingOmens` 전체 보존 및 위 정책 ID의 `assumptions` 1개를 반환한다. assumption의 n/min/max는 모두 1이며, **사용자 선택 simulator 정책임을 명시한다**. 장착 관측·분열·미공개 훼손 슬롯·implicit/explicit·socket·기타 상태는 바꾸지 않는다. 클라이언트는 +1과 이 불변을 다시 검증한다.

공개 게임 자료에서 실제 rarity별 1회 증분은 여전히 확정하지 못했다. 이 구현을 실제 게임 증분 VERIFIED로 표기하지 않는다. 기존 5/2/1 추정값·`.env`·운영 설정을 사용하지 않는다. root는 품질 2종 registry/serviceScope의 DEFERRED를 **사용자 선택 정책 실행 지원**으로 변경하고, B의 Abyss passthrough 및 카드 연결을 최종 검증해야 한다. C는 root 소유 registry/자동 policy/B 소스를 수정하지 않았다. 기존 네 공통 패치는 추가 변경 없이 유지된다.

미공개 훼손 슬롯을 가진 상태의 일반 `/apply` 연결에서는 root가 기존 `controller.patch`의 `allowDesecrated` 조건에 `QualityCurrencyRules.isQuality(request.action()) || 기존 축성 trigger 조건`을 추가해야 한다. C 엔진의 +1 경로는 슬롯을 수정하지 않으며 이 경우도 검증했다. 다른 action에 슬롯 실행을 허용하는 변경은 필요 없다. B의 Abyss wrapper를 통과할 때도 item의 품질만 +1하고 기존 slot/offer/active omen을 보존하는 연결은 root가 구현·검증한다. root의 현재 dirty Controller/B 파일은 C가 수정하지 않았다.

WorkbenchSimulator의 수동 정책 suffix는 `-sanctification-independent-stat-percent-v1-user-selected-equipment-quality-plus-one-v1`로 바뀐다. `automaticIdentity()` 및 legacy reader는 이번 후속 작업에서 수정하지 않았다. root가 진행 중인 read-only legacy 호환성 처리에서 이전 `equipment-quality-deferred-v1` suffix와 이 사용자 정책 suffix를 구분해 검토해야 한다.

## 요구 17: 축성 계약

- 활성 ID `Omen_of_Sanctification`, trigger `DIVINE`, Rare에만 적용한다. 같은 trigger인 Blessed와 동시 활성은 기존 조합 검증으로 거부하며 소비하지 않는다. 다른 trigger의 알려진 omen은 남는다.
- 기존 `SANCTIFIED` condition을 재사용한다. 새 `ItemState.sanctification` / `ConcreteItem.sanctification`은 `{policy:'independent-stat-percent-v1', implicits:{modifierId:{statId:percent}}, explicits:{...}}` 구조다. percent는 78..122 정수다. modifier.values는 원래 정수 그대로다.
- `sanctifiedValues(state, modifier, 'implicits'|'explicits')`가 원본 값에 배율을 정확히 한 번 적용한다. 표시값만 기존 1자리 formatter를 사용한다. catalyst와 결합할 때 중간 정수 반올림을 피한다. 기존 `catalystProjection`의 정수 표시값에 배율을 중복 적용하지 않는다.
- `sanctifiedLabel(locale)`는 6개 언어 카드 하단 표시용이다. 실제 flag는 `conditions.includes('SANCTIFIED')`다.
- 공개 게임 keyword는 78..122 범위와 대부분의 수정 제한을 설명한다. **독립적인 각 stat별 균등 정수 percent, implicit/explicit 포함 범위, 분열 수치의 표시 배율, 미공개 슬롯을 유지하는 처리는 명시적인 simulator 정책**이다. 알려진 게임 분포나 rounding으로 주장하지 않는다. 같은 modifier의 여러 stat에 독립 배율을 쓴다는 점도 게임의 modifier 단위 배율과 구분해야 한다.
- 기본 품질, 요구사항, socket/affix capacity, 최대 품질, binary skill/socket metadata와 `unscalable` modifier는 대상에서 제외한다. 수치와 percent 메타데이터는 별도로 보존한다. 이미 장착된 Augment의 해석되지 않은 설명·미공개 훼손·분열 identity·catalyst 품질·socket 관측을 삭제하지 않는다. Augment 설명 자체에 배율을 추정하지 않는다.
- 기존 base policy가 Divine을 허용하는 검토된 Rare base에 적용된다. basic/time-lost Jewel도 포함한다. `reviewedBasicJewel`는 현재 conditions를 거부하지 않고 원본 roll을 검증하므로 추가 패치가 필요 없다. Belt 등 Divine 미지원 base는 계속 미지원이다.
- 지원된 후속 수정은 모두 거부한다. 이는 **현재 구현에서 확인한 수정 예외가 없다는 보수적 지원 정책**이며 실제 게임의 모든 조작이 금지된다는 뜻이 아니다. 공개 keyword와 0.5.2 Augment 관련 변경을 조사했으나 현재 지원 화폐의 허용 예외를 확정하지 못했다. 읽기/저장/복원/내보내기는 허용한다. 향후 기능 추가를 임의로 구현하지 않았다.
- 일반 Divine의 implicit 보존 불변은 유지했다. 축성은 일반 Divine reroll을 먼저 적용하지 않고 원래 roll+배율을 보존한다. 이 처리 또한 simulator 정책이다.

### catalyst/Jewel 효과와 축성의 정확한 표시 합성

`preciseModifierProjection.ts`는 원본 정수만 받는다. 기존 `catalystProjection`을 원본 값으로 호출하여 검토된 적용 자격/status를 재사용하지만, 그 함수의 반올림된 값은 합성에 쓰지 않는다. 실제 합성은 `source * (100 + matchingQuality) * (100 + existingJewelEffect) * sanctificationPercent / 1_000_000`의 BigInt 정수비다. 기존 Jewel 효과의 source 값을 사용하며 새 효과 전파 규칙을 추정하지 않는다. 예를 들어 11 × 1.20 × 0.79는 10.428이며 13 × 0.79로 바뀌지 않는다. 해당 Jewel 효과가 40%이면 14.5992다.

카드의 기존 modifier map 내부에서 다음처럼 연결한다. **정밀 helper는 SANCTIFIED 표시에서만 호출하고 일반 상태의 기존 사용자 선택 정수 정책은 유지한다.**

```ts
const legacy = catalystProjection(
  d,
  m.values,
  concrete.catalystQuality,
  concrete,
)
const exact = concrete.sanctification
  ? preciseModifierProjection(
      concrete,
      d,
      m,
      i < concrete.implicits.length ? 'implicits' : 'explicits',
    )
  : null
const displayText = localizedModifierText(d, exact?.values ?? legacy.values)
```

`localizedModifierText`의 기존 6개 언어 숫자 formatter에서만 1자리 표시를 적용한다. `legacy.status` 또는 `exact.qualityStatus`는 기존 적용 자격/status이며 새 게임 확률을 나타내지 않는다. `exact.ratios`는 원래 정수비를 보유한 표시 계산 전용 BigInt다. 저장 JSON/ItemState/기록에는 이 helper의 반환 객체를 넣지 않고 원본 `modifier.values`와 숫자형 `sanctification` 메타데이터만 저장한다. `sanctifiedValues`의 소수 Map을 `catalystProjection`으로 전달하면 안 된다.

## root/D가 적용할 공통 패치

이 폴더의 raw patch는 **원본 파일에 적용하지 않았다**.

1. `common.patch`: WorkbenchDefinitions의 BLESSED/SANCTIFICATION trigger switch, `workbench-definitions-v1.json`의 enum 순서대로 품질 2개와 omen 1개 append.
2. `serialization.patch`: 기존 CraftingConfiguration.ItemStateJson mixin에 sanctification NON_NULL accessor. ItemState는 Jackson-free를 유지한다. 구기록에 새 null 필드를 강제로 직렬화하지 않는다.
3. `controller.patch`: sanctification 필드 허용, factor의 정수/범위 검증. 축성 trigger의 `/apply`에서 훼손 슬롯을 보존한다. `/actions`, `/quality-display`는 read-only 슬롯 검증을 허용한다. 다른 일반 action에 미공개 슬롯 실행을 열지 않는다.
4. `history.patch`: D 소유 workbenchHistory의 단일 SANCTIFIED condition 복원 허용 및 `validSanctification(state, initial.modifiers)` 검사. D의 기존 수정과 합쳐 검토해야 한다.

원본 common resource seal/registry/manifest는 root 소유다. `node scripts/update-ruleset-manifest.mjs`로 최종 **실제 바이트**의 digest를 갱신하고, `RulesetManifestLoaderTest`, registry 검사 및 UI/API 초기 상태를 최종 통합 검증한다. C의 임시 후보는 common patch의 resource 한 개에 대해서만 실제 SHA256으로 seal 값을 맞췄으며 seal 검증을 끄지 않았다. 원본 catalog 숫자나 기대값을 완화하지 않았다. WorkbenchSimulator ruleVersion에는 새 축성 정책/품질 보류 suffix가 추가된다.

품질 2종 registry는 위 사용자 결정에 따라 root에서 정책 지원 상태로 변경한다. Sanctification registry 활성화에는 위 전체 연결이 필요하다. sourceSha256은 `docs/evidence/quality-sanctification-c-20261103/source-captures.json`의 실제 개별 페이지 캡처값을 사용한다. 이 값은 원문 캡처의 hash이고 실제 게임 증분이 검증되었다는 표시는 아니다. Raw HTML은 `data-pipeline/captures/quality-sanctification-c-20261103`에 ignored 상태로 보존했다. 추가 이미지 수집은 하지 않았다.

## 검증과 범위

Frontend 관련 테스트·typecheck·lint·format·build와 no-socket Java 21 unit/ArchUnit/format/bootJar를 실행했다. 최초 축성 검사는 `docs/evidence/quality-sanctification-c-20261103/verification.json`, 사용자 +1 정책 후속 검사는 `docs/evidence/quality-plus-one-c-20261103/verification.json`에 기록한다. 후보 조립 스크립트는 `prepare-verification.py`, Java 격리 명령은 `verify-no-socket.sh`다. 후보 검증은 root의 최종 통합 전체 검사와 다르다.

브라우저 자동 QA, 서버 시작/변경, remote push, DB/Redis/volume/54개 저장파일/운영 설정 수정은 하지 않았다. Docker host socket은 연결하지 않았다. DB integration 및 generateJooq는 이전 소켓 승인 차단과 이번 명시적 no-socket 범위 때문에 미실행이다. Frontend 전체 테스트는 root가 통합 후 실행한다. 자체 리뷰이며 독립 리뷰로 표현하지 않는다.
