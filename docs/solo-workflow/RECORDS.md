# 기록·상태·공통 도구

상위: [공통 규칙](MULTI_SESSION_WORKFLOW.md).

## 최소 파일 구조

공용 기록 위치는 SETUP.md에서 자동 결정한다. 관리자만 config.json, sessions.json, task.json을 쓴다. 작업자·리뷰어는 자신에게 지정된 report 파일만 쓴다.

- `config.json`: 감지 결과, 운영 버전, Git·외부 작업 승인, 실제 도구 연결.
- `sessions.json`: 실제 ID, 논리 역할·이름, task, worktree, 상태, 이전 ID 이력.
- `tasks/<task_id>/task.json`: 현재 단계, 소유권, commit, 검증·리뷰 참조, 수정 횟수, 다음 행동.
- `tasks/<task_id>/reports/<역할>-<번호>-<회차>.json`: 위임 결과.
- `tasks/<task_id>/logs/`: 필요한 명령 결과. 비밀 제거.

전체 이벤트 로그를 의무화하지 않는다. 작은 작업은 task.json 하나와 필요한 증거만 있으면 된다. 완료 작업의 상세 이력은 새 요청의 문맥에 넣지 않는다.

## 상태 전이

관리자가 증거를 확인하고 갱신한다. 작업자는 상태 변경을 요청하는 결과만 보낸다.

| 상태 | 진입 근거 | 다음 상태 |
|---|---|---|
| RUNNING | 요청·작업 공간·담당 기록 | VERIFYING, BLOCKED, NEEDS_INPUT, STATUS_UNKNOWN |
| VERIFYING | 구현 commit/비 Git 입력 상태 확정 | REVIEWING, FIXING, READY, BLOCKED |
| REVIEWING | 필요한 테스트 성공과 리뷰 대상 고정 | FIXING, READY, BLOCKED |
| FIXING | 결함과 잔여 예산 확인 | VERIFYING, BLOCKED, NEEDS_INPUT |
| READY | 필수 검증·리뷰 충족, 차단 결함 없음 | DELIVERING |
| DELIVERING | 저장된 Git 정책 범위 수행 | DONE, VERIFYING, BLOCKED, STATUS_UNKNOWN |
| DONE | 해당 정책의 산출물 확인 | 새 요청은 새 task |
| BLOCKED / NEEDS_INPUT / STATUS_UNKNOWN | 원인·완료분·다음 결정 기록 | 해결 근거 확인 후 마지막 유효 단계 |

독립 리뷰 생략·대체는 근거를 남긴 뒤 READY로 갈 수 있다. 고위험은 생략 불가. local의 DONE은 main 반영·push 완료를 뜻하지 않는다.

## 간결한 task 기록 양식

```json
{
  "task_id": "실제 생성값", "workflow_version": "2.0",
  "request": "짧은 요청", "kind": "ordinary", "state": "RUNNING",
  "manager_session_id": null, "worktree": "실제 절대 경로",
  "base_sha": null, "target_sha": null,
  "owners": [], "source_commits": [],
  "checks": [], "review": null, "findings": [], "fix_rounds": 0,
  "delivery": {"policy": "local", "commit": null, "pr": null, "push": "not_requested"},
  "next_action": "다음 작업", "updated_at": "UTC 시각"
}
```

각 check: 명령, cwd, 시작·종료 SHA, 관련 입력 식별값, 실행 환경, 종료 코드, 성공/실패/미실행, 로그 경로, 재사용 근거. 해당 없는 필드는 null과 이유를 사용하며 SHA를 만들어내지 않는다.
각 finding: ID, 관점, 심각도, 파일·근거, 원 대상 SHA, 담당, 수정 SHA, 재검증·재리뷰 참조, OPEN/CLOSED.

## 전달과 결과

전달: `task_id / message_id / parent_message_id / 실제 대상 ID / 역할 / 요구사항·계약 경로 / 소유 범위 / 기준 SHA / worktree / 검증 범위 / 결과 파일`.
결과: `DONE | DONE_WITH_CONCERNS | NEEDS_CONTEXT | BLOCKED`, 자기 실제 ID, 변경·commit, 검증 증거 경로, 남은 문제. 작업자의 DONE은 관리자 task의 DONE과 구분한다.
완료 보고는 변경 요약, 검증·리뷰 결과와 미검증, commit·PR·push 상태, 남은 결정 순으로 짧게 한다. 전체 로그를 대화에 붙이지 않는다.

## 포함된 공통 저장 도구

[scripts/state_io.py](scripts/state_io.py)는 Python 3 표준 라이브러리만 사용한다.

- `write PATH`: stdin의 JSON을 같은 디렉터리 임시 파일로 저장 후 원자 교체. config/task/report 저장에 사용.
- `claim PATH OWNER`: 잠금 파일 원자적 생성. 기존 파일이 있으면 실패하며 삭제하지 않음.
- `release PATH OWNER`: 일치하는 소유자 잠금만 해제. 이전 실행이 실제로 종료됐는지는 호출자가 확인.

OWNER는 실제 세션 ID와 이번 실행의 고유 토큰을 조합한다. 실제 ID가 없으면 논리 로컬 실행 ID와 토큰을 사용하되 메시지 주소로 쓰지 않는다. 잠금은 모든 참여자가 규칙을 지키는 로컬 파일시스템을 전제로 한다. 원격·비표준 파일시스템은 배타 생성/원자 교체 지원 확인 전 병렬 쓰기를 금지한다.
이 도구는 권한 검사·세션 생성·메시지·worktree 생성·테스트를 대신하지 않는다. 관리자가 기존 Git/실행 도구로 수행한다. write 호출 전에 관리자 잠금과 파일 소유권을 확인해야 한다. 스크립트가 없거나 Python이 없으면 플랫폼의 동등한 배타 생성·원자 저장을 사용하고 확인 불가능하면 병렬 운영을 중단한다.
