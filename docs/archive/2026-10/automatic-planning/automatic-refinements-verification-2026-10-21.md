# 자동 제작 변경 검증 — 2026-10-21

상태: 운영 적용 전 검증 실패 3개가 남아 있다. 조건부 상태 표현의 큰 확장은 사용자 답변을 기다리며 착수하지 않았다. 원래 완료 단언과 제품 자원 한도는 유지했다.

## 검증 소스와 실행 환경

- 작업 트리: `C:\SSAFY\PYJ\Codex\2026-10-21\task\exile-refinements`
- 브랜치: `codex/automatic-refinements-20261021`
- 기준 HEAD: `40c4a2d59600b773a5b841f6a4a0ac5f1df982d3`; 변경은 미커밋이다.
- 변경 코드·계약·테스트 53개 파일의 SHA-256 목록을 정렬해 계산한 소스 지문: `d4112fa3d3c5ecce33f2b5675ac64f0f724ad4cb55b24242c2d949eca513e073`.
- QA ruleset: `reviewed-equipment-20261009-v2-68910c93fedbba8263972e2c97605e96271d6cf73fa6186088d3c9b76903cf1a`.
- QA UI/API: `http://localhost:18081` / `http://localhost:18080`. 최종 소스로 재빌드한 전용 이미지와 DB·Redis·저장 볼륨을 사용한다.
- QA 앱 이미지: `sha256:22d4823725710b1b06864807804bf53ba3d92cc6c91091b68ea4897a5f1efec5`.
- QA 프런트 이미지: `sha256:d412f66aeafebc7f67e7e8182f7b715ee21b0c9c691e53140c8ed278c99df75e`.

## 전체 검사

| 검사 | 최종 결과 |
| --- | --- |
| Docker `gradlew --no-daemon --continue check generateJooq bootJar` | 종료 코드 1; 단위 테스트 실패로 전체 `check` 성공 아님 |
| 단위·ArchUnit 등 `test` | 879개 중 876개 통과, 아래 제품 시나리오 3개 실패, 오류·건너뜀 0 |
| Docker 통합 검사 | 8개 모두 통과 |
| Spotless | 통과 |
| 임시 PostgreSQL 마이그레이션·jOOQ 생성 | 기존 마이그레이션 3개 적용 및 생성 통과 |
| `bootJar` | 통과 |
| 프런트 `npm ci`, lint, typecheck, format, 표시 자료 검사, build | 통과 |
| 프런트 전체 Vitest | 2,282개 모두 통과; `--maxWorkers=2`, 기존 5초 테스트 제한 유지 |

단위 검사 중 카오스·엑잘 회귀 8개, 퀄리티 목표 6개, 기존 퀄리티 작업 2개, 구형 결과 읽기 5개, 정책 목록 4개, 관측 지평 검사 2개가 통과했다. 실패를 삭제하거나 완료 기대값을 자원 중단으로 바꾸지 않았다.

## 세 실패의 의미와 정상 제품 흐름

고정 테스트는 `GoalDirectedPlannerScenariosTest.graph()`에서 64 상태 / 128 방법 / 20,000 보존 레코드 / 5,000 primitive 결과로 시작한다. `exhaust()`는 최대 120,000 협력 계산 단계만 실행하며 `increaseLimits()`나 제품 `RESUME`을 호출하지 않는다.

제품은 다르다. `AutomaticMethodSearch.tsx`는 자원 중단이며 `canResume=true`이면 기존 `RESUME` API를 자동 요청한다. `AutomaticSearchService.control()`은 원래부터 상태 128 / 방법 256 / 보존 레코드 50,000 / primitive 결과 10,000까지 허용한다. QA에서는 이 API 흐름만 따랐다. 코드·설정·절대 상한은 변경하지 않았다.

| 시나리오 | 고정 테스트의 실패 | 최종 QA의 기존 제품 흐름 관찰 |
| --- | --- | --- |
| 05-whittling-preserve-life | 120,000 단계에서 전체 탐색 미완료. 자원 한도 중단 아님 | 200회 제어 관찰, 약 66.6초 후 234,571 단계까지 계산 중. 상태 1/64, 방법 2/128, 보존 레코드 1,060/20,000, blocker 없음. 목표 노드 18개. 관찰 횟수 제한으로 종료 후 QA 작업만 취소했으며 실제 완료 시점은 미확인 |
| 06-tier-preparation | 기본 20,000 보존 레코드에서 중단 | 정상 20,000 → 40,000 → 50,000 이어 계산 후 50,026개에서 `PAUSED / RESOURCE_BUDGET`, `canResume=false`, blocker `RETAINED_RECORDS`. 상태 1/128, 방법 4/256, 목표 노드 174개. 전체 계산 미완료 |
| 07-rarity-preparation | 기본 20,000 보존 레코드에서 중단 | 정상 20,000 → 40,000 → 50,000 이어 계산 후 50,056개에서 동일하게 재개 불가. 상태 1/128, 방법 3/256, 목표 노드 174개. 전체 계산 미완료 |

보존 레코드 수가 문턱을 조금 넘는 것은 기존 협력 계산 단계가 끝난 뒤 예산을 검사하는 방식의 관찰값이다. 이 수치는 실제 RAM 측정값이 아니다.

05의 이름은 과거 이름이다. `IndependentPlannerReviewTest`의 실제 입력은 희귀 아이템의 생명력 120 + 냉기 저항 6, 목표는 생명력 120 유지 + 카오스 저항 1 이상이다. 시나리오 입력에 징조가 없으며 `graph(start, target)`은 현재 `AutomaticCurrencyPolicyLoader.load()` 정책을 사용한다. QA의 실제 후보는 우측 찬미 징조를 곁들인 엑잘과 일반 카오스 두 개였고, 보류된 절사 징조는 없었다. 따라서 현재도 유효한 목표이며 절사 사용을 전제로 한 단언이 아니다.

06의 이름에 있는 티어 준비도 현재 실제 후보를 보장하는 이름은 아니다. 현재 QA 후보는 일반 카오스와 엑잘 준비 후 일반 카오스이며, 07은 제왕 준비 후 일반 카오스다. 보류 징조 제거는 의도된 정책 변경이고, 정확한 반복 전이의 전체 계산이 위 경계 안에 끝나지 않는 문제는 별도의 미해결 계산 성능·표현 한계다. 목표를 찾았다는 사실과 전체 분포 완료를 구분한다.

## 기능별 QA

최종 이미지에서 HTTP/API 확인 40개를 통과했다. UI HTML·JS/CSS·`/admin`, 프록시 registry, health/liveness/readiness, provenance를 포함한다.

- 퀄리티 필드가 없는 기존 요청이 수락되며 원래 목표 JSON에 퀄리티를 강제로 저장하지 않는다.
- 이미 알려진 실제 퀄리티 20만 판정하는 목표는 20에 MATCH, 21에는 NO_MATCH다. 퀄리티 41은 422로 거부한다.
- 목표 20에는 별도 `WORK:QUALITY_20` 경로를 확인했다. 목표 노드 발견 직후 관찰을 끝냈으므로 그 QA 작업의 전체 완료 상태를 주장하지 않는다.
- 최소 목표 21·30·40은 에센스로 최대치를 확보한 다음 `WORK:QUALITY_40`을 거쳐 COMPLETE였다. 최대치 속성을 남긴 목표 노드를 확인했다.
- 카오스 예제는 100회 관측까지 COMPLETE, 현재 반복 관측은 1/100만 존재하며 실패 나머지는 대상 노드 없는 `RETRY`다. 추천 지평도 100이다. 1회 약 0.06%, 100회 이내 약 5.75%를 확인했다. 일반적으로 독립 동일 확률을 가정하지 않으며, 별도의 상태 변화 예제와 유리수 oracle 검사도 통과했다.
- 보류 징조 세 개는 자동 후보에 없고 수동 registry 정의에는 남아 있다. 엑잘의 유리한 징조, 접두/접미가 가득 찬 경우의 일반 엑잘, 비교 불가능한 후속 목표 보존은 단위 검사로 확인했다.
- 구형 v8/v9 결과의 읽기 전용 접근과 저장 바이트 보존 검사는 통과했다.

퀄리티 30은 정확히 30이 아닌 최소값이므로 40도 충족한다. 현재 승인 모델의 반지·목걸이 추상 작업을 사용하며, 에센스 1회와 추상 품질 작업 1단위를 구분한다. 작업 1단위를 기폭제 1개로 해석하지 않는다. 실제 기폭제 종류·개수·가격과 속성 배율은 미정이므로 총 실제 제작 비용을 계산했다고 주장하지 않는다. 실행 QA는 Solar Amulet 기준이며 다른 장비에 대한 범위를 확대하지 않았다.

실제 브라우저 시각 검증은 미실시다. 컴퓨터 도구 inventory는 `apps: [], browsers: []`였으며 in-app browser도 사용할 수 없었다. 우회 도구나 환경 설치는 사용하지 않았다. 프런트 DOM 검사와 HTTP 확인을 실제 브라우저 확인으로 표현하지 않는다.

## 보존 및 증거

운영 UI/API 8081/8080은 원래 master `40c4a2d`와 ruleset `99ed6f9e…`로 유지됐다. 운영 컨테이너 ID·이미지, PostgreSQL·Redis, 볼륨과 저장된 계산 결과를 교체하거나 삭제하지 않았다. 원본 저장소는 clean이다. 원본 `.env` SHA-256은 `33C2E0343991D294A322DFA9512AC479B95695B8AF52DEBE8548BD78FCA28F26`으로 같다. 최종 운영·QA UI는 HTTP 200, 양쪽 readiness는 UP이었다. push·운영 교체는 수행하지 않았다.

로컬 증거는 작업 트리 옆 `../evidence/`에 보존했다.

- `source-manifest-final.json`: 기준 SHA와 소스 지문·파일별 해시
- `backend-final-test-results/`, `backend-final-reports/`, `backend-final.log`: 전체 검사
- `frontend-r7-results.xml`: 프런트 최종 전체 검사
- `qa-summary.json`, `qa-chaos.json`, `qa-quality-*.json`: 최종 기본 QA
- `qa-normal-resource-flow.json`, `qa-05-*-final.json`, `qa-06-*-final.json`, `qa-07-*-final.json`: 정상 이어 계산 관찰
- `final-runtime.json`: 운영/QA의 최종 HTTP·readiness·provenance

다음 단계는 사용자 답변에 따라 미해결 다속성 조건부 상태 표현의 범위를 정하는 것이다. 승인 전 큰 구조 확장은 진행하지 않는다.
