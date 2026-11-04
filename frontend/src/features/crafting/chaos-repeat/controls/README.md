# Chaos repeat controls 준비 UI

후속 기준 `ecb65d27c7a200b5ba189290470736e5ac153090`, branch `codex/chaos-repeat-controls-limits`.
사용자 확정으로 초기 반복 횟수는 100, UI 입력 상한은 1000으로 갱신했다.
기존 카오스 준비 commit과 별도 창고 수정 `f809f46d` branch·로그는 보존했다.
변경 소유 범위는 이 `controls/` 디렉터리뿐이며 Page·Provider·API·저장·기보·formatter·stash CSS는 수정하지 않는다.

## 제어 계약

`index.ts`에서 `ChaosRepeatControls`, props/types, 3종 화폐 ID, 메시지, 초기 입력 상수를 export한다.

| prop/export                                  | 책임                                                                                                                                                                |
| -------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `enabled`, `onEnabledChange(boolean)`        | ON/OFF 모드 설정만 전달한다. 실행하지 않는다.                                                                                                                       |
| `pattern`, `onPatternChange(string)`         | 공백·불완전한 정규식 모양을 포함한 원문 문자열을 전달한다. 컴파일·매칭·trim하지 않는다.                                                                             |
| `maximumUses`, `onMaximumUsesChange(string)` | 숫자 편집 중의 빈 값·선행 0·불완전한 값도 그대로 전달한다.                                                                                                          |
| `defaultChaosRepeatMaximumUses`              | 부모 초기 입력에 사용할 `'100'`. 컴포넌트 내부 상태가 아니다.                                                                                                       |
| `chaosRepeatMaximumUsesLimit`                | 확정된 UI 입력 상한 `1000`. 서버가 보고하는 상한과 별도다.                                                                                                          |
| `parseChaosRepeatMaximumUses(raw)`           | 유효하면 1..1000 정수, 잘못된 입력이면 null. 부모의 실제 실행 handler가 반드시 재사용한다.                                                                          |
| `running`                                    | 실제 요청이 진행 중일 때 부모가 true로 설정한다. 입력·toggle을 잠그고 indeterminate spinner, `aria-busy`, polite live 설명을 표시한다.                              |
| `disabled`                                   | 부모의 외부 사용 불가 상태. 이것만 true이면 진행 중 설명/애니메이션을 만들지 않는다.                                                                                |
| `currencyId`                                 | 일반/Greater/Perfect Chaos의 실제 표시 대상. 기본 일반 Chaos이며 3종 모두 기존 Chaos 이미지 자산을 재사용한다. 화폐 선택 UI나 실행 명령이 아니다.                   |
| `errors.pattern/maximumUses/request`         | 부모/API가 검증한 현 언어 오류. 횟수 오류는 로컬 범위 오류보다 외부 오류를 우선한다. 필드 aria-invalid/describedby와 role=alert로 표시한다.                         |
| `serverMaximumUses`                          | 기존 표시 전용 계약 유지. null/undefined는 미확정이며, 실제 서버 한도 검증과 오류는 부모/API가 제공한다. UI 범위 1..1000을 동적으로 바꾸거나 값을 clamp하지 않는다. |
| `onHintInteraction(currencyId)`              | hover/pointerdown/focus/click의 선택적인 설명 연결 hook. 같은 제스처에서 여러 번 호출될 수 있으므로 부모는 멱등적으로 처리한다. 실행 명령으로 쓰지 않는다.          |

실행·취소·타이머·확률·평균 비용·진행률·성공 판정·결과·기보·storage callback은 없다.
요청이 끝났다는 이유로 성공 문구를 만들지 않고 `running=false`이면 busy 설명을 비운다.
로컬 횟수 검사가 통과해도 regex나 서버 한도가 검증됐다는 뜻은 아니다. 부모가 실제 실행 가능 여부를 결정해야 한다.
`running` 동안 다른 화폐의 실제 handler를 잠그는 책임도 후속 Page 연결에 있다. 이 컴포넌트만으로 다른 버튼을 차단했다고 주장하지 않는다.

## 횟수 표시와 실행 유효성

`maximumUses`와 callback은 편집 원문을 유지한다. 0·음수·소수·1001·빈 값은 오류를 표시하며 `aria-invalid=true`다.
자동 보정·자르기·기본값 재삽입 없이 해당 값을 그대로 둔다. 1과 1000은 허용한다.
파서는 ASCII 10진 숫자 문자열만 허용하고 선행 0은 보존한 채 숫자로 읽는다.
공백·줄바꿈·지수/16진 표기·비숫자는 null이다. 사용자 정규식 패턴을 컴파일하는 파서가 아니다.

ON/OFF는 설정만 바꾸므로 횟수가 잘못돼도 모드를 끌 수 있다. 실행 경로는 별도이며 다음 계약을 따른다.
부모는 실제 Chaos 사용 시 `parseChaosRepeatMaximumUses(maximumUses)`가 null이면 반복 요청을 보내지 않아야 한다.
유효한 숫자가 나와도 외부 횟수 오류·서버 cap·패턴 검증과 실제 running/disabled 조건을 함께 검사해야 한다.
서버가 예를 들어 cap 25를 보고해도 로컬 파서는 900을 UI 범위 내 값으로 읽는다. cap 25 위반은 부모/API 오류로 차단해야 한다.
이 컴포넌트에 실행 callback은 없으며 로컬 검사가 전체 실행 허가로 쓰이면 안 된다.

6 locale에 기본 100·허용 1–1000 설명과 오류를 표시한다.
기본 정규식·대소문자 무시·옵션별 비교·복합 다중 행 옵션을 한 묶음으로 취급한다는 승인된 설명도 표시한다.
실제 정규식 지원 토큰·컴파일·매칭·API·취소 로직은 이 UI에서 구현하지 않는다. 취소 정책은 아직 사용자 답변 대기다.

## 공유 설명과 후속 Provider 연결안

현재 section의 `data-feedback-tooltip`으로 기존 Provider의 hover/focus/pointerdown/click 경로를 재사용한다.
한국어 설명은 승인 문구 `옵션에 패턴이 등장할때까지 카오스 사용`이며 6개 locale을 지원한다.
toggle에는 같은 설명을 참조하는 독립 SR `aria-describedby`도 있어 Provider가 고정 설명 때문에 hover를 차단해도 읽을 수 있다.
별도 tooltip이나 native title을 만들지 않는다. 이미지 오류 fallback은 기존 `CurrencyImage` 동작을 재사용한다.

현재 Provider는 실제 `held ?? selected`에 해당하는 `priorityMaterialId`가 있으면 일반 설명 갱신을 차단한다.
따라서 **고정된 동일 Chaos 설명에 hint를 붙이는 기능은 아직 연결되지 않았다**. 기존 우선순위를 우회하지 않는다.
후속 root 계약에서 다음 연결을 제안한다. 이 작업에서는 Provider를 수정하지 않는다.

1. Page는 실제 집거나 선택한 Chaos가 있으면 그 ID를 `currencyId`로 넘기고,
   `onHintInteraction`으로 마지막 설명 요청 ID만 임시 상태에 기록한다. 실제 실행에는 이 callback을 사용하지 않는다.
2. Provider에 선택적인 `priorityMaterialHint?: { materialId: string; text: string } | null` prop을 추가한다.
   Page가 기존 실제 `cursor?.id`(held 우선)와 요청 ID가 **같은 Chaos ID일 때만** 현 locale 메시지를 넘긴다.
   다른 재료를 집은 상태에서는 null로 넘겨 그 재료 설명 우선을 유지한다.
3. Provider의 고정 설명 생성 경로에서 기존 `MaterialDescription`과 출처 링크를 그대로 두고 해당 hint를 부가한다.
   `show()`의 priority 차단을 풀거나 controls를 고정 anchor로 바꾸지 않는다.
   `priorityAnchor`·기존 focus 복귀·공유 200px 패널·설명 수명·색상/링크를 보존한다.
4. locale/contextKey/실제 우선 재료가 바뀌면 hint 요청을 초기화하여 오래된 언어·다른 선택의 설명이 재사용되지 않게 한다.
   hover leave만으로 지우지는 않는다. 독립 BasePreviewTooltip 경로도 그대로 둔다.

## 후속 Page 연결 경계

현재 컴포넌트는 Page에 import하거나 canvas에 배치하지 않았다.
별도 연결 소유권을 받은 후, 현재 canvas의 Chaos 화폐와 하단 즐겨찾기 사이 공간에 넣는다.
컴포넌트 내부는 `[ON/OFF · 작은 Chaos 이미지 · 계속사용] [패턴 입력]` 및 다음 줄의 횟수 입력이다.
flex wrap·min-width 0·max-width 100%와 긴 오류 줄바꿈으로 좁은 호스트에서 축소할 수 있게 준비했다.
좌표·grid 구조·피드백 높이·하단 정렬을 이 파일에서 새로 정하지 않는다.

실제 일반/Greater/Perfect Chaos 사용 handler가 ON 상태를 보고 반복 API를 호출하는 연결은 후속 작업이다.
초기부터 일치해도 1회 사용, 마지막 결과와 정상 사용 N만 기록, 중간 frame 없음, 한도 도달의 마지막 결과/N 반영은 승인된 요구다.
이 컴포넌트는 그 실행·저장 의미를 구현하지 않는다.
기본 정규식·대소문자 무시·옵션별/복합 다중 행 단위와 기본 100·상한 1000은 사용자 확정 사항이다.
구체적인 regex 토큰 계약과 실제 API 연결은 root가 통합하며, 취소 정책은 사용자 답변 대기다.

## 이전 ecb65d27 준비 commit 검증 이력

검사 경로는 이 worktree의 `frontend`이며, package/lockfile가 이전 설치와 동일함을 확인해 `node_modules`를 재사용했다.
아래 47개 검사는 이전 기본 500·로컬 횟수 검증이 없던 준비 commit의 기록이다. 현재 소스의 결과와 구분한다.

- `npm run test -- --run src/features/crafting/chaos-repeat/controls src/features/crafting/feedback-tooltip --maxWorkers=1 --reporter=dot --reporter=json --outputFile.json=node_modules/.cache/chaos-repeat-controls/unit-feedback-vitest.json`
  : 4 files / 47 tests 통과(49.50초). 신규 controls 21, 기존 feedback 26이다. 최초 실행에서 모두 통과했다.
- `npm run lint`, `npm run typecheck`, `npm run format:check`, `npm run build`: 통과.
  build에 500kB 초과 chunk 경고가 있다.
- `git diff --check`: 통과. 새 파일은 commit 전 staged 검사에서도 확인한다.
- 검증 로그: 이 worktree의 `frontend/node_modules/.cache/chaos-repeat-controls/unit-feedback-vitest.json`,
  `build.log`, `verification.json`을 보존한다. `verification.json`에 실제 명령 결과와 제품·테스트 소스 SHA256을 기록한다.
- 이번 작업에서 dependency 설치 실패·IPC 실패·assertion 실패는 없었다. 이전 작업의 제한 환경 IPC 실패를 근거로
  Vitest와 build는 처음부터 승인된 host 실행을 사용했다. `npm ci`는 동일한 의존성 재사용 지시에 따라 재실행하지 않았다.

자체 리뷰에서 ON/OFF가 네트워크/실행을 호출하지 않음, 모든 입력 callback의 busy guard,
3 Chaos 종류·기존 자산, null cap·원문 입력 보존, 외부 오류만 표시, 실제 busy 전용 상태와 reduced-motion,
6 locale·SR 이름/설명·focus·키보드 생성 click, 공유 hint와 선택 재료 우선순위를 확인했다.
고정된 동일 Chaos에 hint를 부가하는 계약은 README 제안 단계이며 실행·Page 연결 완료로 표현하지 않는다.

컴포넌트가 Page에서 import되지 않으므로 build는 새 UI가 화면에 배치되었음을 검증하지 않는다.
새 파일의 TypeScript 컴파일과 단위 DOM/CSS 검사를 수행했고, 기존 앱 build의 성공을 확인한 것이다.
jsdom은 실제 배치 엔진이 없어 좁은 화면의 픽셀 단위 overflow, 실제 장치 touch·키보드·스크린리더·reduced-motion 환경을 검증하지 않았다.
브라우저 자동 QA·Page 전체 테스트·실사용 storage·서버·Docker·DB·Redis·설정·실제 저장 기록·원격 push는 지시대로 미실행이다.
테스트의 locale/storage 접근은 jsdom 안에서만 이루어졌다.

검사 뒤 이 Worker가 만든 `node_modules/.vite`, `node_modules/.vite-temp`만 절대 경로를 확인해 정리했다.
worktree·node_modules·dist·검증 로그는 리뷰까지 보존하며 공유 cache나 다른 Worker 자료는 삭제하지 않는다.

## 이번 100/1000 후속 검증

초기 관련 검사 5 files / 70 tests와 lint·typecheck는 통과했다.
자체 리뷰에서 숫자 파서의 줄바꿈 거부를 명확히 보강하고 경계 검사를 추가했다.
최종 고정 소스에서 다음 명령이 모두 종료 코드 0으로 통과했다. 이후 README 결과만 보완했다.

- `npm run test -- --run src/features/crafting/chaos-repeat/controls src/features/crafting/feedback-tooltip --maxWorkers=1 --reporter=dot --reporter=json --outputFile.json=node_modules/.cache/chaos-repeat-controls-limits/final-impact-vitest.json`
  : **5 files / 71 tests 통과**, 27.94초. controls·숫자 파서 45개, 기존 feedback 26개다.
- `npm run lint`, `npm run typecheck`, `npm run format:check`, `npm run build`: 통과.
  build의 500kB 초과 chunk 경고는 남는다. 이번 검사 실패·skip·기대값 완화는 없었다.
- `git diff --check`와 staged 검사로 변경 범위를 확인한다. 최종 source SHA256과 build의 dist SHA256은 전용 `verification.json`에 기록한다.

검증 로그는 `frontend/node_modules/.cache/chaos-repeat-controls-limits/`에 별도 보존한다.
`impact-vitest.json`은 초기 70개 결과, `final-impact-vitest.json`은 최종 71개 결과이며 `build.log`, `verification.json`도 보존한다.
자체 리뷰에서 100/1000 정책·원문 보존·로컬 오류/외부 오류 우선·파서 null 실행 차단 계약과 UI 모드 설정의 분리를 대조했다.
Page 전체 테스트·전체 Vitest·브라우저 QA·API 실행·storage·서버·Docker·원격 push는 지시 범위 밖이므로 미실행이다.
`npm ci`는 package/lockfile가 이전 설치와 같아 재실행하지 않았다. build는 ecb65d27 기반 준비 branch의 컴파일 검증용이다.
창고 수정과 카오스 최초 준비 로그, 공유 cache·deps·dist는 삭제하지 않는다.
