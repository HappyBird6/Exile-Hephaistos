# Catalog 데이터 유지보수

2026-10-09 · 기존 동작을 보존한 데이터 분리

## 데이터 출처와 경계

- `backend/src/main/resources/catalog/top-bases.json`: 117개 확장 base의 key/ID/pool/원문 property/출처. 기존 17개 base는 `base-policies.json`의 `legacy`에 있다. 총 Workbench 134개다.
- `catalog/base-policies.json`: family의 검증된 capability와 개별 override. 알 수 없는 base/class에 capability를 상속하지 않는다. `BaseRegistry`가 key 불일치·중복 ID·누락 capability·정책 오타·orphan override를 거부한다.
- `catalog/<pool>/catalog.json` 및 special snapshot: modifier ID, stat range, tier, tag, weight, source. 이미 JSON이며 이번 작업에서 이동하거나 게임 값을 바꾸지 않았다. `ItemCatalogLoader`와 `ItemCatalog`가 출처 digest, implicit 일치, modifier 중복, prefix/suffix 개수·weight 합을 검증한다.
- `crafting/goalfilter/definitions-v1.json`: 14개 검토된 직접 stat의 label/unit, 6개 pseudo의 직접 source 참조. `GoalDefinitions`는 불변 타입이며 `GoalDefinitionsLoader`가 strict JSON으로 읽는다. 미검토 raw stat은 계속 `source` 단위·미지원이다.
- `crafting/goalfilter/bases-v1.json`: goal catalog의 명시적인 17개 key/pool 범위. Workbench 134개 지원과 별개다. pool의 base ID와 실제 Workbench 초기 상태 ID가 같아야 한다. 여기 등록해도 Solar 외 numeric probability나 임의 텍스트 매핑을 지원하지 않는다.
- `crafting/registry-v2.json`: 220개 재료 inventory와 ACTIVE/DEFERRED 상태. 반복되는 base whitelist는 `crafting/supported-base-sets-v1.json`의 34개 set을 참조한다. group 이름은 최초 재료 ID에서 파생한 안정적인 식별자이며 재료의 실행 조건을 뜻하지 않는다. set은 명시적인 key 목록이고 새 family/base를 자동 등록하지 않는다.

`CraftingRegistryLoader`는 set 참조를 기존 `supportedBases` 배열로 펼쳐 API에 반환한다. 기존 Blessed 목록에 두 번 들어 있던 Elegant/Flexed Crossbow key만 제거했다. 테스트가 이 두 중복을 명시적으로 재구성한 뒤 과거 SHA-256 fixture와 비교하므로 그 외 모든 기존 값과 배열 순서를 검증한다. registry에 추가되는 `ruleset`은 형식 버전·내부 검토 bundle version·내용 identity·게임 시즌/패치 확인 상태·provenance를 제공한다. 등록은 실행 구현의 증거가 아니다. ACTIVE/DEFERRED 수치, base 참조, 중복 ID, global rule/ledger version, currency action/active omen ID를 검증한다. DEFERRED의 출처 미확인 `sourceSha256:null`은 유지하며 ACTIVE에는 검증된 digest가 필요하다.

## 데이터 수정 예시

source 검토가 끝난 직접 stat을 추가할 때 `definitions-v1.json`의 `stats`에 다음 형태를 추가한다. 예시 ID는 테스트용이고 제품 데이터에 등록하지 않는다.

```json
{"sourceStatId":"test_reviewed_flat","label":"Reviewed Test Stat","unit":"flat"}
```

pseudo에는 같은 단위의 직접 source만 참조한다. 중복 참조와 pseudo-to-pseudo 참조는 금지한다. coefficient는 기존 정책대로 1이며 계산식 DSL을 추가하지 않는다.

```json
{"id":"total_test","label":"Total Test","unit":"flat","sourceStatIds":["test_reviewed_flat","base_maximum_life"]}
```

`GoalDefinitionsLoaderTest`는 catalog modifier에 새 stat을 넣고 JSON 정의만 추가해 label·단위·pseudo contribution이 반영되는지 검증한다. non-Solar probability는 계속 미지원이다. 기존 정의가 정확히 같으면 기존 goal version을 보존하고, 정의의 내용 변경은 자동 digest로 goal version을 변경한다. 이미 저장한 목표는 기존 version 검증 규칙에 따라 다시 검증해야 한다. 호환용 과거 digest를 새 내용에 맞춰 갱신해서는 안 된다.

같은 class base를 추가할 때는 source 검토된 snapshot과 `top-bases.json` 항목을 추가한다. 항목의 `key`와 객체 key를 같게 유지한다. `family`의 기존 policy가 실제로 적용되는지 검토한다. 개별 차이는 `baseOverrides`에 명시한다. `BaseRegistryTest`의 Sourced Spear 예시는 데이터만으로 같은 class 등록을 검증하며 미검토 class는 실패한다. modifier 수치 변경에는 source raw/digest와 metadata 합계도 함께 갱신한다. 원문이나 상태 출처를 지우는 정규화는 하지 않는다.

Frontend의 `topBases.json`, `basePolicies.json`, `topBaseEssences.json`은 독립 출처가 아니라 위 Backend JSON의 mirror다. 수정 후 `node scripts/sync-base-registry.mjs`로 동기화하고 `node scripts/sync-base-registry.mjs --check`로 의미 일치를 확인한다. `top-base-essences.json`은 기존 형식을 유지한 typed `ReviewedEssencesLoader`로 읽는다. root/target map shape, source ID 타입, 중복 JSON key, unknown field, 등록 base 집합을 검증하고 기존 simulator의 modifier 존재·layer·weight 검증은 유지한다.

지원 범위 변경은 해당 whitelist set을 수정한다. 많은 재료가 같은 set을 참조하므로 먼저 참조 항목을 모두 확인하고 영향이 같은지 검토한다. 일부 재료만 다르면 기존 set을 수정하지 말고 별도 set을 만들고 해당 entry만 참조한다. 삭제한 base는 모든 set·goal 범위·override에서 참조를 정리한다. counts와 source 증거 없이 pending 재료를 ACTIVE로 바꾸지 않는다.

## 스키마와 검증

편집기용 명시 JSON Schema는 `contracts/goal-definitions-v1.schema.json`, `goal-bases-v1.schema.json`, `supported-base-sets-v1.schema.json`이다. 런타임의 Java 타입·검증기가 참조 무결성을 추가 검사한다. duplicate JSON field, unknown field, schemaVersion, stat/pseudo ID와 단위를 테스트한다. 알고리즘은 JSON으로 옮기지 않는다.

Backend의 `./gradlew.bat check generateJooq bootJar`로 formatter·unit·ArchUnit·Docker 통합 검사를 실행한다. 변경 중 빠른 확인은 `test --tests '*GoalDefinitionsLoaderTest' --tests '*CraftingRegistryLoaderTest' --tests '*BaseRegistryTest'`다. 전체 회귀에는 기존 125-base runtime parity, goal production-example snapshot, numeric 분포·quality/출처 보존 테스트가 포함된다. Frontend는 AGENTS.md의 npm 검사 전체를 실행한다. schemaVersion은 정수이며 문자열로 바꾸지 않는다.

## 남은 작업

inventory의 일부 `workbenchBases.ruleVersion`은 legacy simulator 계열 version이고 BaseRegistry의 variant wrapper version과 의미가 다르므로 동일성만으로 오류 처리하지 않는다. 이를 별도 이름의 version 필드로 명확히 하는 API 변경은 후속 부채다. film/root/request identity와 legacy 정책도 아직 남는다.

## 화폐와 징조 정의 수정

`crafting/workbench-definitions-v1.json`은 enum 순서의 currency160개와 omen18개를 모두 정의한다. currency의 baseAction, minimumModifierLevel, fixed/choice/replacement target 배열과 source URL, omen의 API ID/trigger/affix/tiered 적용성은 이 파일이 실행 metadata 출처다. 편집기 schema는 `contracts/workbench-definitions-v1.schema.json`이다.

모든 enum ID/key가 기존 순서로 정확히 한 번씩 있어야 한다. 정의 삭제를 지원 중단으로 해석하지 않는다. 기존 ID를 유지한 지원 범위 조정은 inventory의 ACTIVE/DEFERRED와 whitelist 및 실제 구현 경계를 함께 검토한다. 새 실행 ID는 enum·계약·source 검토·필요한 알고리즘 구현을 먼저 요구하며 JSON 행 추가만으로 실행을 부여하지 않는다.

화폐의 level을 바꿀 때는 해당 currency 행의 `minimumModifierLevel`만 수정한다. inventory의 17개 `minimumModifierLevelRef`는 action ID를 가리켜 기존 공개 API 숫자를 자동 생성한다. ref와 직접 숫자를 함께 쓰거나 다른 action을 참조하면 실패한다. target 수정은 fixedModifierId와 단일 essenceModifierIds의 일치 또는 fixed=null인 choice mode, replacement mode를 구분한다. 빈 배열과 null의 의미를 바꾸지 않는다. 선택 배열의 순서를 유지하고 source URL도 검토한다. 현재 liquid/alloy의 replacementEssenceModifiers 예외, catalyst type·refined 분류, liquid source를 통한 catalog 내 target 선택은 기존 타입 안전한 코드다.

`WorkbenchDefinitionsLoader`는 strict typed JSON과 enum 전체 coverage/order, 중복/미지 identity, target mode, level과 operation 경계, source URL, 미구현 omen 적용성을 검사한다. 실제 소비하는 catalog 정의에 target ID가 있어야 하고 basic essence는 양의 ordinary explicit 후보, replacement는 explicit 후보여야 한다. 같은 ID가 여러 base catalog에서 서로 다른 source range/weight를 가질 수 있으므로 전역 modifier로 합치거나 값을 보정하지 않는다. catalog별 실제 적용성·override 검증은 기존 simulator가 유지한다. Omen의 sideWhittling/sideDoubleRemoval/verifiedDoubleAddition과 homogenising 조합은 Java에 남고 미검토 조합은 확장하지 않는다.

bootstrap과 inventory loader는 immutable metadata를 명시적으로 초기화한다. domain은 resource를 읽지 않는다. 한 JVM은 한 active metadata bundle을 사용하고 서로 다른 정의로 교체하려 하면 실패한다. 새 bundle은 새 앱 시작에서 적용하며 hot reload나 과거 시즌 엔진을 제공하지 않는다. 테스트 JVM도 JUnit extension에서 같은 명시적 초기화를 수행한다.

`WorkbenchDefinitionsLoaderTest`의 fixture는 분리 전 compile된 enum에서 모든 getter를 실행해 수집한 독립 oracle다. SHA `8d050b94aeb67a475c24eb767a0700f1c6441d169d6d697c290013cfc5b15d93`를 고정하고 target 배열을 정렬하지 않는다. getter 전체, null/empty, 예외, enum/API ID와 순서를 비교한다. 실제 규칙 변경에서는 원본 oracle를 일괄 갱신하지 말고 검토된 변경 항목과 그 외 보존 항목을 분리해 회귀 근거를 작성한다.

## 시즌과 패치 업데이트

`crafting/ruleset-v1.json`의 `schemaVersion`은 파일 형식이고 `rulesetVersion`은 검토된 내부 데이터 bundle 이름이다. 현재 `reviewed-equipment-20261009-v2`은 GGG 시즌 이름이 아니다. `gameSeason`과 `gamePatch`는 원문 source 자료와 실제 게임 release의 연결이 확인되지 않아 `UNVERIFIED`다. 확정되지 않은 시즌 이름이나 패치 번호를 채우지 않는다.

manifest는 catalog 데이터와 crafting/goal/화폐·징조 지원 정의 462개 resource의 정확한 bytes SHA-256을 선언한다. runtime은 실제 resource의 존재와 digest, engine ruleVersion와 assumption ledgerVersion을 검사한다. 게임 데이터가 바뀌었는데 manifest가 그대로인 bundle은 앱 초기화에 실패한다. identity는 rulesetVersion·시즌/패치 표기·engine/ledger version·정렬한 resource digest에서 계산한다. 내용 identity는 addition pool cache namespace와 production goal catalogVersion에 반영된다. 기존 내용의 고정 compatibility digest만 현재 goal version을 보존한다. 새 시즌에서 이를 임의 갱신해 과거 목표를 다시 유효하게 만들지 않는다.

manifest에서 소비 파일을 빼는 것도 실패해야 한다. `SealedResources.requiredPaths()`는 loader의 고정 의존성과 `top-bases.json`이 선택하는 pool의 catalog/base.raw/details.raw를 합친다. 현재 440개 소비 의존성이 있으며 전체 폴더를 탐색하지 않는다. manifest에는 이 집합 외 기존 보관 자료도 계속 포함한다. 모든 현재 bundled consumer는 `SealedResources.open()`을 통해 실제 읽는 경로의 manifest 등록과 정확한 bytes digest를 다시 확인한다. 새 loader의 동적 경로가 고정 의존성 목록에 아직 추가되지 않았어도 미등록 파일을 조용히 사용할 수 없다. `ArchitectureTest.bundledConsumersCannotBypassRulesetSeals`는 item/crafting/bootstrap에서 직접 Class resource 접근으로 이 경계를 우회하는 것을 금지한다. 새 고정 loader 의존성은 FIXED 목록에도 추가해 bootstrap 이전 manifest 검사와 편집 가이드를 유지한다. 새 pool은 registry의 명시적 pool 선택으로 자동 검사한다. 검증기의 bootstrap 독립성을 위해 resource seal helper는 특정 domain이나 crafting infrastructure를 참조하지 않는다.

`RulesetManifestLoaderTest`는 모든 현재 소비 resource를 하나씩 manifest에서 제거했을 때 실패하는지, 새 registry pool의 미등록 catalog와 새 소비 경로가 실패하는지 검사한다. 기존 461개 개수만 확인하는 검사는 coverage 증거로 충분하지 않다. registry entry의 필드는 명시적 whitelist이며 잘못된 `supportedBaseSett` 같은 키를 거부한다. 의도된 scope가 없는 기존 Homogenising 징조 계약은 유지한다. summary의 registered/active/deferred는 문자열·소수·null을 정수로 강제변환하지 않는다. 공통 typed JSON mapper의 `ACCEPT_FLOAT_AS_INT`도 꺼져 있으므로 schemaVersion 1.5를 1로 해석하지 않는다.

업데이트 순서는 source 검토 → 범위/capability와 snapshot 수정 → engine/ledger 의미 version 검토 → 새 rulesetVersion 지정 → `node scripts/update-ruleset-manifest.mjs` → manifest diff 리뷰 → strict loader·order-sensitive·전체 검증이다. 이 스크립트는 파일 digest만 갱신하며 게임 효과나 version/provenance를 결정하지 않는다. 알고리즘이 바뀌면 해당 engine ruleVersion을 반드시 올린다. 과거 snapshot을 `compatibleSnapshotIds`에 추가하는 것은 모든 기존 modifier/state 의미가 보존된 additive extension임을 증명한 경우만 허용한다. target 배열과 extension 로드 순서를 정렬하지 않는다. `CatalogLoadOrderTest`와 `ReviewedEssencesLoaderTest`가 각각 Solar/Stocky extension·compatibleSnapshotIds·modifier 순서와 모든 Essence target 배열 순서를 별도로 검사한다.

현재 film은 snapshot과 개별 craft의 ruleVersion/ledgerVersion을 기록하지만 최초 root를 포함한 film 전체에 ruleset identity를 갖고 있지 않다. 이 작업에서 과거 film을 새 시즌으로 자동 이관하거나 삭제하지 않는다. 미래 시즌 실행 지원을 열기 전에는 film/request ruleset ID, 현재 ruleset과의 mismatch 거부, 명시적인 legacy 호환/읽기 전용 정책을 추가해야 한다. manifest 재봉인만으로 과거 film 실행의 시즌 호환성을 보증하지 않는다. 기존 ItemState snapshot 검증·compatibleSnapshotIds 정책은 유지했다. 모든 과거 시즌 엔진이나 새 경로 탐색 서비스는 구현하지 않았다.

화폐·징조 분리 bundle v2는 분리 전 getter/배열 값 그대로지만 추가 resource와 registry level ref를 identity에 반영한다. goal/catalog/cache identity도 변한다. 과거 호환 digest를 v2 값으로 갱신하지 않으며 기존 목표는 현재 catalogVersion으로 재검증해야 한다. 과거 film을 새 ruleset으로 자동 재라벨링하지 않는다. film/root/request의 ruleset mismatch와 버전 없는 legacy 기록 분류/읽기 전용 정책은 아직 구현되지 않았다. 실제 다음 시즌의 동작 변경을 적용하기 전에 이 경계를 완료해야 한다.

후속 구현은 `workbenchHistory.ts`의 version1 저장 형식과 `CraftingPage.tsx`의 load/save 경계, `workbenchApi.ts`/`craftingApi.ts`, `WorkbenchController`와 기존 `CraftingController` 요청 경계에서 시작한다. 보수적인 정책 후보는 current/mismatch/legacy-unverified/inconsistent를 읽을 때 분류하되 저장 bytes를 변경하지 않는 것이다. ruleset identity는 물리적 ItemState 밖에 film/root/result/request로 전달하고, 누락·불일치는 실행뿐 아니라 actions/quality/goal 해석에서도 거부한다. 기존 snapshot 호환 업그레이드를 ruleset 재라벨링에 사용하지 않는다. 버전 없는 기록의 ruleVersion/ledgerVersion만으로 시즌을 추정하지 않는다. 현재 규칙으로 재사용을 제공한다면 명시적 새 film 생성과 현재 state 검증을 거쳐 원본 film/evidence를 보존한다. 이 정책은 다음 묶음에서 확정·구현·검증할 후보이며 현재 앱의 보장 사항이 아니다.
