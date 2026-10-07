# Goal filter v1 프론트 통합 지침

공통 계약 기준: `8c4f758132a4eb732c0c8ca09cdb2c9b58d3ec62`. 공통 계약 파일은 수정하지 않았다.

## 연결

통합 담당이 기존 `QueryClientProvider` 안의 `CraftSupport.tsx`에서 `GoalFilterPanel`을 연결한다. 실제 catalog를 조회한 다음 `createGoalFilterEditor(emptyGoal(context, catalogVersion))`를 작업대/session별 한 번 생성하고 재렌더링 중 같은 store를 유지한다. 실제 아이템 snapshot/base/level을 `context`, registry base 목록을 `bases`, 기존 언어 상태를 `language`로 전달한다.

실제 서버는 `createHttpGoalFilterAdapter()`를 사용한다. 독립 demo에서만 `mockGoalFilterAdapter`를 사용한다. `useGoalFilterCatalog`와 `useGoalFilterValidation`은 context/AST를 Query key에 포함하고 AbortSignal을 전달한다. 새 identity에 이전 결과를 placeholder로 노출하지 않는다. Catalog 변경 시 기존 goal의 버전을 자동 대체하지 않는다. 기존 행을 보존하고 버전 불일치를 해결한 후 진행한다.

추천 요청은 통합 담당이 연결하고 결과 `recommendation`과 요청 당시의 `recommendationGoal`을 함께 전달한다. 현재 AST와 catalog identity가 같을 때만 숫자를 표시한다. COMPLETE/PARTIAL만 검증된 하한·상한·미해결 질량을 표시하며 UNKNOWN/UNSUPPORTED는 숫자 확률을 표시하지 않는다. 요청 item은 전체 ItemState JSON을 보존해야 하며 StateBucket이나 fixture observedStats를 사용하지 않는다.

필요한 소유자 연결: `CraftSupport.tsx` mount, 기존 언어 상태, registry/item state, 실제 catalog 초기화, 추천 요청 trigger. App, 공통 CSS, 공통 번역 사전 및 공통 타입은 수정하지 않았다.

## 승인된 차이와 한계

승인된 위임 지시와 부모 승인에 따라 계약의 영어 UI 문장보다 한국어 우선을 적용했다. 별도 `goalFilterNamespace = 'goalFilter'`와 ko/en messages를 제공한다. Catalog label, issue message 및 기술적인 그룹 이름은 서버 영어를 유지한다. 통합/계약 담당이 언어 문장을 일치시켜야 한다.

Fixture에는 합성 stat 3개만 있으며 production catalog 응답이 없다. Mock은 fixture-snapshot-v1 / fixture-base만 지원한다. 그 외 base는 빈 목록과 UNSUPPORTED_BASE를 반환하며 Solar로 대체하지 않는다. 합성 label은 mock에서만 생성한다. Production 후보·unit·eligibility·기여 metadata는 서버 catalog를 그대로 사용한다. 범주는 계약에 있는 EXPLICIT/IMPLICIT/PSEUDO이며 거래소의 다른 범주는 추측하지 않았다.

그룹 종류 변경은 행을 유지하고 range/weight를 새 종류에 맞춘다. Weighted 종류 사이에서는 weight를 유지한다. 이전 종류의 range를 숨은 이력으로 보존하지 않는다. Collapse는 AST 밖의 store에 있다. 미완성 숫자 편집을 유지하고 잘못된 숫자는 경고하며 validation 요청과 추천 숫자 표시를 중지한다. 음수와 0 가중치는 허용하고 입력을 임의 clamp하지 않는다.

## 검증 및 독립 demo

`src/features/crafting/goal-filter/demo.html`은 별도 Vite 진입점이다. 실제 앱의 App/CraftSupport를 수정하지 않고 합성 계약 UI를 확인할 수 있다.

전용 unit/component 테스트는 공통 fixture validation 9건, 6종 그룹 전환, 중복 후보 방지, 빈 그룹·stale catalog, collapse AST 분리, 미지원 base 변경 후 행 보존, 추가/삭제 focus, HTTP wire shape와 AbortSignal, 늦은 catalog 응답 폐기, 미완성 숫자 편집을 검사한다. Docker typecheck/lint/build/format 및 브라우저 결과는 최종 인계에 별도로 기록한다. Backend 검사와 전체 기존 frontend unit suite는 이번 독립 소유 범위 검사에 포함하지 않았다.
