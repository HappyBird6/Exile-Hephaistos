# 제작 경로 Backend v1 구현 인계

기준 계약 commit: `bb8c624c51106b0047676169bb70362ffe04af7f`.
runtime snapshot binding 보완: `b806a0bf8c1c9c043845f9a032a8c2d3fd77cb31` (fast-forward 반영).
구현 branch: `crafting/path-search-backend-v1`.
Frontend와 `contracts/crafting-paths-v1/**`는 변경하지 않았다.

## 구현 범위

`/api/v1/crafting/path-searches`에 create/get/graph/cancel/resume/recoveries를 등록한다.
Solar Rare, nonfractured explicit 한 개, quality 없음, conditions/omens 없음이 계산 범위다.
기존 GoalFilter validator/projector/evaluator로 AND/COUNT(OR=min 1), min/max, reviewed pseudo를 평가한다.
NOT/IF/weighted와 다른 시작 범위는 원문을 보존하며 UNSUPPORTED job으로 반환한다.
현재 데이터가 보장하지 않는 joint roll이나 새 효과의 확률을 만들어 내지 않는다.

고정 여섯 정책은 계약 순서대로 `solar-policy-1`~`solar-policy-6`이다.
정책마다 기존 Workbench plan과 BasicCurrencyTransitions의 전체 후보 사전검사를 사용한다.
renewal cursor는 각 양의 확률 roll에 대해 full item을 구성하고, 다음 제거가 동일한 empty item으로 돌아오며
다음 후보 분포가 동일한지 확인한다. 숫자 목표도 각 concrete roll에서 평가한다.
모든 roll 검사가 끝나기 전에는 renewal 완료를 주장하지 않는다.
증명된 완전 kernel의 CDF는 기존 `ChaosRenewalCalculator`/`FirstHitCalculator` 계산을 재사용한다.
미확정 목표 질량이 섞인 kernel은 hit/unknown을 분리한 exact renewal 전파를 사용한다.
Annulment→Exalted는 phase 두 개이며 empty 중간 상태에서 성공하면 즉시 흡수한다.
UNKNOWN/UNSUPPORTED 판정은 미확정 질량에 흡수하고 실패로 치환하지 않는다.

무한 반복의 100/300/500 first-hit CDF와 eventual proof를 별도로 반환한다.
비교는 여섯 후보군 안에서만 확정되며 모든 관측 결과가 COMPLETE일 때만 확정 순위를 부여한다.
수치 동률은 공동 순위이며 실제로 평가하지 않은 후보를 성공률 0으로 채우지 않는다.
일반 다중 explicit 상태 그래프 탐색이나 전역 최적 정책 탐색을 구현했다고 주장하지 않는다.

## 그래프·복구

상태 ID는 기존 `BasicCurrencyState.canonicalKey()`를 재사용하고 digest 충돌 시 full state 동등성을 검사한다.
execution은 full state/policy/phase를 분리한다. 이 범위의 modifier는 implicit 하나와 explicit 0~1개다.
표시 그래프는 정책당 처음 24개 실제 roll 결과까지만 보관한다.
계산은 나머지 모든 roll을 계속 검사하지만 표시하지 않은 분기를 가짜 node로 만들지 않는다.
따라서 CDF가 COMPLETE여도 표시 expansion은 PARTIAL일 수 있다.
반복 정책의 결정적 Annulment 복귀 edge는 실제 검증된 상태에만 표시한다.

페이지는 48 edges 단위다. 첫 페이지에 참조할 nodes/executions를 제공하고,
expansion 정보는 해당 revision의 모든 edges가 전달된 마지막 페이지에서만 발행한다.
cursor는 job/revision에 묶인 서버 발급 opaque token이며 다른 revision으로 바꾸지 않는다.

복구는 보관된 parent revision에서 NO_MATCH failure execution과 동일 정책의 실제 역방향 도달 경로를 검사한다.
checkpoint가 단순히 같은 job에 있다는 이유로 승인하지 않는다.
failure full state를 새 root로 삼고 full checkpoint 일치 predicate를 사용한다.
복구 결과는 조건부이며 부모의 CDF/순위/graph를 갱신하지 않는다.
empty 실패 상태 등 새 root가 v1 범위 밖이면 UNSUPPORTED이다.

## 실행 수명과 자원

- 프로세스당 single worker, bounded queue 16, job 최대 16, TTL 15분.
- job당 고정 revision 최근 8개, 멱등 mutation 기록 최대 64개. 용량 초과는 명시적인 503이다.
- 실행 generation당 elementary work 50,000개. 중단하면 PAUSED이며 cursor와 누적 질량을 그대로 보관한다.
- 각 cursor는 candidate index/다음 roll/총 weight를, job은 완료 후보와 미완료 후보를 보관한다.
  재개는 미완료 지점부터 이어간다. 앞부분을 처음부터 계산해 더하지 않는다.
- fraction 출력 예산 65,536 bit를 관측점 수로 나눠 적용한다. 관측 수/문자열 Long 상한은 계약을 따른다.
  표현 예산 밖의 결과는 이미 증명한 lower를 보존하는 bound이고 사용 횟수 상한이나 성공률 추정값이 아니다.
  이 hard output limit은 PAUSED/resumable=false와 FRACTION_OUTPUT_LIMIT로 표시한다.
- graph/확률/순위/revision을 job monitor 안에서 원자 발행한다.
  취소가 먼저 commit되면 generation을 바꾸고 이후 기존 worker 발행을 폐기한다.
- 작업 요청과 원문을 로그/DB/Redis에 저장하지 않는다. 새로운 전역 전이 캐시를 만들지 않았다.
  partial cursor는 job 안에만 존재하고 기존 complete transition cache에 넣지 않는다.

익명 브라우저 범위는 `crafting_path_client` HttpOnly/SameSite=Strict cookie로 구분한다.
인증이나 HttpSession은 추가하지 않았다. clientRequestId는 이 범위 안에서만 멱등이다.
같은 origin의 FE fetch는 기본 cookie 전달로 동작한다. 다른 origin을 사용하는 QA client는 cookie jar를 유지해야 한다.
모든 POST에는 기존 `X-Crafting-Ruleset` header가 필요하다.

## 통합 파일

- `bootstrap/SupportConfiguration.java`: PathSearchService Bean 하나와 close lifecycle 등록.
- `presentation/RulesetBoundary.java`: 새 namespace의 기존 ruleset 오류에서 detail만 생략하여 고정 problem schema에 맞춤.
  다른 API의 응답은 유지한다.
- `GoalFilterService.java`: 검증 후 재사용할 concrete-item predicate compiler.
- `BasicCurrencyTransitions.java`: 기존 plan/사전검사에 기반한 정확한 streaming renewal cursor.
- `ChaosRenewalCalculator.java`: 기존 동작을 유지하며 검증된 새 정책에서도 호출할 수 있게 공개 접근만 확장.
- 새 application/domain/presentation의 `pathsearch` 패키지가 job/API/계산을 소유한다.

FE는 현재 Workbench의 full ItemState와 `/basic-paths/provenance`, 같은 context의 goal catalogVersion을 함께 제출한다.
202 snapshot 이후 GET으로 현재 revision을 조회하고 graph.nextCursor를 같은 revision으로 요청한다.
resume는 PAUSED/CANCELLED이면서 resumable=true일 때 새 commandId와 현재 expectedRevision을 사용한다.
고정 revision이 폐기되면 410 REVISION_EXPIRED, job 만료/재시작 후에는 410 JOB_EXPIRED다.

## 공유 fixture 확인 사항

`solar-source-fixture.json`의 startItem은 ordinary snapshot
`poe2db-amulets-base-2026-09-29-a4f439852790`를 고정한다.
실제 `ItemCatalogLoader.loadDefault()`는 검증된 special 확장 snapshot을 반환하고 기존 validator는
ordinary snapshot 입력을 SNAPSHOT_MISMATCH로 거부한다. 후속 계약 `b806a0b`가 별도 runtime 입력의 snapshot binding을 명시했다.
제품 요청을 자동으로 현재 snapshot으로 바꾸지 않았다.
테스트는 ordinary 입력 거부를 확인하고, loader의 compatibleSnapshotIds에 ordinary identity가 있음을 확인한 후
동일한 base/implicit/explicit roll을 갖는 **새 current-snapshot 테스트 아이템**을 명시적으로 구성한다.
이에 대한 새 endpoint 결과와 공유 `650/21107` oracle를 비교한다.
공유 source fixture 자체를 제품 요청으로 그대로 재전송하거나 자동 migration하지 않는다.

## 검증 기록

2026-10-10, Java 21 전용 QA 컨테이너(2 CPU, 2 GiB)에서 `spotlessCheck test bootJar` 통과.
전체 unit/ArchUnit **604 tests, failures 0, skipped 0**이며 새 pathsearch 테스트는 11개다.
검사 실행은 컨테이너 내부 소스에서 2분 30초였다. Windows bind mount에서 직접 실행한 전체 검사는
파일 접근 지연으로 중단하고 내부 소스 방식으로 전부 다시 실행했다.

공유 계약 `verify.mjs`는 정상 17/의도적 실패 6개와 source oracle를 통과했다.
`backend/src/test/js/path-search-contract.cjs`는 실제 endpoint snapshot/graph pages 및 Java DTO round-trip
총 23개 payload를 공유 JSON Schema로 검증하고, 확률 질량과 page expansion outgoing 합을 검사했다.
Node/Ajv와 Gradle 의존성은 기존 설치/cache를 재사용했다.

Solar 실제 numeric predicate의 Chaos 1회 확률은 `650/21107`이고 0/1/2/100/300/500 CDF가
독립 정수 oracle와 일치했다. 여섯 후보 모두 해당 관측점에서 COMPLETE이다.
777 work 단위 pause/resume와 중단 없는 실행의 recommendations/rankings가 정확히 일치한다.
작은 데이터 추가 fixture의 COUNT 검색과 별도 exact checkpoint 복구도 각각 독립 `1/4`, `1/2` oracle와 일치한다.
disabled 구조, 삭제 stat/버전/삭제 modifier, 원본 snapshot 거부, strict HTTP 입력,
멱등·다른 owner 격리·TTL·용량·stale cursor/revision·취소/완료·복구 ancestor를 검사했다.

성능 관측: Solar 연속/재개 두 실행을 포함한 테스트 4,954ms, 표시 full states 51,
executions 153, pages 5, 응답 약 209KB, 최대 관측 분모 7,964 bit.
실제 executor를 제어한 queued-generation 취소 경합의 acknowledgement는 183μs였다.
이는 부하 상황의 최악 지연 보장이 아니며, 전체 JVM heap 또는 최초 cold HTTP 응답 시간을 별도로 측정한 값은 아니다.

전체 `check`의 Docker integrationTest 및 `generateJooq`는 **미실행**이다.
QA 컨테이너에 Docker socket을 노출하는 단계가 자동 승인 검토에서 기존 자원 조작 권한 확대를 이유로 거부됐다.
부모가 사용자에게 별도 승인을 요청한 상태이며 승인 전에는 socket을 연결하지 않는다.
전용 컨테이너의 Testcontainers 임시 PostgreSQL/Redis/Ryuk만 사용할 준비가 되어 있고 기존 서버/DB/volume은 변경하지 않았다.
원격 push 역시 부모가 요청한 묶음 승인 대기 상태다. 이 문서의 완료 범위는 로컬 구현·자체 리뷰·위 독립 검증이다.
