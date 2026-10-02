# 야간 작업 인계 — 2026-10-02 UTC

시작 baseline은 `origin master e937ddccf7493ea0d114139142ff36224c3a672c`이며 원격은 `HappyBird6/Exile-Hephaistos`로 확인했습니다. 원본 저장소는 `E:\WORK\Exile-Hephaistos\Exile-Hephaistos`, master/clean 상태를 보존했습니다. 작업 브랜치는 `workbench/20261002`, 작업 위치는 `E:\WORK\Exile-Hephaistos\codex\workbench-20261002`입니다. 마지막 코드 커밋은 **20851fd**이며 이 인계 문서 추가 커밋은 `git log -1`에서 확인할 수 있습니다. 원격 push·merge·배포는 수행하지 않았습니다.

## 현재 사용할 수 있는 범위

등록 220개 중 구현은 **60개: 화폐 19, 징조 11, 에센스 29, Alloy 1**입니다. 이는 전체 게임 목록이나 모든 장비 지원을 뜻하지 않습니다. 실제 활성 베이스는 **Solar Amulet 하나**입니다. Stocky Mitts는 무고정옵션 기반과 자료 검증만 준비했고 제작 선택은 비활성입니다.

Workbench에 반복 Shift 사용, 선택 취소, 즐겨찾기 징조 활성화·충돌 방지, 좌우 창고/속성·히스토리 이동, Alt 동안 inline 범위 표시, 제작 세션별 선형 필름과 과거 단계에서 새 필름 생성이 구현되어 있습니다. 새 필름을 만들어도 원래 미래 기록을 보존합니다. 마지막 단계에서는 각 필름 단계의 rule/ledger/가정/선택 비율을 재로드 후에도 표시하도록 보존했습니다. 이전 version-1 필름은 그대로 읽고 근거를 임의 생성하지 않습니다. 잘못된 선택적 근거는 경고·숨김 처리하며 유효한 아이템 기록과 기존 저장 내용은 유지합니다. 로그인/사용자별 DB는 구현하지 않았습니다.

주요 최신 커밋: `dd85498` 무고정옵션 기반, `01fbe26` 수치 domain 차단점, `f6f9084` 이슈 목록·공통 비율 모델, `1e5aa54` 비율 증거 계약, `20851fd` 단계별 근거 재로드 보존.

## 검증·가정·미지원 구분

- 검증된 자료: Solar 활성 catalog·조건 및 기존 제작 결과; Stocky Mitts 일반 풀 182개/게시 가중치 합 148,200과 176개 상세 근거 대응. 복합 속성 42개 전체의 stat ID·범위는 확보했습니다. PoE2DB 게시 가중치는 게임에서 추출한 실제 확률이라고 주장하지 않습니다.
- 사용자 승인 가정: 복합 수치는 동일 비율로 연동하고 반올림합니다. 구현은 0–10000 tick, HALF_UP(±0.5는 0에서 멀어짐)이며 미검증 추정으로 표시합니다. 비율 tick 균등과 반올림 결과 tuple 균등은 다릅니다. 기본 Solar는 이 모델에 opt-in하지 않습니다. 기존 1/N 합의는 허용 결과 집합이 확인된 경우에만 적용합니다.
- 미지원: 장갑 상세 **6개**(Encased, 요구량 감소 5단계), 일부 수치 정밀도/표시 단위, Catalyst·특수 상태·다른 베이스 규칙. 불완전 풀을 제거·재정규화하거나 stat ID를 만들지 않았습니다. Vaal Orb/Hinekora는 사용자 범위 제외이며 버그가 아닙니다. 4개 encounter 징조는 장비 제작 범위 밖입니다.

## 최신 검사와 실행하지 않은 검사

| 검사 | 결과/범위 |
|---|---|
| Docker BE 전체 | 1e5aa54에서 **183개**(unit 177 + integration 6), formatter/check/JOOQ/bootJar 통과. 그 이후 BE 변경이 없어 재실행하지 않음 |
| Docker FE 전체 | 20851fd에서 lint/typecheck/format/**106개**/build 통과 |
| 실제 Chromium | 20851fd에서 **56개**: 일반 30, 가정 재로드/이동 9, 잘못된 근거/legacy 6, snapshot 호환 11 |
| 누락 조합 보완 | 현재 코드 HEAD에서 **21개** 추가: Alchemy→Fracturing→Essence/징조→Alloy→Shift Divine, 필름 근거·잠금·과거 제작·미래 보존·재로드·Alt·420px 화면, page error 0 |
| 범위 제한 | 복합 모델 브라우저는 명시적 synthetic 계약 fixture이며 장갑 실제 지원 검사가 아님. 기존 각 Essence/Alloy의 전용 전체 브라우저 묶음은 이번 인계 단계에서 반복하지 않음. 기존 cache 지속성 검증도 이전 결과이며 새 재시작 검사가 아님 |

증거는 모두 `E:\WORK\Exile-Hephaistos\codex\qa-20261002`에 있습니다: `history-evidence-validation.json`, `history-evidence-*-browser-results.json`, `overnight-cross-feature-browser-results.json`, `coupled-evidence-backend-artifacts/test-results`. 이후 실패가 재현되지 않아 추가 코드를 억지로 변경하지 않았습니다.

## 내일 검토 우선순위 — 최대 5개

1. [WB-001](../ISSUES.md): 공통 비율 가정, tick 해상도/분포, HALF_UP 정책을 유지할지 실제 근거로 수정할지.
2. [WB-002/WB-006](../ISSUES.md): 장갑 6개 상세가 확보되기 전에는 비활성 유지. 이미 실패한 주소를 반복 조회하지 않기.
3. [WB-003/WB-004](../ISSUES.md): 정밀도·표시 단위·Catalyst 증가량/반올림/가중치의 검증 또는 별도 가정 범위.
4. [WB-007](../ISSUES.md): 향후 새 베이스 API에서 대상 ID를 별도 modifierId 필드로 분리할지. 현재 tick·값 검증은 완료.
5. [WB-008](../ISSUES.md): 선택적 제작 근거의 저장량과 표시 범위. 이전 기록·용량 실패 처리는 검증 완료.

이슈 관리는 루트 **ISSUES.md 한 곳**에서 합니다. 감사 문서는 근거로 링크합니다. 야간 임시 추천안은 되돌릴 수 있도록 기록했고 인증·보안·약관·영구삭제·DB 초기화·원격 작업에는 적용하지 않았습니다.

## 실행 및 재개

- 현재 QA UI: **http://localhost:18081**, API: **http://localhost:18080**. FE/app/Redis/PostgreSQL 4개 QA 서비스는 유지했습니다. 최신 app 불변 JAR는 `poe2craft-9ed0d92fbe65.jar`, PostgreSQL healthy입니다. 원본 DB/볼륨은 보존했습니다.
- 제 일회성 Docker Chromium/Node 검사는 `--rm`으로 종료되었고, BE 검사 컨테이너는 종료 코드 0의 정지 상태로 보존했습니다. 무관 서비스/사용자 테스트 서버를 종료하지 않았습니다.
- 작업 폴더에서 `git -c safe.directory=E:/WORK/Exile-Hephaistos/codex/workbench-20261002 status`로 시작하고 `ISSUES.md`·`AGENTS.md`를 읽으세요. QA 상태는 `docker compose -f E:\WORK\Exile-Hephaistos\codex\qa-20261002\compose.yaml ps`로 확인합니다.
- BE 변경 후에는 기존 `docker start -a exile-workbench-backend-check-20261002` 필수 검사 컨테이너를 재사용할 수 있습니다. FE 검사 스크립트는 codex QA의 `history-evidence-frontend-check.sh`, 마지막 조합 검사는 `overnight-cross-feature-browser.cjs`입니다. 실제 runtime/test는 프로젝트 Docker로 실행합니다.
- 데이터 차단이 풀리면 Workbench-only catalog/기본 Essence 순서로 재개하고 Solar 회귀를 유지합니다. 독립적으로 가능한 확정 구현/재현된 버그가 없어 현재는 안전하게 대기하는 상태이며 프로젝트 전체 완료를 주장하지 않습니다.