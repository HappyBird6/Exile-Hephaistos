# Base registry 구조 정리

기준 branch `workbench/top-bases-20261004`, HEAD `e80d63fca33b9702b192cf23970b03fb8891eed5`. 이 작업은 기존 **125 bases**(확장 catalog 108 + legacy 17)를 유지한다. Staves6/Talismans3는 검증된 구조 정리 checkpoint 다음 작업에서 추가한다.

## 설계와 경계

`Weapon → Mace` interface 상속을 만들지 않는다. `ItemCatalog`와 `ModifierDefinition`을 `CraftingEngine`, `ModifierPoolResolver`, `AdditionRules`, `WorkbenchSimulator`에 주입하는 composition을 유지한다. 확률 계산, 후보 선택, tier/family/tag 규칙은 변경하지 않는다.

`catalog/top-bases.json`은 확장 base ID·pool·source property·requirements·implicit의 canonical manifest이다. `catalog/base-policies.json`은 class별 검증 capability·rule/ledger version·표시 class·snapshot 정책과 legacy 예외의 canonical manifest이다. `BaseRegistry`가 두 데이터를 검증하여 등록한다. 알 수 없는 family, 누락 capability, 중복 ID는 거절한다. 불명확한 path를 이름이나 metadata 문자열로 추측하지 않는다.

`Reviewed*` 15개 wrapper는 기존 테스트/호출 호환을 유지하되 ID 목록을 독립 저장하지 않는다. 공통 등록은 이 wrapper들의 목록을 나열하지 않는다. Soldier/Imperial은 기존 source pool·special loader·snapshot ID를 보존하는 `legacyCatalog` 정책 예외이다. legacy 17개 base의 loader, 기본 Jewel/Liquid mechanics, Solar clipboard mapping과 Support/Explorer는 현재 전용 구현을 유지한다.

class 정책의 snapshot 날짜·version은 기존 저장 이력의 호환 identity를 보존한다. 실제 source 수집 시각은 raw source/provenance evidence에 별도로 유지된다. 새 source 날짜가 다른 같은 class base에는 필요하면 `baseOverrides`에서 snapshot 정책을 데이터로 지정한다. 기존 ID의 날짜 표기를 일괄 보정하지 않는다. `registry-v2.json`의 base별 material/provenance 목록도 보존하며 importer가 새 base의 source 검증 결과로 확장한다.

Frontend의 `WorkbenchBaseKey`, 초기 텍스트, source slug, 표시 class와 selector 확장 목록은 데이터에서 파생한다. Backend canonical manifest를 수정한 뒤 `node scripts/sync-base-registry.mjs`로 Frontend mirror를 생성한다. `--check`는 내용 동등성을 검사한다. 기존 6곳의 수동 base union은 제거한다. i18n modifier templates·gameTerms·UI strings는 변경하지 않는다.

Catalyst quality 상태 허용과 ordinary/refined Catalyst 적용은 별도 capability다. ordinary는 기존 Ring/Amulet만, refined는 기존 Basic Jewel만 허용한다. Artificer와 supplied socket 상태는 기존 Stocky Mitts의 0/1 empty sockets만 허용한다. 다른 장비의 source socket maximum은 표시 데이터이며 실행 capability가 아니다. class 정책의 socket 실행 maximum은 null을 기본으로 하고 현재 구현은 검증된 1만 허용한다. 일반 장비 socket 실행을 지원한다고 주장하지 않는다.

quality cap20과 기존 Breach cap40·overflow·HALF_UP, Divine/Omen·Essence 후보·full pools·films·snapshot 호환을 보존한다. class policy는 지원되지 않은 mechanics를 구현하는 수단이 아니다. combat·shapeshift 동작·excluded/deferred50 범위는 확대하지 않는다.

## Importer 범위

최근 Maces/Quarterstaves/Spears importer에서 동일했던 ordered spawn eligibility와 6locale numeric display binding을 `scripts/reviewed-catalog-importer.mjs`로 추출했다. source 저장·exact roster·class tags·special/implicit 검증은 bundle importer에 남는다. 모든 역사 importer를 하나의 범용 프로그램으로 바꾼 것은 아니다. 역사 migration 준비 script는 재실행용 importer가 아니다.

## 다음 9종 추가 recipe

1. 별도 source evidence directory에 ordinary roster·released availability·정확한 endgame metadata·skill/implicit·6locale 자료를 수집한다. 기존 evidence를 덮어쓰지 않는다.
2. Staves와 Talismans의 source-specific full eligible pools·ordered spawn tags·families·ranges·weights·Essence/Omen restrictions를 검증한다. Quarterstaves와 Staves를 합치지 않는다. missing weights는 승인된 1/N valid candidates로 명시한다.
3. **새 class당 `base-policies.json`의 family 정책 한 곳**에 검증된 class label·capability·version을 추가한다. source properties/requirements/skills와 implicit 초기 상태는 해당 catalog 데이터에 넣는다. socket capability는 null 유지한다.
4. 각 base의 raw/details/catalog, `top-bases.json`, `top-base-essences.json`, registry source/provenance·supported material evidence, 6locale gameTerms/modifierTemplates를 생성한다. 같은 class의 추가 base는 기존 policy를 재사용하며 Java ID map·Frontend union·slug map·class allowlist 편집이 필요 없다.
5. `sync-base-registry.mjs`와 `--check`, importer provenance/pool checks, 직접 영향 및 전체 필수 검증을 실행한다. 기존125 IDs/films와 새9 positive/negative paths·저ilvl·implicit/skill·6locale screenshots를 확인한다. 새로운 mechanics에 공통 engine이 충분하지 않으면 그 부분은 근거를 명시하고 지원을 거절한다.

## 완료 checklist

- [x] 기존 125 initial/API schema·full pool·snapshot·currency availability 동등
- [x] 기존125 × ilvl1/20/82 × 고정 seed trace 동등
- [x] 같은 class의 test fixture 등록은 데이터 추가만으로 성공, 새 class는 명시 policy 없으면 거절
- [x] 과거 Reviewed maps·125 initial texts/slugs·6locale display 보존
- [x] importer ordinary rows의 이전 binder output 재현
- [x] Backend `check generateJooq bootJar`, Frontend 필수 검사·build
- [x] aggregate API·representative browser/films·6locale visual review
- [x] live services/mounts/storage·DB volumes 보존, 작업 전용 QA만 종료
- [x] 자체 리뷰·local clean commit, remote push/merge/deploy 없음

## 검증 결과와 인계

Backend unit492 + integration6, Frontend1917, API2902 + registry582, browser1088 + 강화 film481 + negative Jewel17, importer3 tests/620 rows가 통과했다. 기존 catalog/i18n 등441파일은 시작 HEAD와 byte-identical이고 runtime/build 입력764개를 대조했다. 정확한 등록 수는 **125**, 이번 신규 base는 **0**이다. 상세 결과와 실패 보존·샘플 수정 경위는 `docs/evidence/base-registry-refactor-2026-10-05/completion.json`에 있다.

Film 첫 샘플은 Jewel에 미지원 Fractured를 넣었으므로 화면이 거부되었다. 기존 validator가 올바르게 거부한 것으로 확인했다. 저장값 보존만 검사하던 probe를 보강해24대표 ×6locale의 실제 옵션 표시와 alert 부재를 확인했다. 별도 negative probe는 거부 화면과 원본 film 보존을 검증했다. Runtime 규칙을 완화하지 않았다. 스크린샷은 전부 생성했고 시각 검토는 명시한 대표 contact sheets와 원본에 한정한다.

전용 QA4서비스만 정상 stop했고 live 서비스 ID·시작시각·mount가 동일하다. QA slot은 해제했다. 원본 stack의 `.env`가 worktree/parent에 없어 해당 Compose 검사는 실행하지 않았다. infra 변경은 없으며 전용 QA Compose config는 통과했다.

다음 독립 작업은 Staves6/Talismans3의 source roster·released availability와 풀·skill/implicit·6locale 증거를 수집하여 위 recipe로 추가한다. 신규 class policy는각1곳, canonical/mirror 동기화는1script로 처리한다. 그 뒤134 IDs와 기존125 snapshot/films 보존, 신규9의 ilvl1/20/82·currency/Essence/Omen positive/negative·skill/implicit6locale를 검증하고 전체 필수 checks를1회 실행한다. 마지막 coverage audit은 선택 roster 완결, 실제 released class 누락, representative sidegrade 의도적 생략, 실제 미지원 mechanics를 구분한다. 현재 checkpoint는 Staves/Talismans 완료나 게임 전체 지원을 주장하지 않는다.
