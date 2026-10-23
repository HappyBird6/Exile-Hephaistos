# 자동 제작 티어 분포 및 방법 표시 수정 — 2026-10-22

> 최신 상태(2026-10-23): **Ctrl+휠 그래프 줌**과 **좌클릭 그래프 이동**을 QA 프런트에 반영했다. 백엔드는 아래 **2차 최종 QA 반영 및 검증** 버전을 유지한다. 이전 1차/중단 기록은 이력으로 보존했다. QA 18081/18080 실행 중, 8081 종료 유지.
기반: `codex/automatic-refinements-20261021`, `84b94438b6c2f56f5688a17635e1d020aa35b482`. 이번 변경은 아직 커밋하거나 원격 push하지 않았다.

## Ctrl+휠 그래프 줌 — 2026-10-23

- `useGraphWheelZoom.ts`를 그래프 뷰포트에만 연결했다. 로컬 native wheel 리스너를 `{ passive: false }`로 등록하고 Ctrl 입력만 `preventDefault`한다. 일반 휠과 영역 밖 이벤트는 취소하지 않는다. 기존 10%~200% 한도에서도 Ctrl+휠은 소비해 페이지 줌으로 빠져나가지 않는다.
- 픽셀/줄/페이지 delta를 정규화하고 휠 방향·크기에 따라 연속 배율을 계산한다. SVG 크기 변경 후 포인터 아래 좌표가 유지되도록 스크롤을 보정한다. 콘텐츠 가장자리의 native 스크롤 한계에서는 가능한 범위까지 보정한다. 같은 React commit 이전에 여러 휠이 들어와도 누적하며 반대 방향 입력으로 상쇄되면 남은 기준점을 제거한다. 줌 버튼, 데이터, 제작 상태는 기존 체계를 유지한다.
- 집중 검사 33개 통과 후 전체 lint에서 DOM 필드 직접 갱신 지적을 받아 `scrollBy`로 변경했다. 테스트 mock의 optional 타입도 바로잡았다. 최종 **프런트 2343/2343**, lint/typecheck/format/build 및 `git diff --check` 통과. 신규 줌 회귀 10개를 포함한다. Vite 기존 번들 크기 경고는 유지된다.
- 교체 전후 실제 격리 Chrome에서 Ctrl+휠 확대/축소, 포인터 기준점, 10%/200% 경계, 일반 휠, 영역 밖 이벤트 등 **5항목**을 확인했다. 100%→122%에서 페이지 DPR=1, innerWidth=1600, visualViewport.scale=1 불변, 좌표 오차 X=0.56px/Y=0.45px. 그래프 텍스트와 localStorage도 같다. 패닝·영역 밖 release·노드 선택·티어 펼침·방법 상세·줌/리셋·일반 휠·키보드 이동 **8항목**도 실제 Chrome에서 통과했다. 영역 밖 Ctrl+휠은 `defaultPrevented=false`이고 그래프 배율이 바뀌지 않는 것을 확인했지만, headless Chrome/CDP 입력에서는 브라우저 자체 페이지 줌이 발생하지 않아 실제 하드웨어 입력에 의한 영역 밖 페이지 확대는 미확인이다. 이를 통과했다고 주장하지 않는다.
- 테스트 소스 **318개**, 이미지 정적 파일 **227개** 일치. QA18081 프런트만 교체했다. 이미지 `sha256:772a95b74aa71a9d67e18008b20de2d794df938c448ee309eb9d17924e6bdf9e`, JS `index-DeeBkua3.js`, CSS `index-Bi14gc7Z.css`. 기반 HEAD `84b94438b6c2f56f5688a17635e1d020aa35b482`, 미커밋 소스 fingerprint `41bd888becdf821d536c9c7f19929b30a174b2b1b9ebf61dee3a9fa85ac6cdc6`.
- 교체 후 UI/정적자산/registry/readiness 등 HTTP **9항목** 통과. backend18080·DB·Redis와 기존 8080/8081 컨테이너는 ID·이미지·상태·마운트가 그대로다. 설정 해시 불변. 시작 시 저장 23개가 전부 바이트 보존되었고, 작업 중 새 기록 5개가 추가되어 마지막 관찰 총 28개다. 기록을 삭제·변경하지 않았다. 8081 종료 유지, QA18081 실행 유지. 기존 47개 변경을 보존해 현재 49개 미커밋 파일이며 commit/push는 하지 않았다.
- 증거: 작업 루트 `evidence/wheel-qa/results-final.xml`, `browser-live-final.json`, `pan-browser-live-final.json`, `live-graph.png`, `final-proof.json`, `tested-source.json`. 이번 프런트 수정은 백엔드 전체 검사와 무관하며 기존 06 실패와 반지 복합 수치 미지원은 별도 후속 작업이다.

## 좌클릭 그래프 이동 — 2026-10-23

- 그래프 빈 배경의 마우스 좌버튼 드래그로 가로·세로 스크롤 위치만 바꾼다. 카드·텍스트·후보 펼침·버튼·링크·폼·SVG 경로에서는 패닝을 시작하지 않는다. 터치·다른 마우스 버튼·수정키 입력과 기본 스크롤바를 가로채지 않는다. 기존 줌·휠·키보드 이동을 유지한다.
- `useGraphPan.ts`에 포인터 추적/캡처, 영역 밖 놓기, 취소, 캡처 손실, 창 포커스 손실, 놓침 복구와 정리 처리를 추가했다. 작은 움직임은 클릭으로 남기고 드래그 직후의 클릭만 억제한다. `GroupedMethodGraph.tsx`에 연결하고 `method-search.css`에 grab/grabbing 커서를 추가했다. 제작/저장 상태와 백엔드 계산 코드는 바꾸지 않았다.
- 초기 신규 검사 6개 실패에서 포커스를 잡는 중 발생한 blur가 드래그를 취소하는 문제를 확인했다. 포커스를 먼저 확립한 뒤 포인터 추적을 시작하도록 수정했다. 기대값 완화 없이 최종 신규 10개를 포함한 **프런트 2333/2333**, lint/typecheck/format/build 통과. 잠금 파일이 같은 이전 검증 Node 이미지의 의존성을 재사용했고 Compose 빌드에서도 고정 Node와 `npm ci` 캐시를 사용했다. 기존 Vite 단일 번들 크기 경고는 남아 있다.
- 실제 격리 Chrome/CDP에서 빈 배경 좌드래그로 scrollLeft 80→200, scrollTop 29→0을 확인했다. 영역 밖 release 후 끌림 해제, 노드 선택, 티어 펼침, 방법 상세 버튼, 줌/리셋, 기본 휠·화살표키 이동까지 **8항목 통과**. 드래그 전후 그래프 텍스트·후보 수·localStorage가 같다. 실제 화면 PNG도 열어 확인했다. 초기 브라우저 검사에서는 CDP 이동 인자의 button 상태가 빠져 캡처를 잃었으며, 누른 좌버튼 상태를 정확히 전달한 뒤 정상 검증했다.
- 테스트 소스 **316개**가 호스트와 같고 이미지의 정적 파일 **227개**가 테스트 빌드와 같다. QA 18081만 `up -d --no-deps --no-build frontend`로 교체했다. 새 이미지 `sha256:00a1ca712b3b2bbd8db4a2b95d1aec009b46ed1995e09f8f9e131cf84e20e0e1`, JS `index-BBxk5ol_.js`, CSS `index-Bi14gc7Z.css`. 기반 HEAD는 여전히 `84b94438b6c2f56f5688a17635e1d020aa35b482`이며 미커밋 소스 fingerprint는 `06ed2d0aff9b854aa92c59ec1310258d890765441f048e6304608e64719b05f6`이다.
- 교체 후 실제 Chrome 8항목과 UI/정적 자산/registry/readiness 등 **HTTP 9항목** 통과. QA 백엔드 `18080`과 PostgreSQL/Redis, 기존 8080/8081 컨테이너의 ID·이미지·상태·마운트가 교체 전과 같다. 시작 시 저장 파일 **23개 모두 바이트 보존**, 설정 파일 해시도 같다. 8081은 닫혀 있고 QA18081은 켜 두었다. 원격 push/commit 없음. 기존 44개 변경은 유지했고 이번 추가 후 총 47개 미커밋 파일이다.
- 증거는 작업 루트의 `evidence/pan-qa/results-final.xml`, `browser-live-final.json`, `live-graph-pan.png`, `final-proof.json`, `tested-source.json`에 보존했다. 이번 프런트 입력 변경으로 백엔드 전체 검사를 다시 실행하지 않았다. 아래 기록의 기존 06 관찰 예산 실패와 일부 ring 경로 미지원은 해결했다고 주장하지 않는다.

## 원인과 구현

이전 제왕 정렬은 상세 `OperationCards`에만 적용되었다. 그래프 `stateProjection`은 징조를 포함한 전체 action을 edge 키로 사용해 일반/좌측/우측을 동급 경로로 표시했고, 엑잘은 공통 정렬 대상에 없었다. 실제 기존 QA 정상 목걸이→희귀 생명력 목표 응답에서 일반 제왕 10.60%, 좌측 17.72%, 우측 5.33%, 확정 전략 좌측이었는데 그래프가 이를 기본 표시로 연결하지 않았다.

- 제왕/엑잘 작업 묶음과 정렬을 상세 카드, 그래프, 후보 경로에 공통 적용했다. `ACTION_CERTIFIED` 전체 경로 추천을 우선하고, 그 외 완료된 전체 방법의 목표 확률 구간을 비교한다. 반올림이 같은 값이나 겹치는 확률 구간을 우열의 근거로 삼지 않는다.
- 완전하고 동일한 후속 결과 법칙이 확인되면 일반 화폐를 기본으로 표시한다. 다른 후속 결과를 가진 방법 및 사용자 선택은 상세 대안으로 보존한다. 수동 Workbench 기능은 제거하지 않는다.
- 카오스 준비용 제왕/엑잘은 기존 v4 정책대로 징조 없는 단일 준비 방법을 유지한다. 일반 목표 획득과 준비 목적은 별개다.
- 그래프의 개별 조건부 상태/도착 대안은 접힌 상세로 옮기고, 기본 카드에는 공통 속성을 표시한다.

이전 자동 제작 그래프는 `ItemState`별 노드를 저장했다. 별도 기본 화폐 탐색기의 `TierCell`은 자동 제작 실행에 연결되지 않았다. 이제 자동 제작의 실제 실행 상태, 노드 식별자, 저장 체크포인트가 `Map<String, TierCell>`을 보유한다. 노드 API의 `tiers`는 각 구간의 modifier ID, stat, min/max, 정확한 유리수 조건부 확률을 문자열로 제공한다. 표시만 묶은 변경이 아니다.

- `AutomaticCraftingKernel.expandDeferred`는 실제 분모와 가중치를 합산한다. 기존 속성의 분포는 유지하고, 제거된 속성의 분포만 버리며, 제거 후 다시 뽑은 속성에는 새 분포를 부여한다.
- 동일 속성이라도 목표 경계, 화폐 후보 자격, 태그/계열, 고정 초기값의 의미가 다르면 조건부 구간을 나눈다. 무조건 모든 티어를 하나의 대표 티어로 대체하지 않는다.
- 현재 인증된 묶음은 기존 `RollPartition`이 제공하는 Solar Amulet의 단순하고 안전한 목표 구간을 사용한다. 가중합 목표, 서로 합쳐 임계값을 넘는 기여, 검증되지 않은 기본 아이템/규칙 등에는 정밀 상태를 유지한다. 따라서 모든 장비·모든 목표에서 상태 수가 반드시 감소한다고 주장하지 않는다.
- 서로 다른 조건부 법칙은 노드/실행 식별자가 다르다. 별도 경로의 주변분포를 곱해서 상관관계를 지우지 않는다. 합칠 근거가 없는 경우 혼합물의 가지를 보존한다. 절사 징조를 사용하는 과거 정책은 레벨별 제거가 달라질 수 있어 이 묶음을 활성화하지 않는다.
- 추가만 남은 종료 구간의 기존 계산 공유도 유지한다. 공유 실행의 새 속성 분포와 실제 출발 상태의 기존 분포를 각각 전달한다. 초기 구현에서 이 공유를 제외해 생긴 4목표 완료성 회귀를 집중 검사로 발견·수정했다.
- 상태가 확정 아이템이 아닌 경우 대표 최소 티어를 작업대에 확정값으로 복사하지 않는다. 구체 아이템을 선택해 편집하도록 6개 언어 안내를 표시한다.
- 그래프 버전은 `goal-directed-milestone-graph-v11-tier-laws`, 실행 버전은 `automatic-method-execution-v5-tier-laws`다. v10/v3, v9, v8 결과는 원본 바이트를 보존해 읽기 전용으로 제공한다. 새 제작만 새 구조를 계산한다. 내보내기·불러오기는 조건부 분포와 정확한 큰 정수 문자열을 보존한다.

## 검증 중간 기록

- 첫 Java 컴파일 통과.
- 집중 검사 r2: 22개 통과. 실제 v8 및 v10/v3 원본 기록, 기존 kernel/실행 회귀, 신규 조건부 분포 및 저장 복원 포함.
- 신규 `AutomaticTierLawTest`: 원래 scalar 전이 전체와 정확히 비교. 생명력 목표 1/100은 209→95, 120은 209→96 전이 항목. 추가/제거 후 전체 결합법칙도 원래 전이로 재구성해 동일함을 확인했다.
- 신규 `AutomaticTierRenewalTest`: 두 속성의 실패 상태가 바뀌는 100회 카오스 최초 성공 법칙을 독립적인 유리수 마르코프 계산으로 비교, 확률 구간 포함 및 총질량 1을 확인했다. 단순 IID 공식을 가정하지 않았다.
- 백엔드 전체 r1: unit 891개 중 3개 실패. 기존 06/07과 새 종료 분기 회귀 한 건. 기존 05는 이 실행에서 통과했다. Docker integration 8개, jOOQ, bootJar 수행 결과는 XML/로그에 보관한다.
- 종료 분기 회귀 수정 후 집중 검사 `exile-tier-after-share-r1`: 통과. 네 목표에서 계산 상태 19/64, 실제 실행 22/128, 노드 210, 표시 방법 93, 레코드 506/20000, 197 연산, finished/complete=true. 원래 성공 단언이나 한도는 변경하지 않았다.
- 프런트 전체 r2: lint/typecheck/format 통과, 2305개 중 신규 표시 테스트 1건 실패. 원인은 기존 유리수 표시가 `≈66.66666%`인데 테스트가 `66.67%`를 기대한 것. 표시 정책을 바꾸지 않고 기대값을 실제 기존 형식에 맞췄다. 이후 최종 구간 표시 및 문자열 검증 보강까지 포함한 r3 전체 검사를 실행 중이다.

현재 전체 backend r2, frontend r3가 실행 중이다. 이 문서 작성 시점에는 QA 18080/18081을 교체하지 않았다. 기존 8081은 다시 시작하지 않으며, 8080/DB/Redis/볼륨/설정과 사용자 저장 결과를 보존한다. 새 소스를 검증·빌드한 뒤 QA만 교체하고 실행 파일·정적 자산·API 및 readiness를 확인해야 한다.

실제 브라우저 조작 도구는 이번 세션의 도구 목록에 노출되지 않아 시각 검증은 미실시다. DOM 회귀 및 실제 API 응답 검증과 구분해 보고한다. 신규 원격 push는 요청되지 않았다.

## 최종 체크포인트 — QA 반영 완료

- 최종 backend 전체 r2: unit **891개 중 889 통과, 2 실패**. 실패는 기존 `06-tier-preparation`, `07-rarity-preparation`뿐이다. Docker 통합 **8/8**, ArchUnit, formatter, `generateJooq`, `bootJar` 통과. 전체 `check`는 두 기존 실패 때문에 성공으로 보고하지 않는다.
- 기존 05: finished/complete=true, 53,199 연산, retained 203, goal nodes 9. 이번 최종 소스에서 완료 단언을 통과했다.
- 06: 기본 레코드 한도에서 20,055/20,000, operations 40,711, methods 2, goal nodes 324, limited=true, incomplete. 이번 구조에서 정상 최대 50,000 재개 후 결과는 **미실시**이며 과거 v4 수치를 현재 값으로 대입하지 않는다.
- 07: 120,000 연산 관측 경계에서 incomplete, limited=false, retained 8,788, methods 1, goal nodes 115. 자원 최대치에 막혔다고 주장하지 않는다. 이번 구조의 50,000/500,000 단계 수동 프로브는 미실시다.
- 최종 frontend r3: **2,305/2,305 통과**, lint/typecheck/format/build 통과. 실제 QA 응답을 이용한 별도 DOM 검사 **3/3**: 제왕 기본 추천, 그래프의 결과 묶음당 동급 action 한 개, 분포 상세 기본 접힘, 일반 엑잘의 추천 징조와 카오스 준비용 일반 엑잘 한 방법을 확인했다.
- QA 교체 전 검증 JAR와 이미지 JAR의 **1,343개 엔트리 내용 전체 동일**, 정적 자산 **227개 전체 동일**. 추가 nginx 기본 `50x.html`도 이전 실행 이미지와 동일하다. ZIP 메타데이터 차이와 실제 엔트리 내용 차이를 구분했다.
- QA frontend: `http://localhost:18081`, backend: `http://localhost:18080`. 실제 번들 `index-DEaZtJAF.js`. source fingerprint `5627721b6f099a22ace4b38908cd314bb6d6199c2fda411a9aeef9d9a4100136`의 미커밋 변경본이며 HEAD `84b94438b6c2f56f5688a17635e1d020aa35b482` 자체와 같다고 주장하지 않는다.
- 이미지: app `sha256:25c13d36d15b64d3ac811e7c06273c3a762c9bdedb9d32047a24ad77fe6570e2`, frontend `sha256:e20bdca4ec4738e887ac3ef6948a452d6320b8cae3f68b88ae15ac199e1fd6cb`.
- UI HTTP/admin/정적 파일, health/readiness/liveness, registry/initial/catalog/provenance identity, goal-filter validate, 신규 정상 목걸이/희귀 아이템 계산 및 원본 요청 조회: **23건 기대 상태 통과**. 기존 ruleset identity와 각 응답 header/body가 일치한다.
- 정상 목걸이→희귀 생명력 목표: COMPLETE, 기존 1,801개였던 결과 노드는 **611개**, 그중 **306개가 실제 티어 분포**. 저장된 v11 노드에도 같은 분포가 존재한다. 일반 제왕 10.60493%, 좌측 찬미 17.72449%, 우측 찬미 5.32999%; 확정 전략 및 기본 카드가 좌측 조합을 선택한다. 대안의 확률을 바꾸거나 강제로 같은 값으로 합치지 않았다.
- 희귀 빈 아이템: COMPLETE, nodes 4, distribution nodes 3, methods 2. 목표 획득용 엑잘+좌측 찬미와 **징조 없는 엑잘→카오스**를 구분한다.
- 기존 QA 저장 파일 **11개 바이트 모두 동일**, 설정 파일 SHA 동일, DB/Redis 컨테이너 ID와 볼륨 동일. 기존 8080의 정지 상태 및 8081 종료/포트 닫힘을 유지했다. 서비스는 QA 두 개만 교체했다. 원격 push/커밋은 하지 않았다.
- 검증용 계산 3건이 추가됐다. 첫 검증 스크립트가 지원하지 않는 `SLICE`를 보내 422 `INVALID_COMMAND`를 받았다. 제품 결함이 아니라 검사 명령 오류였으며 실제 `ADVANCE`로 바로잡아 두 신규 계산을 검증했다. 첫 신규 READY 기록은 소유자 쿠키를 보관하지 못해 API 취소하지 못했고 그대로 보존했다. 요청 기반 실행이라 배경 계산은 없다. 기존 기록을 삭제하거나 인증을 우회하지 않았다.

### 남은 범위

이번 반영은 **안전성이 확인된 조건부 티어 구간을 자동 제작 실행/저장에 연결한 1차 구조 변경**이다. 모든 장비·모든 목표의 가능한 티어 전체를 언제나 한 개의 범용 결합분포 상태로 합성한 완성본은 아니다. 기존 `TierPartition`의 생성 화폐 자격 구간/목표 구간은 여전히 분리되며, 인증되지 않은 가중합·다중 기여 등은 정밀 상태를 유지한다. 동일 속성의 서로 다른 생성 자격 구간까지 하나의 사후 분포로 합성하고, 서로 다른 속성의 상관관계를 가진 범용 혼합 상태를 계산/추천/저장까지 전달하는 후속 작업이 남았다. 이를 단순 UI 묶음이나 반올림 확률 동일성으로 대신하지 않는다. 현재 구조의 실제 감소 수치와 남은 한계를 구분한다.

외부 증거는 task의 `evidence/tier-source-final.json`, `tier-backend-full-r2-tests`, `tier-backend-full-r2-integration`, `tier-frontend-full-r3.xml`, `tier-after-share-r1-tests`, `tier-qa/build-proof.json`, `tier-qa/live-proof.json`, `tier-qa/live-ui.xml`에 있다. 개인 설정·실제 저장 파일·검증 쿠키·빌드 산출물은 저장소 변경에 넣지 않는다. 브라우저 시각 검증 도구는 제공되지 않아 스크린샷/실제 배치 확인은 미실시이며 DOM/API 검증과 구분한다.


## 작업 중단 및 내일 재개 체크포인트 — 2026-10-22 09:00 UTC

사용자의 작업 종료 요청으로 2차 구현과 검증을 여기에서 중단했다. 추가 구현·검사·배포·커밋·push를 시작하지 않는다. 이어서 사용자가 NTU 컴퓨터의 정상 종료를 요청했으므로 소스와 본 체크포인트 저장 후 강제 옵션 없이 종료를 시도한다. 기존 기록·DB·볼륨·설정을 삭제하지 않는다.

### 보존된 작업과 실제 실행 버전

- 작업 경로: `C:\SSAFY\PYJ\Codex\2026-10-21\task\exile-refinements`
- 브랜치: `codex/automatic-refinements-20261021`, HEAD 및 마지막 확인 원격: `84b94438b6c2f56f5688a17635e1d020aa35b482`. 1차/2차 변경은 미커밋 상태로 디스크에 보존했다. 이번 작업에서 push하지 않았다.
- **2차 결합분포 수정은 미배포**다. 실제 QA는 아래 1차 빌드를 실행하고 있었다. OS 종료 시에는 서비스도 내려가며 내일 재기동이 필요하다.
- QA frontend `http://localhost:18081`, backend `http://localhost:18080`; JS `index-DEaZtJAF.js`, graph `goal-directed-milestone-graph-v11-tier-laws`, execution `automatic-method-execution-v5-tier-laws`.
- 실제 이미지: app `sha256:25c13d36d15b64d3ac811e7c06273c3a762c9bdedb9d32047a24ad77fe6570e2`, frontend `sha256:e20bdca4ec4738e887ac3ef6948a452d6320b8cae3f68b88ae15ac199e1fd6cb`.
- 8081은 사용자가 종료한 상태, 8080도 기존에 내려가 있던 상태를 보존했다. OS 종료 전까지 이번 2차 작업은 서비스를 교체하지 않았다. DB/Redis 컨테이너·볼륨·설정은 보존한다.
- 08:42 연결 알림 후 실제 연결, 파일 복사 완료, QA readiness UP, 컨테이너 실행을 직접 확인했다. 알림 시 불확실한 배포/쓰기 명령은 없었으며 재배포하지 않았다.

### 2차 구현 완료, 추가 검증 필요

- `AutomaticTierPartition`: 실제 선택 풀마다 조건부 티어 가중치를 만든다. 생성 최소 레벨 구간이 다르다는 이유만으로 이미 생성된 속성을 다른 상태로 나누지 않는다. 원래 고급 화폐 자동 탐색 제외 정책은 그대로다.
- `AutomaticMethodGraph`: 동일 후속 동작이 보장되는 속성/목표 관측 클래스를 실제 하나의 노드·방법 집합으로 합친다. 단순 UI 묶음이 아니다. 목표에 민감한 숫자 구간과 후속 확률이 다른 조건은 분리한다. 종단 전이 동등성 증명도 재사용한다.
- 결합분포 DAG: `members`/`attributes`/`joint`와 전이의 `conditional.local`, `retainedFromSource`, 각 확률 구간으로 서로 연관된 원래 좌표를 보존한다. 독립 주변분포의 곱으로 불가능한 조합을 만들지 않는다. 실제 시작 아이템은 고정값, 후속 분포 노드는 원래 변수의 투영으로 구분한다.
- 프런트: 속성명 한 번 + T별 숫자/범위 표. 동일한 정확 확률 구간·동작의 시작 조건은 기본 그래프에서 한 번 표시하고 원래 조건은 접힌 상세에 보존한다. 반올림 수치만 같은 조건은 같다고 합치지 않는다. 수동 사용 선택은 유지한다.
- 새 저장 코드의 결합분포와 투영, 기존 동결 카드 표시, 기존 v11/v10/v8 읽기 전용 경로를 연결했다. v11 실제 기록 API 재확인은 아직 남았다.
- 만료 기록 생성 시 디스크 삭제를 중단하는 보존 수정이 포함되어 있다. TTL 접근/작업 admission과 저장 상한은 유지한다. 기존 구현이 새 생성 시 만료 파일을 삭제하므로 **1차 QA에 새 작업 생성 검사를 하면 안 된다**. 새 구현 검증·반영 뒤에 하거나 격리 복사본으로 먼저 확인한다. 현재 보존 복사본에는 15개 파일이 있다.

### 검증 결과 — 통과/실패/미실시 구분

- 최종 프런트 전체 `exile-joint-frontend-full-r2`: **2,320/2,320 통과**, lint/typecheck/format/build 모두 통과. 실제 결합분포 압축 코드 왕복·질량 훼손·잘못된 투영 거부 검사 포함. 새 JS 빌드 `index-BaHjp4bj.js`, CSS `index-CJGBIiC-.css`는 검증 산출물이며 배포하지 않았다.
- 이전 집중 `joint-focused-r1`: 22 통과. `joint-law-r5`: 19 중 18 통과, 반지 1건은 이미 원시 단계에서 하나로 묶인 결과를 테스트가 2개 이상 조건부 입력으로 잘못 기대한 실패다. 최종 소스는 **1 입력 분포 안에 2 티어, 1 실제 상태**를 기대하도록 수정했으나 중단으로 최종 재검증하지 못했다. 완료/확률 단언을 완화하지 않았다.
- 가중 복합 목표 검사: `(a,b)=(1,4),(4,1),(4,4)`의 결합 질량 각 2/15, 합 2/5; 불가능한 (1,1)은 없음. 한 상태에서 다음 C 추가 확률 1, 원래 A/B 쌍 투영 보존. 미래 성공이 1/2 대 1로 달라지는 A/C 조건은 별도 상태로 유지. JSON 체크포인트 복원도 통과했다(r5).
- 원시 전이 scalar 대 분포 전개 일치, 실제 실패 상태를 반영한 카오스 100회 최초 성공, 복원, TTL 파일 보존 검사는 r5에서 통과했다. 그 뒤 반지 테스트 기대값만 수정되었다.
- 최종 백엔드 전체 `exile-joint-backend-full-r1`: compileJava/compileTestJava/**bootJar 완료** 후 단위 검사 진행 중 사용자 요청으로 종료했다. 종료 전 기존 **06-tier-preparation 실패**가 확인되었다. 완료 XML이 없으므로 전체 실패 수를 확정할 수 없고 `check` 통과로 주장하지 않는다. Docker stop의 30초 유예 후 테스트 컨테이너 exit137, 사용자 데이터/QA 컨테이너는 이 중지 대상에 포함되지 않았다.
- 최종 전체의 integrationTest/ArchUnit/spotlessCheck/generateJooq는 실행 완료를 확인하지 못했다. 1차 결과는 이전 단락(891 중 2 실패, 통합 8/8)과 구분한다. 07 및 최종 반지 검사 결과도 새 전체 결과로 확정하지 않는다.
- 앞서 저장 코드 집중 1실패는 테스트가 dyadic 구간 대신 2/15 같은 임의 분수를 Bounds로 입력한 형식 오류였다. 실제 Bounds 형식으로 고친 뒤 최종 프런트 전체에서 통과했다. 제품 직렬화 회귀로 결론내리지 않는다.
- **미실시:** 06/07 정상 자동 이어 계산 최대 예산 재검사, 2차 실제 QA API/DOM(특히 Magic empty → plain Regal 준비 → Chaos), 새 이미지/전체 검사 산출물 일치 검증, 2차 QA 교체.
- 브라우저/CUA 및 NTU Library materialization 도구가 노출되지 않아 실제 시각 검증/첨부 로컬 픽셀 확인은 미실시다. 부모는 두 이미지 픽셀을 확인했고 그 설명을 수용 기준으로 사용했다. 권한 거부를 우회한 것은 아니다.

### 바로 다음 재개 순서와 명령

1. NTU 기동 후 본 경로/브랜치/디스크 변경과 Docker 상태를 먼저 확인한다. 실행 중 명령은 현재 없다. 8081을 켜지 않는다. QA를 켤 때는 **기존 1차 이미지**를 우선 사용하며 새 소스 자동 build/교체를 하지 않는다.
2. `check-joint-law-final.sh`의 마지막 Legacy 클래스명은 잘못된 패키지다. 실제는 `com.poe2craft.bootstrap.LegacyAutomaticResultReaderTest`; 재사용 전 스크립트를 바로잡는다. 최종 반지 1건과 결합분포 집중 검사를 실행한다. `check-backend.sh`로 전체 check/generateJooq/bootJar를 완료하고 실패별 원인을 확인한다. 같은 Gradle 캐시 볼륨을 쓰는 JVM을 동시에 돌리지 않는다.
3. `check-grade-remaining.sh`는 기본 한도에서 정상 RESUME 규칙으로 128/256/50000/10000 상한까지 관찰하는 격리 검사다. 06/07 개선 여부를 확인하되 한도를 늘리거나 실패 단언을 지우지 않는다. 500000 단계/180초 관찰 중단이면 제품 절대 한도와 구분한다.
4. 프런트 소스를 더 변경하지 않았으면 위 2320 통과 결과를 재사용한다. 수정 시 필요한 검사만 다시 하고 최종 전체 범위를 명시한다.
5. 현재 15개 보존 파일과 `.env`/override/DB·Redis ID·마운트를 재조회한다. `joint-qa-prepare.py`는 작성만 했고 **미실행**이다. 현재 1차 이미지 ID를 단언하고 새 `saved-predeploy`에 복사/rollback 태그를 만드는 스크립트다. 같은 경로가 이미 있으면 불확실한 복사를 재시도하지 말고 먼저 확인한다.
6. 전체 검증 후 QA app/frontend만 새 이미지로 build하고 테스트 JAR 엔트리·정적 파일 일치를 확인한다. 이어 v11 실제 기록 읽기 전용/원본 요청, Magic empty Regal 준비/Chaos, Normal 전환, 실제 UI DOM, property 1회와 T별 수치, 정확 확률 중복 제거를 검증한다. 실제 브라우저가 없으면 한계를 명시한다.

예시(작업 루트 PowerShell, 기존 `.env` 내용은 출력하지 않음):

```powershell
git -C exile-refinements status --short
docker ps -a --format '{{.Names}} {{.Status}}'
# 필요할 때 기존 QA 이미지 재기동. build/down/-v 금지.
docker compose -p exile-refinements-20261021 --env-file exile-qa.env -f exile-refinements/infra/compose.yaml -f exile-qa.override.yaml --profile stack up -d --no-build postgres redis app frontend
# 격리 백엔드: 새 이름 사용, 기존 결과 컨테이너는 보존.
docker run --name exile-joint-backend-resume-r1 --mount type=bind,source=C:\SSAFY\PYJ\Codex\2026-10-21\task\exile-refinements,target=/source,readonly --mount type=bind,source=C:\SSAFY\PYJ\Codex\2026-10-21\task,target=/checks,readonly --mount type=volume,source=exile-refinements-gradle-20261021,target=/root/.gradle --mount type=bind,source=/var/run/docker.sock,target=/var/run/docker.sock -e DOCKER_HOST=unix:///var/run/docker.sock -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal -w /workspace eclipse-temurin:21-jdk-jammy@sha256:c7d5863b5dd8f26b90c64f1d80cc2b0e5a5e4642f8db9955a370d348edd8f438 sh /checks/check-backend.sh
```

위 실행은 재개 후 현재 상태를 확인한 뒤 사용한다. 정상 컴퓨터 종료로 Docker/서버가 내려가는 것은 이번 사용자 요청에 따른 것이며 데이터 삭제가 아니다. 종료 명령 수락과 실제 전원 차단 확인은 구분한다.

### 증거 및 변경 파일

task의 `evidence/joint-frontend-full-r2.xml`, `joint-tested-dist`, `joint-law-r5-tests`, `joint-focused-r1-tests`, `joint-qa/saved-before`, `joint-stop-checkpoint.json`, `joint-stop-source.json`에 보존했다. 종료한 backend 컨테이너에도 산출물/로그가 남아 있다. 비밀·쿠키·실제 저장 파일은 저장소에 추가하지 않았다. 1차 전체 소스 ZIP `evidence/tier-v11-source.zip`과 1차 QA 검증 증거도 유지했다.

현재 변경/신규 파일(1차 및 2차 누적):

- `backend/src/main/java/com/poe2craft/crafting/domain/pathsearch/AutomaticTierPartition.java`
- `backend/src/test/java/com/poe2craft/bootstrap/AutomaticTierLawTest.java`
- `backend/src/test/java/com/poe2craft/crafting/application/pathsearch/JointAttributeLawTest.java`
- `backend/src/test/java/com/poe2craft/crafting/domain/AutomaticEquipmentTierLawTest.java`
- `backend/src/test/java/com/poe2craft/crafting/domain/AutomaticTierRenewalTest.java`
- `backend/src/test/java/com/poe2craft/crafting/domain/JointLawFixture.java`
- `docs/automatic-tier-distribution-2026-10-22.md`
- `frontend/src/features/crafting/method-search/arrivalProbabilities.test.ts`
- `frontend/src/features/crafting/method-search/arrivalProbabilities.ts`
- `frontend/src/features/crafting/method-search/automaticTierLaws.test.ts`
- `frontend/src/features/crafting/method-search/automaticTierLaws.ts`
- `frontend/src/features/crafting/method-search/compactAttribute.test.tsx`
- `frontend/src/features/crafting/method-search/compactAttribute.ts`
- `frontend/src/features/crafting/method-search/compactFrozenCard.ts`
- `frontend/src/features/crafting/method-search/jointValidation.ts`
- `backend/src/main/java/com/poe2craft/crafting/application/pathsearch/AutomaticSearchService.java`
- `backend/src/main/java/com/poe2craft/crafting/application/pathsearch/LegacyAutomaticResultReader.java`
- `backend/src/main/java/com/poe2craft/crafting/application/pathsearch/SavedAutomaticResultReader.java`
- `backend/src/main/java/com/poe2craft/crafting/domain/AutomaticCraftingKernel.java`
- `backend/src/main/java/com/poe2craft/crafting/domain/pathsearch/AutomaticMethodExecution.java`
- `backend/src/main/java/com/poe2craft/crafting/domain/pathsearch/AutomaticMethodGraph.java`
- `backend/src/main/java/com/poe2craft/crafting/domain/pathsearch/RollPartition.java`
- `backend/src/test/java/com/poe2craft/bootstrap/TerminalBranchingScenariosTest.java`
- `backend/src/test/java/com/poe2craft/crafting/domain/AutomaticSearchServiceTest.java`
- `frontend/src/features/crafting/ItemCard.tsx`
- `frontend/src/features/crafting/item-card.css`
- `frontend/src/features/crafting/itemCardData.ts`
- `frontend/src/features/crafting/method-search/AutomaticMethodSearch.tsx`
- `frontend/src/features/crafting/method-search/CandidateRoute.tsx`
- `frontend/src/features/crafting/method-search/GroupedMethodGraph.tsx`
- `frontend/src/features/crafting/method-search/OperationCards.test.tsx`
- `frontend/src/features/crafting/method-search/OperationCards.tsx`
- `frontend/src/features/crafting/method-search/ScenarioCodePanel.tsx`
- `frontend/src/features/crafting/method-search/SearchResources.tsx`
- `frontend/src/features/crafting/method-search/automaticApi.ts`
- `frontend/src/features/crafting/method-search/commonStateCard.ts`
- `frontend/src/features/crafting/method-search/graphNavigationMessages.ts`
- `frontend/src/features/crafting/method-search/operationGrouping.ts`
- `frontend/src/features/crafting/method-search/scenarioCode.test.ts`
- `frontend/src/features/crafting/method-search/scenarioCode.ts`
- `frontend/src/features/crafting/method-search/stateProjection.test.tsx`
- `frontend/src/features/crafting/method-search/stateProjection.ts`

컴퓨터 종료 준비: QA app/frontend/PostgreSQL/Redis를 무기한 정상 종료 대기(docker stop --timeout -1, 강제 kill 없음)로 정지했다. 컨테이너/볼륨/파일 삭제 없음. 8080/8081은 기존 종료 상태를 유지했다. 다음 동작은 /f 없는 shutdown.exe /s /t 0 정상 OS 종료 요청이다.

## 2차 최종 QA 반영 및 검증 — 2026-10-23

### 실행 소스와 서버

- 작업 디렉터리 `C:\SSAFY\PYJ\Codex\2026-10-21\task\exile-refinements`, 브랜치 `codex/automatic-refinements-20261021`, HEAD `84b94438b6c2f56f5688a17635e1d020aa35b482` 기반 **미커밋 변경본**이다. HEAD 자체와 실행 파일이 같다고 주장하지 않는다. 이번 재개에서 commit/push/master 병합/force reset은 하지 않았다.
- 실행 소스 fingerprint: `8ae2a50f1c8067389fe664c7d8cc2e4e68b38438794138ea6746b7823d5ba465` (코드·설정·테스트·계약·빌드 관련 1,777개 파일). 최종 문서는 이 지문 범위 밖이며 코드 지문을 바꾸지 않는다.
- QA frontend `http://localhost:18081`, backend `http://localhost:18080`. 최종 JS `index-DfDW4fXI.js`, CSS `index-CJGBIiC-.css`.
- graph `goal-directed-milestone-graph-v12-joint-attribute-laws`, execution `automatic-method-execution-v6-joint-lineage`.
- ruleset `reviewed-equipment-20261009-v2-debb3f07052d54419a4732f49d9febf78508e23cd69a489698c5e81be80597f7`. 이번 구조 변경으로 제작 가중치·게임 규칙·카탈로그 수치를 바꾸지 않았다.
- app image `sha256:fa9aae90d4620420ff91b1444afb49b6c4f56a38a960d2aaafdcd31a1e0e8ea0`, frontend image `sha256:3e89e3d16df5d52ef1fcb621eab56336e7a429b160a380c803738b51a08012b1`.
- 실행 JAR SHA256 `ddbb869baae0626d339d306947d92d4ff2fdae0044c61c0e4f3541dbb72093f7`. 전체 검사 JAR과 이미지의 **1,353개 엔트리 내용이 전부 같고**, 최종 프런트 검사 build와 이미지 정적 파일 **227개가 전부 같다**. nginx 기본 `50x.html`은 이전 이미지와 동일하다. ZIP 메타데이터 차이와 클래스/리소스 내용 차이를 구분했다.
- QA app/frontend만 교체했다. 기존 QA DB/Redis는 같은 컨테이너 ID·이미지·마운트·설정으로 시작했고, 기존 8080/8081 프로젝트 컨테이너는 종료 상태를 그대로 유지했다. 8081 포트가 닫힌 것을 확인했다. 이번 턴에는 OS 종료를 요청하지 않았고 QA를 켜 두었다.

### 구현 범위와 정확성

1. `AutomaticTierPartition`은 생성 시 실제 eligible pool의 가중치와 uniform/weighted 선택 규칙에서 posterior를 만든다. 생성 최소 레벨 구간 차이를 이미 생성된 속성의 후속 상태 차이로 유지하지 않는다. Solar 전용 허용 목록은 없다. 미지원 물리 전이의 확률 질량은 임의로 재분배하지 않는다.
2. `AutomaticMethodGraph`는 동일 후속 동작이 보장되는 속성/관측 클래스를 **같은 실제 노드와 방법**으로 intern한다. 예전처럼 계산만 공유하고 티어마다 별칭 노드/방법을 발행하는 구조가 아니다. 목표 경계·tags·family·슬롯·Fractured·품질·다른 미래 성공 확률은 보존한다. 안전성을 증명할 수 없는 숫자 의존성은 정밀 상태를 유지하며, 종단 추가 전이는 정확한 법칙 비교로 추가 통합한다.
3. `members`, `attributes`, `joint`와 방법 결과의 `conditional.local`, `retainedFromSource`, 각 확률 구간이 결합분포를 보존한다. 서로 다른 속성의 주변분포를 곱하지 않는다. 기존 source 변수는 그대로 투영하고 새 추첨/고정값만 local factor로 기록한다. 이것은 재귀적 조건부 표현이며 UI 공통 카드의 수치 합산이 아니다.
4. 초기 실제 아이템은 고정점으로 보존한다. 원시 확률 구간의 초기 고정점 경계와 후속 동등성을 판단하는 목표 관측 경계를 분리했다. 이로써 고정값 10과 생성값 20이 같은 후속 행동을 가질 때 보존용 경계만으로 상태를 나누지 않는다. 가중 목표처럼 미래 성공이 달라지는 경우는 분리한다.
5. 그래프는 동일 정확 구간과 동작의 시작 조건 확률을 한 번 표시한다. 서로 다른 조건의 확률을 더하거나 반올림 문자열만 같다는 이유로 합치지 않는다. 원래 조건은 접힌 상세에 남는다. **서로 다른 속성 조합**은 실제 다른 상태로 남을 수 있다.
6. 속성명은 한 번 표시하고 T1부터 티어별 수치/범위를 표로 표시한다. 확정 가능한 티어 확률만 붙이며, 여러 조건의 전체 혼합 가중치를 모를 때 첫 입력의 가중치를 전역 확률처럼 표시하지 않는다. 수치가 실제 단일 값인 생성 상태는 원래 이력이 있다는 이유만으로 수동 재사용을 막지 않는다. 분포인 상태만 구체화가 필요하다.
7. 제왕/엑잘은 기존 전체 경로 추천과 완료된 정확 확률에 따라 유리한 징조 조합을 기본 표시한다. 동일 효과면 일반 화폐를 우선하고, 카오스 준비용 제왕/엑잘은 징조 없이 한 방법을 유지한다. 수동 Workbench 징조 선택은 유지한다.
8. v11/v10/v8 저장 기록을 새 엔진으로 재실행하지 않고 읽기 전용으로 보존한다. 구형 v4/v5 execution에만 새 primitive boolean 기본값을 주어 strict JSON reader의 기존 기록 거부 문제를 수정했다. 현재 v6 형식의 엄격한 검증은 유지한다. **구형 기록의 노드 수를 마이그레이션해 바꾸지 않으며, 새 계산에 새 구조가 적용된다.**
9. 빠른 완료 직후 CANCEL이 들어온 경우 RESUME이 CANCELLED에 남던 실제 경쟁 상태를 수정했다. 이미 끝난 계산의 상태만 복구하고 추가 작업이나 예산 증가를 하지 않는다. 별도 회귀 검사와 실제 API 취소/복원 검사에서 검증했다.
10. 새 작업 생성 시 만료된 저장 파일을 삭제하지 않는다. 접근 TTL과 활성 작업/디스크 저장 상한은 유지한다. 원래 파일을 삭제하거나 만료 시각을 고쳐 검증하지 않았다.

### 최종 검증

| 항목 | 결과 |
|---|---|
| 최종 집중 검사 | **34/34 통과**: 실제 노드 통합, scalar 전개와 정확 가중치 일치, 변경되는 실패 상태의 카오스 100회 최초 성공, 생성 자격, 가중 복합 목표 상관관계, 체크포인트/취소/재개/저장 호환 |
| 최종 backend 전체 | **895 중 894 통과, 06 한 건 실패**. 전체 `check`가 녹색이라고 주장하지 않음 |
| Docker 통합 | **8/8 통과**. ArchUnit, spotlessCheck, generateJooq, bootJar 통과 |
| 최종 frontend 전체 | **2,323/2,323 통과**, lint/typecheck/format/build 통과. 이전 전체에서 검증한 동일 package lock/node_modules를 재사용해 표시 정렬 최종본을 다시 검사 |
| 실제 최종 API 응답 기반 DOM | **5/5 통과**: 공개 API validator 수용, 시나리오 압축 왕복, 속성명 1회와 티어 숫자, 제왕/엑잘 우선순위, 제왕 준비 plain 한 방법 |
| QA HTTP/API | 핵심 API 검사 **20개** + 최종 정적 파일/health/registry **9개** 기대 상태 통과. 별도 반지 부분 결과와 실제 브라우저 새 제작도 확인 |
| 실제 Chrome | 최종 번들을 격리 프로필로 열어 티어 표/제왕 준비 화면 픽셀 확인. 저장 시나리오 복사 → 새 제작 시작 → validate/API 계산 → 완료 및 구체적인 결과 수동 재사용 가능까지 확인. visible alert/브라우저 요청 오류 없음 |

수학 검증의 한 예: 가중 복합 목표에서 `(a,b)=(1,4),(4,1),(4,4)`가 각각 2/15, 합 2/5의 공동 질량으로 한 체크포인트에 들어간다. `(1,1)`은 불가능하며 다음 C 추가 후에도 나타나지 않는다. 미래 성공이 1/2 대 1인 다른 조건은 별도로 유지했다. 일반 반지 합성 fixture에서는 생성 레벨이 다른 두 티어 가중치 1/4와 3/4를 한 속성 분포로 보존했다.

실제 Solar API 결과:

- 일반 빈 목걸이 → 희귀/생명력 1 이상: **33노드, 3방법, COMPLETE**. 동일 요청의 1차 QA는 611노드였다. 32노드에 실제 티어 분포가 붙는다. 방법 확률은 일반 제왕 10.60493%, 좌측 대관 제왕 17.72449%, 우측 대관 제왕 5.32999%로 보존되고 좌측 조합이 기본이다.
- 희귀 빈 목걸이 → 생명력 1 이상: **2노드, 2방법, COMPLETE**. 목표 노드 하나 안에 T1~T9를 보존한다. 직접 엑잘은 좌측 찬미를 기본으로 표시하고, 별도 카오스 준비는 일반 엑잘 한 번의 준비만 포함한다.
- 매직 빈 목걸이 → 미니언 +3: **2노드, 2방법, COMPLETE**. 실제 API와 Chrome에 직접 제왕+우측 대관과 **징조 없는 일반 제왕 → 카오스 100회 이내** 한 경로가 표시된다. 후자는 약 5.81%로 표시되며 제왕을 100회 사용한다고 계산하지 않는다.
- 접두 3칸이 찬 희귀 목걸이의 냉기 저항 목표: 즉시 엑잘 후보가 **징조 없는 하나**임을 확인했다. 무관한 이후 카오스 탐색은 해당 소유 검증 작업만 CANCEL하여 중단했고 파일은 남겼다.
- v11 실제 완료 기록 2개: 노드/티어/방법/확률/원본 요청 보존, `LEGACY_READ_ONLY`, 재개 불가를 확인했다. 재조회 후 작업 메모리 집계가 47→12 등으로 줄어드는 경우는 복원 시 작업 맵 압축 차이이며, 결과/확률 차이와 분리해서 대조했다.

### 05/06/07과 남은 한계

- 05: COMPLETE, 11,827 단계, 3노드/2방법, retained 77. 이름의 `whittling`은 과거 시나리오 이력이며 현재 자동 보류 징조를 다시 허용한 것이 아니다. 실제 승인된 일반 화폐 경로를 검사했다.
- 07: 기본 예산에서 **COMPLETE**, 94,774 단계, 30노드/1방법, retained 640. 이전 1차의 120,000단계 미완료가 이번 구조에서는 해결됐다.
- 06: 기존 완료 테스트의 **120,000단계 관찰 가드에서 미완료**여서 전체 단위 검사 실패를 유지했다. 이때 limited=false, retained 1,806으로 20,000/50,000 자원 상한에 막힌 것이 아니다.
- 06 별도 장기 진단: **216,226단계에서 실제 finished/complete=true**, 30노드/2방법, retained 1,252, resumes=0, limited=false. 기본 64/128/20,000/5,000 안에서 끝났고 정상 최대 128/256/50,000/10,000으로 늘릴 필요가 없었다. 그러나 약 251초가 걸려 진단의 **180초 타임아웃이 실패**로 기록됐다. 제품 완료 관측과 테스트 통과를 혼동하지 않는다. 속도/테스트 관찰 범위 문제는 남았으며 원래 실패 테스트를 삭제하거나 완료 단언을 완화하지 않았다.
- 실제 `ring` API(`Metadata/Items/Rings/FourRing1`)에서도 **2노드 중 한 노드에 여러 티어**가 붙지만 전체는 PARTIAL이다. 원인은 기존 `JOINT_ROLL_MODEL_NOT_IMPLEMENTED`: 한 modifier 안의 여러 숫자 stat을 함께 굴리는 물리 롤 모델이다. 원격 기반 84b9443의 `AutomaticCraftingKernel`에도 같은 미지원이 있었다. 이것은 이번에 구현한 서로 다른 속성/티어 간 선택 이력의 결합분포와 구별된다. 해당 모델의 확률을 독립으로 추측하거나 unknown mass를 버려 COMPLETE로 표시하지 않았다.
- 가중 목표 등에서 다음 성공/제거/태그 선택이 실제 달라지거나 동등성을 증명할 수 없으면 정밀 조건을 보존한다. 모든 숫자 상태를 무조건 합치는 구현이 아니다.
- 사용자 첨부 두 장의 Library 전송은 현재 helper의 Windows `os.setxattr` 미지원으로 최종 로컬 설치에 실패했다. 파일을 열어 보지 않고 확인했다고 하지 않았다. 부모가 본 원본 설명을 참조했고, 위 최종 앱 화면은 별도 Chrome 캡처를 실제 열어 픽셀로 확인했다. 도구 거부를 우회하거나 helper를 변조하지 않았다.

### 데이터·증거·재개

- 기존 저장 파일 **15개 SHA256 전부 동일**, 현재 파일 21개(이번 자체 QA 6개 추가: 완료 4, 부분 1, 취소 1)다. 임의 데이터 삭제/볼륨 삭제/DB 초기화는 없다. `.env`, QA override, DB/Redis 컨테이너 ID·이미지·마운트가 보존됐다. Redis의 기존 영속화 설정을 바꾸거나 RAM 캐시 보존을 과장하지 않는다.
- 최종 코드는 원격에 push하지 않았고 작업 브랜치에 미커밋 상태로 보존한다. 8081은 종료 유지, QA 18081/18080은 실행 상태다.
- task `evidence/joint-source-final.json`, `joint-backend-final-r2-tests`, `joint-backend-final-r2-integration`, `joint-final-focused-r2-tests`, `joint-frontend-final-r4.xml`, `joint-budget-r1-tests`, `joint-qa/build-proof.json`, `joint-qa/live-proof.json`, `joint-qa/final-proof.json`, `joint-qa/live-ui-final.xml`, `joint-qa/ring-proof.json`, `joint-qa/browser-new-craft.json`, `joint-qa/browser-point-edit.json`, `joint-qa/browser-live-magic-proof.json`에 근거를 보존했다. 내부 Chrome 캡처도 같은 evidence 폴더에 있으며 Library에 업로드하지 않았다.
- 어제 중단 지시와 컴퓨터 종료는 완료된 과거 요청이다. 현재 요청은 재개/QA 실행이며 서버를 켜 둔다. 새 테스트 프로세스는 실행 중이지 않다. 이어서 필요한 별도 개선은 06의 성능/테스트 관찰 가드와 기존 복수 stat 물리 롤 모델의 근거 확보다. 다음 작업 전에 현 파일·데이터·컨테이너를 다시 조회하며, 성공한 검사를 무조건 반복하거나 미지원 확률을 만들지 않는다.

최종 변경/신규 파일 수: **44개**(아래 목록은 기존 1차부터 누적, 개인 설정/쿠키/실제 저장 데이터/빌드 산출물 제외).

- `backend/src/main/java/com/poe2craft/crafting/domain/pathsearch/AutomaticTierPartition.java`
- `backend/src/test/java/com/poe2craft/bootstrap/AutomaticTierLawTest.java`
- `backend/src/test/java/com/poe2craft/crafting/application/pathsearch/JointAttributeLawTest.java`
- `backend/src/test/java/com/poe2craft/crafting/domain/AutomaticEquipmentTierLawTest.java`
- `backend/src/test/java/com/poe2craft/crafting/domain/AutomaticTierRenewalTest.java`
- `backend/src/test/java/com/poe2craft/crafting/domain/JointLawFixture.java`
- `docs/automatic-tier-distribution-2026-10-22.md`
- `frontend/src/features/crafting/method-search/arrivalProbabilities.test.ts`
- `frontend/src/features/crafting/method-search/arrivalProbabilities.ts`
- `frontend/src/features/crafting/method-search/automaticTierLaws.test.ts`
- `frontend/src/features/crafting/method-search/automaticTierLaws.ts`
- `frontend/src/features/crafting/method-search/compactAttribute.test.tsx`
- `frontend/src/features/crafting/method-search/compactAttribute.ts`
- `frontend/src/features/crafting/method-search/compactFrozenCard.ts`
- `frontend/src/features/crafting/method-search/jointValidation.ts`
- `backend/src/main/java/com/poe2craft/crafting/application/pathsearch/AutomaticSearchService.java`
- `backend/src/main/java/com/poe2craft/crafting/application/pathsearch/LegacyAutomaticResultReader.java`
- `backend/src/main/java/com/poe2craft/crafting/application/pathsearch/SavedAutomaticResultReader.java`
- `backend/src/main/java/com/poe2craft/crafting/domain/AutomaticCraftingKernel.java`
- `backend/src/main/java/com/poe2craft/crafting/domain/pathsearch/AutomaticMethodExecution.java`
- `backend/src/main/java/com/poe2craft/crafting/domain/pathsearch/AutomaticMethodGraph.java`
- `backend/src/main/java/com/poe2craft/crafting/domain/pathsearch/RollPartition.java`
- `backend/src/test/java/com/poe2craft/bootstrap/TerminalBranchingScenariosTest.java`
- `backend/src/test/java/com/poe2craft/bootstrap/TerminalContinuationApiTest.java`
- `backend/src/test/java/com/poe2craft/crafting/domain/AutomaticSearchServiceTest.java`
- `backend/src/test/java/com/poe2craft/crafting/domain/TerminalAdditionClassesTest.java`
- `frontend/src/features/crafting/ItemCard.tsx`
- `frontend/src/features/crafting/item-card.css`
- `frontend/src/features/crafting/itemCardData.ts`
- `frontend/src/features/crafting/method-search/AutomaticMethodSearch.tsx`
- `frontend/src/features/crafting/method-search/CandidateRoute.tsx`
- `frontend/src/features/crafting/method-search/GroupedMethodGraph.tsx`
- `frontend/src/features/crafting/method-search/OperationCards.test.tsx`
- `frontend/src/features/crafting/method-search/OperationCards.tsx`
- `frontend/src/features/crafting/method-search/ScenarioCodePanel.tsx`
- `frontend/src/features/crafting/method-search/SearchResources.tsx`
- `frontend/src/features/crafting/method-search/automaticApi.ts`
- `frontend/src/features/crafting/method-search/commonStateCard.ts`
- `frontend/src/features/crafting/method-search/graphNavigationMessages.ts`
- `frontend/src/features/crafting/method-search/operationGrouping.ts`
- `frontend/src/features/crafting/method-search/scenarioCode.test.ts`
- `frontend/src/features/crafting/method-search/scenarioCode.ts`
- `frontend/src/features/crafting/method-search/stateProjection.test.tsx`
- `frontend/src/features/crafting/method-search/stateProjection.ts`

원본 첨부 NTU 물질화 미완료 대상: libfile_a703c1b0919481918f584f2237bf5202, libfile_40190fa849c08191935ad40af00e95cd. 승인된 prepare_materialize 이후 현재 helper의 os.setxattr 호출이 Windows에서 AttributeError로 실패했다. 로컬 최종 PNG는 설치되지 않았고 원본 픽셀을 NTU에서 열지 못했다. 도구 거부는 아니며 metadata 절차를 우회하지 않았다. 실제 최종 앱의 Chrome 캡처 3장은 별도로 열어 확인했다.
검증용 격리 Chrome은 정상 종료했고, QA app/frontend/PostgreSQL/Redis 네 컨테이너는 계속 실행 중이다. 현재 진행 중인 테스트/빌드/배포 명령은 없다.
> 후속 최신 상태(2026-10-23): [속성 목표 판정 복구 및 최종 QA 검증](unsupported-attributes-2026-10-23.md)을 먼저 참고한다. 반지 신규 20개 판정과 선언된 복수 수치 모델을 QA18080/18081에 반영했다. 프런트2350 통과, 백엔드900 중 기존06 한 개 실패·통합8 통과. 기존 저장28개 보존,8081 종료,미커밋·미푸시.
