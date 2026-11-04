# 세션 기보 연결 계약

현행 기준 `3f4495e664d8112011cc5dcd9f17330446167a97`와 이번 Chaos 반복 기록 준비 변경.

## Page 연결

```tsx
<SessionNotation
  film={film}
  cursor={filmState.history.cursor}
  resources={stashResources}
  onNavigate={onNavigate}
  disabled={busy}
/>
```

`resources`는 필터링 전 전체 표시 registry다. 옛 화폐·징조도 ID를 표시하므로
현재 선택 가능 자원만 전달하지 않는다. 부모는 catalog 검증과 실제 cursor·저장을 소유한다.
기보는 읽기 전용 투영이며 데이터나 storage를 수정하지 않는다.

## 범위와 증거

- 활성 Film의 전체 화폐 기록을 cursor 이후까지 보존한다. cursor는 현재 강조만 바꾼다.
  2열, 행 순서, 실제 steps 요소의 `wb-scrollbar`를 유지한다.
- `recordCraft`의 evidence 또는 `recordAbyss`의 manual action이 Frame action과
  일치할 때만 화폐 후보로 취급한다. 두 evidence가 충돌하면 생략한다.
  currency ID는 기존 역방향 resolver와 `isBone`으로 찾는다.
- 루트·IMPORT·BASE_SELECTION·ABYSS 관측은 화폐가 아니다. 성공 evidence 또는
  화폐 ID가 없는 기록은 생략하고 한계 문구를 표시한다.
- 징조는 저장된 consumedOmens만 표시한다. 명시 `[]`는 확인된 미소비이고,
  누락 배열은 미기록이다. 활성/남은 징조를 통해 소모를 추정하지 않는다.
- 새 분기는 새 Film의 단계 1부터 시작한다. 기존 Film이나 미래를 이어 붙이거나 삭제하지 않는다.

## Chaos 반복 확장

writer와 adapter의 정확한 계약은 [Chaos 반복 기록 준비](../chaos-repeat/README.md)를 따른다.
반복 실행 UI/API·regex·상한·취소 정책은 여기서 정하지 않는다.

- `Frame.chaosRepeat`가 없으면 `NotationStep { uses: 1, repeated: false }`다.
  일반 1회의 기존 화면은 그대로이고 인접 Chaos 기록도 합치지 않는다.
- 유효한 확장이면 `uses`를 그대로 투영하고 `repeated: true`다. `uses: 1`도
  count를 명시한다. 해당 일반/Greater/Perfect Chaos 아이콘과 현재 언어 숫자의
  count 문구를 한 행에 표시한다. N개 이미지·Frame·행으로 펼치지 않는다.
- 명시된 확장이 손상됐거나 Frame/evidence action과 맞지 않으면 화폐 행을 생략한다.
  단일 1회로 복원하지 않는다. repository도 손상 새 확장을 거부하고 저장 bytes를 보존한다.
- evidence의 consumedOmens는 API 검증기가 확인한 batch 전체 소모 집계다.
  count로 징조 소모를 곱하거나 마지막 witness에서 누적분을 추정하지 않는다.
  확인된 중복 ID도 소모 건수대로 아이콘을 보존한다.
- 최종 witness events/assumptions는 마지막 성공 회차의 증거다. 기보 count를
  전체 전이 trace 또는 실제 게임 화폐 사용 증명으로 표현하지 않는다.

## Tooltip·접근성과 레이아웃

registry에 있는 ID에만 `data-material-tooltip`을 준다. 기존 MaterialTooltip/feedback
provider 연결을 유지하며 새 tooltip 인스턴스나 title을 만들지 않는다. 독립 컴포넌트의
fallback tooltip 검사와 실제 Page provider 흐름의 검사는 서로 다른 경계다.

반복 행과 native jump button의 SR 이름에는 localized 화폐 이름과 count가 포함된다.
일반 행의 기존 SR 이름은 그대로다. `onNavigate`가 있으면 아이콘은 중첩 focus를
만들지 않고 button 하나가 keyboard target이다. 없으면 아이콘을 직접 focus할 수 있다.
이미지 누락·실패는 기존 중립 기호와 접근성 이름을 유지한다.

count·징조 아이콘은 flex wrap을 유지하고 count에 min-width 0, max-width 100%,
overflow-wrap anywhere를 적용한다. jsdom 검사는 선언·계산 style을 확인하며 실제
브라우저의 픽셀 clipping/overflow 검증을 대신하지 않는다. 브라우저 QA는 사용자 보류다.

## 통합 시 확인

전용 반복 API 검증 후 최종 state와 count 저장 → batch 한 단계 이동 → 저장 복원,
실패/취소/늦은 응답 보호, 남은 징조, 동시 적용 차단, tooltip feedback와 전체 Frontend
검증을 root가 연결한다. 이 Worker는 CraftingPage/workbenchApi/공통 번역/provider를 수정하지 않는다.

## 이전 Worker 검증 기록 (2026-11-03, 이번 변경 전)

실행 경로: 현재 Worker worktree의 `frontend`, 2026-11-03. 검증 범위는 공통 Page에 연결하기 전 독립 모듈이다.

- PASS: lockfile 유지 `npm ci` (일반 호스트 실행). sandbox 최초 설치는 registry DNS ENOTFOUND로 중단되어 재시도했다.
- PASS: `npm run test -- --run src/features/crafting/session-notation --maxWorkers=1` (일반 호스트), 2 files / 20 tests. pretest의 6개 언어 표시 coverage 검사도 통과했다. sandbox 최초 실행은 임시 IPC 파일 ENOENT로 **0개 테스트**에서 중단되었으며, 이를 assertion 실패나 통과로 집계하지 않는다.
- PASS: `npx eslint src/features/crafting/session-notation --max-warnings 0`, `npm run typecheck`, `npx prettier --check src/features/crafting/session-notation`, `npm run build`, `git diff --check`.
- 자체 리뷰: Frame/Film 원본·raw provenance·저장 key를 변경하지 않는다. 출력 배열은 복사하며 deep freeze 입력으로 불변성을 확인했다. 실패 응답과 AbortError, 관측 제외, legacy 미상, 명시 빈 징조 배열과 누락 배열, 동시 소비 징조, cursor prefix, 분기, 저장 복원, 6locale, tooltip pointer/focus/Escape, 이미지 fallback을 검증했다.
- 첫 실제 테스트에서 발견한 Windows 파일명 충돌을 helper `extractSessionNotation.ts`로 분리했고, shared HTTP fixture가 Transmutation만 지원하는 점과 관측 테스트의 실제 before state를 수정했다. 기대값 완화는 하지 않았다. 수정 후 20개 모두 통과했다.
- 미실행: 전체 Frontend suite 및 공통 Page 연결 후 화면/브라우저 검증은 root 담당이다. Backend·DB 통합·jOOQ는 변경 범위가 아니며 이 Worker는 Docker 소켓/서버를 조작하지 않았다.
