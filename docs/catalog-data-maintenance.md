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

`WorkbenchCurrency`의 fixed/replacement modifier ID와 source URL, 일부 minimum level 및 `WorkbenchOmen`의 trigger/affix 정의는 Java에 남는다. 다음 묶음에서는 enum API를 유지하면서 typed 데이터 주입과 전체 enum parity fixture를 먼저 설계해야 한다. 현재 실행 알고리즘의 조건문을 무분별하게 JSON 표현식으로 바꾸지 않는다. inventory의 일부 `workbenchBases.ruleVersion`은 legacy simulator 계열 version이고 BaseRegistry의 variant wrapper version과 의미가 다르므로 동일성만으로 오류 처리하지 않는다. 이를 별도 이름의 version 필드로 명확히 하는 API 변경은 후속 부채다.

## 시즌과 패치 업데이트

`crafting/ruleset-v1.json`의 `schemaVersion`은 파일 형식이고 `rulesetVersion`은 검토된 내부 데이터 bundle 이름이다. 현재 `reviewed-equipment-20261009-v1`은 GGG 시즌 이름이 아니다. `gameSeason`과 `gamePatch`는 원문 source 자료와 실제 게임 release의 연결이 확인되지 않아 `UNVERIFIED`다. 확정되지 않은 시즌 이름이나 패치 번호를 채우지 않는다.

manifest는 catalog 데이터와 crafting/goal 지원 정의 461개 resource의 정확한 bytes SHA-256을 선언한다. runtime은 실제 resource의 존재와 digest, engine ruleVersion와 assumption ledgerVersion을 검사한다. 게임 데이터가 바뀌었는데 manifest가 그대로인 bundle은 앱 초기화에 실패한다. identity는 rulesetVersion·시즌/패치 표기·engine/ledger version·정렬한 resource digest에서 계산한다. 내용 identity는 addition pool cache namespace와 production goal catalogVersion에 반영된다. 기존 내용의 고정 compatibility digest만 현재 goal version을 보존한다. 새 시즌에서 이를 임의 갱신해 과거 목표를 다시 유효하게 만들지 않는다.

manifest에서 소비 파일을 빼는 것도 실패해야 한다. `SealedResources.requiredPaths()`는 loader의 고정 의존성과 `top-bases.json`이 선택하는 pool의 catalog/base.raw/details.raw를 합친다. 현재 439개 소비 의존성이 있으며 전체 폴더를 탐색하지 않는다. manifest에는 이 집합 외 기존 보관 자료도 계속 포함한다. 모든 현재 bundled consumer는 `SealedResources.open()`을 통해 실제 읽는 경로의 manifest 등록과 정확한 bytes digest를 다시 확인한다. 새 loader의 동적 경로가 고정 의존성 목록에 아직 추가되지 않았어도 미등록 파일을 조용히 사용할 수 없다. `ArchitectureTest.bundledConsumersCannotBypassRulesetSeals`는 item/crafting/bootstrap에서 직접 Class resource 접근으로 이 경계를 우회하는 것을 금지한다. 새 고정 loader 의존성은 FIXED 목록에도 추가해 bootstrap 이전 manifest 검사와 편집 가이드를 유지한다. 새 pool은 registry의 명시적 pool 선택으로 자동 검사한다. 검증기의 bootstrap 독립성을 위해 resource seal helper는 특정 domain이나 crafting infrastructure를 참조하지 않는다.

`RulesetManifestLoaderTest`는 모든 현재 소비 resource를 하나씩 manifest에서 제거했을 때 실패하는지, 새 registry pool의 미등록 catalog와 새 소비 경로가 실패하는지 검사한다. 기존 461개 개수만 확인하는 검사는 coverage 증거로 충분하지 않다. registry entry의 필드는 명시적 whitelist이며 잘못된 `supportedBaseSett` 같은 키를 거부한다. 의도된 scope가 없는 기존 Homogenising 징조 계약은 유지한다. summary의 registered/active/deferred는 문자열·소수·null을 정수로 강제변환하지 않는다. 공통 typed JSON mapper의 `ACCEPT_FLOAT_AS_INT`도 꺼져 있으므로 schemaVersion 1.5를 1로 해석하지 않는다.

업데이트 순서는 source 검토 → 범위/capability와 snapshot 수정 → engine/ledger 의미 version 검토 → 새 rulesetVersion 지정 → `node scripts/update-ruleset-manifest.mjs` → manifest diff 리뷰 → strict loader·order-sensitive·전체 검증이다. 이 스크립트는 파일 digest만 갱신하며 게임 효과나 version/provenance를 결정하지 않는다. 알고리즘이 바뀌면 해당 engine ruleVersion을 반드시 올린다. 과거 snapshot을 `compatibleSnapshotIds`에 추가하는 것은 모든 기존 modifier/state 의미가 보존된 additive extension임을 증명한 경우만 허용한다. target 배열과 extension 로드 순서를 정렬하지 않는다. `CatalogLoadOrderTest`와 `ReviewedEssencesLoaderTest`가 각각 Solar/Stocky extension·compatibleSnapshotIds·modifier 순서와 모든 Essence target 배열 순서를 별도로 검사한다.

현재 film은 snapshot과 개별 craft의 ruleVersion/ledgerVersion을 기록하지만 최초 root를 포함한 film 전체에 ruleset identity를 갖고 있지 않다. 이 작업에서 과거 film을 새 시즌으로 자동 이관하거나 삭제하지 않는다. 미래 시즌 실행 지원을 열기 전에는 film/request ruleset ID, 현재 ruleset과의 mismatch 거부, 명시적인 legacy 호환/읽기 전용 정책을 추가해야 한다. manifest 재봉인만으로 과거 film 실행의 시즌 호환성을 보증하지 않는다. 기존 ItemState snapshot 검증·compatibleSnapshotIds 정책은 유지했다. 모든 과거 시즌 엔진이나 새 경로 탐색 서비스는 구현하지 않았다.
