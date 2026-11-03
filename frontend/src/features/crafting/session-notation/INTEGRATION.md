# 세션 기보 연결 계약

기준 HEAD: `e82e246de18df1688e0eaba8a858b57a3c5b60ff`. 소유 범위는 이 디렉터리뿐이다.

## 공통 Page 연결

```tsx
import { SessionNotation } from './session-notation/SessionNotation'
```

root가 배치하는 `.bench-lower`의 피드백 아래에 다음을 추가한다. 기존 `film`은 `filmState.revision === draft.baseRevision`인 활성 Film으로 이미 제한되어 있다.

```tsx
<SessionNotation
  film={film}
  cursor={filmState.history.cursor}
  resources={stashResources}
/>
```

`resources`는 필터링 전 전체 `WorkbenchResource[]`이다. 과거 징조와 현재 선택 불가능한 재료도 표시하므로 현재 stash 탭/노출 정책으로 걸러서 전달하지 않는다. CSS는 컴포넌트가 직접 import하며 `.wb-session-notation` 아래에만 적용된다. 추가 callback, 저장, tooltip 인스턴스는 필요 없다.

## 표시 범위·증거

- 활성 Film의 frame 1부터 cursor까지 포함한다. cursor 이후 기록은 표시하지 않는다. undo에서 기보가 짧아지고 redo/복원에서 다시 나타난다. 마지막으로 표시된 화폐 행에 `aria-current="step"`과 현재 단계 표시가 붙는다. cursor가 관측 frame이면 그 이전 마지막 화폐 행이 현재 화폐 단계다. 루트는 단계 0이다.
- 분기 시 `recordCraft`/`recordAbyss`가 만든 새 Film만 표시한다. 새 Film의 루트 이전 기록을 원래 Film에서 이어 붙이지 않는다. 번호는 확인된 화폐 적용 행에만 1부터 부여한다.
- `recordCraft`의 `evidence.action` 또는 `recordAbyss`의 `manual.action`이 frame action과 일치할 때만 화폐 후보로 취급한다. 기존 `workbenchCurrencyActions` 역방향 resolver와 `isBone`으로 화폐 ID를 찾는다. `ABYSS_*` 관측, IMPORT, 루트, base 선택은 화폐 행이 아니다. `ABYSS_APPLY`처럼 화폐 ID를 확인할 수 없는 기록은 한계로 알린다.
- 기준 저장 형식에는 `receipt`가 없다. 성공 기록은 `recordCraft`가 applied 응답만 저장하고, Page가 성공·취소/늦은 응답 검사 후 `recordAbyss`를 호출한다는 기존 계약을 따른다. 이 표시를 현재 catalog의 재검증 또는 실제 게임 사용 영수증으로 표현하지 않는다.
- 징조는 저장된 `consumedOmens`만 같은 행에 표시한다. 활성/남은 징조 또는 trigger로 추정하지 않는다. 기준 작성기는 `manual.consumedOmens`의 빈 배열을 생략한다. root는 신규 기록에만 배열을 항상 저장하도록 변경할 예정이며, 명시 `[]`는 확인된 미소비, 없는 배열은 정보 미기록으로 구분한다. 기존 기록은 다시 쓰지 않는다. action-only legacy 기록은 화폐 행으로 복원하지 않는다.
- modifier 정의는 읽지 않는다. 아이템 수치/성공 여부를 재계산하지 않으며 기존 catalog 검증 경계는 root가 유지한다. tooltip은 resource ID로 기존 `MaterialTooltip`이 설명·출처를 제공한다.

## Tooltip·접근성

`resources`에서 찾은 ID에만 `data-material-tooltip`을 준다. 기존 Page의 document-level `MaterialTooltip` pointerover/focusin/Escape 처리를 그대로 쓴다. 새 popover나 이름만 있는 title은 만들지 않는다. 작은 아이콘은 focus 가능하며 localized aria-label을 갖는다. 이미지 누락·실패는 이름 목록 대신 중립 기호를 표시한다. locale은 기존 `useI18n`을 구독하며 독립 문구는 6개 언어로 제공한다.

## 통합자가 확인할 항목

Page에서 실제 적용 성공 → 기보 증가, 실패/취소 → 불변, undo/redo, 새 분기, 보관함 복원, 기존 tooltip 포커스/포인터 동작, 전체 Frontend 검증을 수행한다. 이 Worker는 공통 Page/history/CurrencyImage/전역 번역을 수정하지 않는다.

## Worker 검증·자체 리뷰

실행 경로: 현재 Worker worktree의 `frontend`, 2026-11-03. 검증 범위는 공통 Page에 연결하기 전 독립 모듈이다.

- PASS: lockfile 유지 `npm ci` (일반 호스트 실행). sandbox 최초 설치는 registry DNS ENOTFOUND로 중단되어 재시도했다.
- PASS: `npm run test -- --run src/features/crafting/session-notation --maxWorkers=1` (일반 호스트), 2 files / 20 tests. pretest의 6개 언어 표시 coverage 검사도 통과했다. sandbox 최초 실행은 임시 IPC 파일 ENOENT로 **0개 테스트**에서 중단되었으며, 이를 assertion 실패나 통과로 집계하지 않는다.
- PASS: `npx eslint src/features/crafting/session-notation --max-warnings 0`, `npm run typecheck`, `npx prettier --check src/features/crafting/session-notation`, `npm run build`, `git diff --check`.
- 자체 리뷰: Frame/Film 원본·raw provenance·저장 key를 변경하지 않는다. 출력 배열은 복사하며 deep freeze 입력으로 불변성을 확인했다. 실패 응답과 AbortError, 관측 제외, legacy 미상, 명시 빈 징조 배열과 누락 배열, 동시 소비 징조, cursor prefix, 분기, 저장 복원, 6locale, tooltip pointer/focus/Escape, 이미지 fallback을 검증했다.
- 첫 실제 테스트에서 발견한 Windows 파일명 충돌을 helper `extractSessionNotation.ts`로 분리했고, shared HTTP fixture가 Transmutation만 지원하는 점과 관측 테스트의 실제 before state를 수정했다. 기대값 완화는 하지 않았다. 수정 후 20개 모두 통과했다.
- 미실행: 전체 Frontend suite 및 공통 Page 연결 후 화면/브라우저 검증은 root 담당이다. Backend·DB 통합·jOOQ는 변경 범위가 아니며 이 Worker는 Docker 소켓/서버를 조작하지 않았다.
