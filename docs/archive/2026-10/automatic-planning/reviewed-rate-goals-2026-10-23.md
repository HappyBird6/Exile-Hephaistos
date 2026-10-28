# 재생·흡수율 목표 환산 — 2026-10-23

앞선 [속성 판정 복구](unsupported-attributes-2026-10-23.md)의 남은 세 항목을 조사하고
지원했다. 일반 경로의 자원 한도나 대형 결과 내보내기 한도는 변경하지 않았다.
기존 작업 브랜치 `codex/automatic-refinements-20261021`, HEAD `84b94438b6c2f56f5688a17635e1d020aa35b482`
이후 미커밋 변경에 이어 작업했다. 원격 push 없음.

## 근거와 환산

| raw stat ID | 표시 의미 | 검증한 환산 | 반지 원본 행 |
|---|---|---|---|
| `base_life_regeneration_rate_per_minute` | 초당 생명력 재생 | raw/60, flat | LifeRegeneration1…7, 7행 |
| `base_life_leech_from_physical_attack_damage_permyriad` | 물리 공격 피해의 생명력 흡수율 | raw/100, percent | LifeLeech2…3, 2행 |
| `base_mana_leech_from_physical_attack_damage_permyriad` | 물리 공격 피해의 마나 흡수율 | raw/100, percent | ManaLeech2…3, 2행 |

단위 이름만으로 추정하지 않았다. `catalog/iron-ring/catalog.json`과 `details.raw.json`의
11개 원본 행, 기존 `scripts/bind-source-unit-display.mjs`와
`docs/evidence/i18n-source-unit-display-2026-10-04.json`의 검증된 endpoint 매핑을 확인했다.
예를 들어 재생60…120↔초당1…2, 생명력 흡수600…690↔6…6.9%,
마나 흡수500…590↔5…5.9%이다. 다른 티어의 두 endpoint도 같은 환산과 일치한다.

GGG 추출 데이터를 담은 [PoB2 stat descriptions](https://github.com/PathOfBuildingCommunity/PathOfBuilding-PoE2/blob/bb52d6b368307457eb9c54bb13f1829993d390b1/src/Data/StatDescriptions/stat_descriptions.lua#L21055)
commit `bb52d6b368307457eb9c54bb13f1829993d390b1`도 별도로 확인했다.
이 파일은 GGG가 직접 제공하는 공식 API가 아니라 GGG 데이터의 커뮤니티 추출본이다.
재생 행은 `per_minute_to_per_second`, 두 흡수율 행은 `divide_by_one_hundred` 변환을
정확한 raw ID와 연결한다(재생21055…21075, 생명력21151…21169, 마나21352…21370 부근).
전체 추출 파일 SHA256은 `51ca4503c1c4b1b8b8bde2f75b68a859f33c7663b9ccdbb89d2b77db5589af6c`.
PoE2DB hover URL은 웹 도구에서 직접 열리지 않았으므로 새로 조회했다고 주장하지 않는다.
저장된 원자료와 고정된 추출본을 교차 확인한 결과다.

이 근거는 **표시·목표 단위의 환산**을 입증한다. 게임 내 내부 수치 증가폭·균등성·
롤 분포를 새로 입증하지 않는다. 기존 Workbench의 raw 정수 샘플링 가정과 공통 비율
모델은 그대로이며 검증된 게임 확률로 재분류하지 않는다.

## 변경

- goal 정의40개(이전37개)에 검토된 `sourceDivisor` 60/100을 명시했다. 기존 정의의
  생략값은1이며 엄격한 JSON 검사를 유지한다. raw 아이템 값은 변경하지 않는다.
- `GoalCatalogIndex.Mapping`의 기존 분수 계약과 `RollPartition`의 raw 경계 환산을
  연결했다. 숫자 목표를 raw 정수에 되돌릴 때 min은 CEILING, max는 FLOOR로 구분한다.
- `GoalNumber`는 분자/분모를 보존한다. 일반 비교·pseudo 합계·가중 그룹 모두 소수
  근삿값이 아닌 교차 곱으로 판정한다. 유한소수는 정확히 출력하고 순환소수는
  `exactValue`/`exactScore`와 DECIMAL128 근삿값을 함께 제공한다. UI는 `91/60 ≈ …`로
  표시한다. 이 소수 표시를 게임의 반올림 규칙으로 해석하면 안 된다.
- 실제 반지 catalog **43/43 evaluation 지원**. 일반 생성 자격, 원본 weight, 티어,
  기존 에센스/기폭제 규칙 및 raw 값은 바꾸지 않았다. 모든 장비의 모든 속성 지원이나
  모든 자동 탐색 경로의 완결을 의미하지 않는다.
- kernel은 `automatic-finite-kernel-v7-reviewed-source-units`. 기존37개 정의를
  `definitions-v4-readonly.json`에 정확히 동결했다(SHA256
  `2eaf293caf99cd63415e4e52badd98cee3e2730d72c2fc4130ebcaa6454d6e36`).
  v3/v4 목표 버전과 이전 ruleset의 결과는 재해석·재개하지 않고 읽기 전용으로 제공한다.
  리소스 봉인 목록465개. 현재 catalog 예제는 원래 아이템·목표·수치22·MATCH를 유지하고
  새 카탈로그 능력/단위 메타데이터만 다시 생성했다.

구형 v6 실데이터 검증 중 미완료 실행의 화면용 1회/100회 관찰이 저장 복원에서
누락됨을 발견했다. `SavedAutomaticResultReader`가 저장된 cursor accumulator를
검증하여 같은 보수적 확률·사용량 구간을 복원하게 했다. 전이·분류 callback은 호출되면
예외를 던지므로 새 계산을 실행할 수 없다. 임시 관찰을 완료로 표시하지 않으며 alias의
조건부 source도 동일하게 운반한다. 원본 저장 파일에 쓰지 않는다.

핵심 변경 파일은 `GoalDefinitions`, `GoalCatalog`, `GoalCatalogIndex`, `GoalNumber`,
`GoalEvaluator`, `ItemStatProjection`, `GoalDefinitionsLoader`, `SupportConfiguration`,
`AutomaticSearchService`, `SavedAutomaticResultReader`, `AutomaticCraftingKernel`,
`SealedResources`, goal 정의/봉인 JSON 및 `ConnectedGoalFilter`, `evaluationNumber`,
`numericApi`, 관련 테스트다. 기존 티어 분포/그래프 이동/줌 변경을 보존했다.

## 검증 기록

- 환산·구형 호환 집중 검사: 최종52개 통과. 순환소수의 양쪽 경계, 두 weighted 모드,
  pseudo 정확한 합계, 유한소수 흡수율, 구형 정의 생략값 및 진행 중 v12 관찰을 포함한다.
- 프런트2351/2351, lint/typecheck/format/build 통과. JS `index-ZyXGGbNF.js`.
- 실제 격리 API: 경계/가중 판정11개 및 직접 엑잘 확률 오라클3개 통과(23요청).
  43개 catalog가 모두 SUPPORTED이며 판정 미지원 항목은 없다.
- ilvl82 빈 희귀 반지, 우측 찬미 엑잘의 목표 수치:
  재생1.51…1.52는 raw91 한 값이므로 `200/1062681`,
  생명력6.01%와 마나5.01%는 각각 `200/1585311`.
  원본 weight와 기존 raw 정수 균등 모델을 별도로 합산한 확률을 API 구간이 포함한다.
  실제 게임 확률이라는 뜻은 아니다. 이 테스트의 계산은104연산에서 완결되었고 검증
  후 자체 job만 취소했다. 큰 경로의 성능 문제와 구분한다.
- Chrome: 초당 재생91/60의 분수 표시 및1.51…1.52 일치,
  소수 근삿값과 같다는 이유로 동등 판정하지 않는 경계,
  생명력6.01%와 마나5.01% 일치를 실제 API와 함께 확인했다.
- 최초 집중 검사에서 optional 환산 필드의 엄격 파서 호환을 보완했고, 다음 검사에서
  오래된 catalog 예제만 남아 실제 서비스로 갱신했다. 진행 중 저장 복원 회귀에서
  임시 관찰이 SINGLE_PASS/완료로 잘못 표시될 수 있음도 수정했다. 기대값 완화나
  테스트 삭제로 통과시키지 않았다.

최종 백엔드 전체 검사(`backend-full-final`)는 **904개 중903 통과, 기존06 한 개 실패**,
오류·skip0개다. 통합8/8, spotless/generateJooq/bootJar는 통과했다. 전체 Gradle 종료코드1은
기존 `06-tier-preparation`의 지원 경로 소진 assertion 때문이다. 이를 해결했다고 주장하지 않는다.
최종52개 집중 검사는 전부 통과했다. 프런트2351개 외에도 실제 완료한 새 목표3개의
API 그래프·카드·조건부 확률을 압축 시나리오로 저장/복원하는 추가3개 검사가 통과했다.
원본11개 행의 양끝22개도 실제 API에서 각각 정확한 단위로 MATCH를 확인했다.

실제 구형 v6 결과1개와 v5 결과3개는 읽기 전용 복원에서 노드 분포·방법 확률·모델
정보·관찰 회차가 보존됨을 확인했다. `joint` 출처와 조건부 질량 행은 순서에 의미가 없는
다중집합으로 비교했다(중복 수는 보존). 화폐 순서·회차·확률·수치 조합을 정렬로 숨기지 않았다.

최종 검증 source fingerprint는
`9ba68ec8ad222cb51265dfde867d6f7b25622ff634742471be530fda289209c2`다.
검증 JAR와 이미지 JAR 내부파일1359개, 프런트 정적 파일227개가 모두 일치한다.
ruleset은 `reviewed-equipment-20261009-v2-c303fc1ea724ddf2ccca61f7c9512e6c9fdcd997cb9d4348fe5a5f8766f07d5a`.
app image `sha256:e9dc1c18bead35921fdb8f847feb48567546bbbedcd4214d0b1e05431d50f326`,
frontend image `sha256:c3fa82d3bf40912ede1db6268e8866556fec13faa90f75bdbecfdbb4ef76109b`.
실행 JAR SHA256은 `62b1c363be5f5568870cdded5df4a4dc6fc3910c2eaacc9e9348d9d54329c925`.

## 증거와 보존

최종 QA 반영 완료: http://localhost:18081 (frontend), http://localhost:18080 (backend).
교체 후 HTTP/readiness/정적 자산9개 검사와 세 속성의 경계·가중 API14요청이 통과했다.
실제 Chrome에서도 분수 표시와 재생의 일치/엄격한 소수 경계를 다시 확인했다.
기존 저장29개(사용자28개와 이전 자체 QA 계산1개)는 바이트 동일하며 추가·삭제가 없다.
PostgreSQL·Redis 컨테이너ID/마운트와 설정 파일 해시가 그대로이고8081은 닫혀 있다.
QA 네 컨테이너는 가동 상태로 둔다. 격리 미리보기와 자체 Chrome만 정상 종료한다.

최종 `git diff --check` 통과. 위 실행 fingerprint에 해당하는 변경은 여전히 미커밋·미푸시다.
남은 별도 이슈는 기존06의 탐색 완결, 복잡한 목표의 자원 예산 및 큰 결과의 내보내기다.
이번 세 속성 지원의 미지원 항목이나 추가 승인 차단은 없다. 게임 내 내부 롤 분포의
검증 부재는 그대로 명시한다.

Git 밖 `../evidence/rate-attributes-qa/`에 원자료 고정 SHA와 행, XML, API JSON,
Chrome PNG/JSON, 검토한 서식 patch 및 source/build proof를 보존한다.
쿠키·실제 계산 저장 파일·JAR·빌드 산출물은 소스나 원격에 포함하지 않는다.
QA18080/18081만 교체하며 기존8081은 계속 종료한다. DB/Redis/볼륨·환경 설정·기존
계산 기록을 보존한다. 원격 push는 수행하지 않는다.
