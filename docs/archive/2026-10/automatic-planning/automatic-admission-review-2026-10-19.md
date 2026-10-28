# 활성 계산 슬롯과 결과 보관 분리 — 2026-10-19

## 원인과 변경

기존 JOB_CAPACITY는 저장 파일 개수를 활성 계산 개수로 취급했다. 실제 저장 16개는 COMPLETE 11개, CANCELLED 3개, PAUSED 2개였고, READY는 0개였다. 총 저장 크기는 81,378,187 bytes였다. 완료 결과까지 활성 슬롯으로 세어 신규 계산이 24시간 TTL 만료까지 막혔다.

AutomaticSearchService는 이제 READY만 활성 admission 16개에 포함한다. COMPLETE와 PARTIAL은 완료 결과이고, PAUSED와 CANCELLED는 계산 중이 아니므로 admission을 점유하지 않는다. PAUSED/CANCELLED는 완료로 변환하지 않으며 원본 checkpoint와 상태를 유지한다. RESUME은 그래프가 끝나지 않았고 READY가 아닐 때 활성 슬롯을 다시 확보해야 한다. 따라서 취소 후 새 계산으로 16개를 채운 경우 이전 계산을 재개해 17개로 늘릴 수 없다.

모든 생성·조회·제어는 기존 서비스의 synchronized 경계에서 직렬화한다. 동시 동일 clientRequestId는 한 작업으로 합쳐지고, 서로 다른 생성은 admission 검사와 저장 사이에 끼어들 수 없다. 취소·재개는 revision/generation을 유지해 같은 이전 revision으로 들어온 두 제어 중 하나만 반영한다. 복원도 동일 경계 내에서 직렬화되며 기존 LRU graph cache 2개를 유지한다(복원·신규 생성 중 임시 graph 1개 추가 가능). 별도 백그라운드 계산은 없다. 이 보장은 기존 단일 앱 인스턴스 구성 기준이며 다중 프로세스 공유 저장소는 새로 지원하지 않는다.

## 저장 보호와 임시 판단

- 활성 계산 한도 16개 유지. READY를 admission 소유자로 정의했다. PAUSED는 재개 가능성을 보존하면서 CPU 실행을 예약하지 않는다.
- 파일당 64MiB 유지.
- 저장 총량 1GiB 추가: 기존 16 × 64MiB의 최악 저장 크기와 동일하다. 파일 교체는 이전 파일 크기를 제외한 교체 후 총량을 검사한다. 원자적 교체 중에는 기존 파일과 임시 파일이 함께 존재해 최대 64MiB가 일시 추가될 수 있다.
- 저장 파일 256개 추가: 작은 완료 결과의 개수와 디렉터리/메타데이터 탐색도 유한하게 제한한다. 활성 한도의 16배를 결과 보관 예산으로 선택했으며 성능 최적값이라는 주장은 하지 않는다.
- 기존 생성 후 24시간 TTL 유지. 조회·제어는 만료를 연장하지 않는다. 기존처럼 다음 create에서 만료 결과를 정리하며, 이번 검증에서는 만료 전인지 확인해 원본 16개 정리가 발생하지 않도록 했다.
- 파일 형식·버전·원본 결과 변경이나 migration이 필요하지 않다. 기존 결과를 삭제하거나 취소하여 공간을 확보하지 않는다.
- 저장 총량/개수 초과는 JOB_STORAGE_CAPACITY로 신규 생성 거절, 활성 슬롯 초과는 JOB_CAPACITY로 구분한다. 6개 언어 UI 안내도 분리했다. 활성 슬롯은 완료·중단 시 해제되므로 만료 시각을 유일한 해제 시각처럼 안내하지 않는다. 자동 POST 재시도는 하지 않는다.

## 검증 범위

- 완료 결과 16개를 실제 작은 엔진에서 생성·완료하고, 17번째 계산을 생성·완료하며 기존 16개 조회 응답과 저장 bytes를 비교한다.
- READY 16개에서 신규 생성 429, 같은 clientRequestId의 재요청은 기존 결과 반환.
- 동시 동일 생성 4개는 한 id, 활성 15개에서 서로 다른 동시 생성 4개는 1개 성공·3개 429.
- 취소로 해제한 슬롯을 새 작업이 차지하면 이전 작업의 RESUME은 429이며 checkpoint bytes가 바뀌지 않는다. 다시 슬롯이 나면 재개 성공, 동일 명령은 idempotent.
- PAUSED는 보존·조회 가능하되 활성 16개에서 재개 불가. 같은 revision 취소·재개 경합은 하나만 성공하고 하나는 STALE_REVISION.
- 생성 중 저장 예산 실패는 작업을 남기거나 활성 슬롯을 소모하지 않는다.
- 저장 1GiB 및 256개 경계에서 추가 저장 거절, 기존 결과 파일 보존, 기존 파일 교체 가능.
- 활성 16개 TTL 경계 전후의 거절·만료 정리 회귀도 유지한다.

최종 검사 및 localhost 실행 결과는 아래에 기록한다.

## 최종 자동 검사

최종 소스에서 Backend `spotlessApply check generateJooq bootJar` 성공(9분 7초), 단위/ArchUnit 849개 및 통합 8개 모두 통과, 실패/skip 0개. Frontend `npm ci`, lint, typecheck, format:check, 2,222개 테스트, build 통과. 수정 영역의 포맷된 소스는 검증 환경과 작업 트리를 일치시켰다. 이번 변경은 Windows 실행 스크립트를 변경하지 않았다.

검증 로그: `E:/CODEX/codex/2026-10-19/task-2/admission-backend.log`, `admission-test-summary.json`, `admission-ui.log`. 실제 결과 XML은 같은 디렉터리의 `empty-focused-results`, `empty-integration-results`에 보존했다. 제작 시나리오 회귀 검사도 전체 검사에 포함되어 통과했다.

## localhost 반영 및 실제 새 계산

제품 커밋 `2c601dae4e09fbfa30ab12d0443028b456fd5d11`의 검증된 JAR/프런트를 localhost에 반영했다. 첫 반영 후 JS 해시 검사가 임시 배포 스크립트의 기존 assets 디렉터리 복사 오류를 발견해 중단했고, 복사 대상을 파일 단위로 수정해 프런트만 다시 반영했다. 이 실패 시점에는 실제 새 계산 POST를 보내지 않았다. 재검사에서 readiness UP, UI 200 및 아래 파일 해시가 모두 일치했다.

- 앱 이미지 `exile-hephaistos-app:empty-2c601da`, 프런트 `exile-hephaistos-frontend:empty-2c601da`.
- JAR SHA256 `d2d4344b6330f39ec6714a5751242f9b79ef113a00677c77b05c33108744e81c`.
- JS `index-DZsOu3a_.js`, SHA256 `1f332992e20bf550a6b9d319bbfbedfc18b98e8242457b8b6fb831773a2fc539`.
- 기존 PostgreSQL/Redis 컨테이너 ID 및 `.env` hash 동일. 원격 push/merge/배포 없음.

| 실제 시작 → 목표 | 생성 | 종결 | 시간 | 제어 요청 | 연산 | 노드/방법 |
|---|---|---|---:|---:|---:|---:|
| Solar Amulet ilvl82, RARE, 정신력15 implicit, 명시옵션 없음 → RARE 소환수 스킬 레벨≥3 | HTTP 202 | COMPLETE, complete=true | 3,700ms | 6 | 7,037 | 2/16 |

실제 검증 작업 id는 `68ea474f-21c4-408d-9493-a9d20e9f4e39`이다. 기존 16개 결과를 삭제·취소·변경하지 않고 17번째 작업으로 생성했다. 동일 clientRequestId를 다시 보내도 같은 id를 반환했고 GET 응답은 완료 응답과 같았다. 원본 16개 파일의 SHA256은 계산 전후와 브라우저 검증 후 모두 동일했다. 현재 총 17개는 원본 16개와 이번 합성 검증 결과 1개이며, 이 결과 역시 보존했다.

브라우저는 실제 localhost 번들과 실제 서버 응답을 사용했고 API mock을 사용하지 않았다. 실제 새 결과의 idempotent POST 202 후 화면에 탐색 완료, 엑잘1회+카오스100회 5.81% 등 결과가 표시됐다. 새로고침 후 GET 200으로 같은 작업을 복원했고 추가 POST는 없었다. 모바일 inspector clientWidth/scrollWidth는 모두 317px, 브라우저 오류 0개였다. 처음 브라우저 검사 입력에서 API 버전2를 UI 저장 계약에 넣어 화면을 복원하지 못했으나, UI 계약 버전으로 검사 입력을 수정한 뒤 통과했다. 이 검사 입력 수정은 제품 코드 변경이 아니다.

증거 파일: `admission-deployment-verification.json`, `admission-live-before.json`, `admission-live-result.json`, `admission-live-browser.json` (모두 `E:/CODEX/codex/2026-10-19/task-2`). 스크린샷은 내부 검토용으로만 보관하고 Library에 올리지 않았다.

## 사용자에게 적용되는 결과와 남은 한계

이전의 '한국시간 10월20일 01:10 이후까지 기다려야 한다'는 안내는 이번 수정으로 대체된다. 현재 저장 결과 때문에 신규 계산이 막히지 않는다. 현재 화면을 새로고침하고 다시 계산을 시작할 수 있다. 실제 READY가 16개이면 완료/중단으로 슬롯이 해제될 때까지 429가 유지된다. 저장 한도에 실제로 도달했을 때는 별도 저장 용량 안내가 나온다.

확률 모델의 기존 미지원 결합 질량과 PARTIAL 표시는 그대로다. 이번 수정이 미지원 제작 메커니즘을 지원한다고 주장하지 않는다. 전체 제작 시나리오 회귀는 최종 소스에서 다시 통과했다. 현재 작업에 남은 실행 차단은 없다.
