# 담당 A — 새 25개 정정 중 1·2·3·7·8·9

기준 SHA: `4fb6b89d0f2319444742aac336f9b1a1832b515c`.
작업 branch: `codex/stash-corrections-a-20261103`.
작업트리: `C:/Users/SSAFY/.codex/worktrees/2721/Exile-Hephaistos`.
시작 시 detached HEAD와 미커밋 변경 없음, 기준 SHA 일치를 확인했다.
환경 정정에 따라 현재 채팅의 전용 작업트리를 최종 소스로 사용한다.
처음 만든 task-3의 `stash-25-corrections-a`는 보존했으며 통합 대상이 아니다.
standard tier는 도구에 설정 필드가 없어 적용 여부 미확인이다.
추가 내부 하위 에이전트는 사용하지 않았다.

## 소유 파일과 통합 경계

실제 수정은 `stash/`, `materials.ts`, `MaterialStash.test.tsx`뿐이다.
`CraftingPage.tsx`, `crafting.css`, `app/App.tsx`, 전역 i18n, 공통 리소스는
직접 수정하지 않았다. 아래 패치는 통합 담당이 적용해야 하는 연결 변경이다.
Worker 운영 명세의 일반적인 인계 문서 금지보다 이번 명시적 INTEGRATION 요청을 따른다.

1. `COMMON_PAGE.patch`: 기존 tablist 한 벌을 `WorkbenchStash.tabs` slot으로 이동,
   `stash-canvas__content`에 기존 좌표 평면 보존, 즐겨찾기를 같은 canvas의 하단 자식으로 이동,
   실제 왼쪽 고정 화폐만 우측에서 제외, 품질 화폐 2종 리소스 연결,
   신규 선택/즐겨찾기 등록/징조 활성화에 visibilityPolicy 적용.
2. `COMMON_RESOURCES.patch`: 기존 gameTerms.en의 출처 설명을 그대로 사용해
   materialTooltips.json에 Armourers_Scrap/Blacksmiths_Whetstone의 툴팁 진입점 추가.
   원문·번역·이미지·라이선스·해시 변경은 없다.
3. `TEST_EXPECTATIONS.patch`: 공유 테스트 helper를 tablist의 의미 기반 조회로 바꾸고,
   currencyButton은 실제 왼쪽 고정 shelf를 먼저 찾도록 정정.
   기존 폐기된 탭 위치/중복 화폐 조회를 유지하면 새 UI 테스트가 잘못 실패한다.

```powershell
git apply --ignore-space-change --check frontend/src/features/crafting/stash/COMMON_PAGE.patch
git apply --ignore-space-change frontend/src/features/crafting/stash/COMMON_PAGE.patch
git apply --ignore-space-change frontend/src/features/crafting/stash/COMMON_RESOURCES.patch
git apply --ignore-space-change frontend/src/features/crafting/stash/TEST_EXPECTATIONS.patch
```

패치는 기준 SHA에서 적용 가능함을 확인한다. 다른 담당 변경과 공통 페이지가 겹치면
통합 담당이 계약에 맞춰 합쳐야 한다. 이 branch만 cherry-pick한 상태는 공통 연결 전 상태이며
새 화면을 완전히 연결한 상태로 보고하지 않는다.

## 배치와 상호작용 계약

- 페이지가 소유하는 activeTab 하나와 기존 키보드 Arrow/Home/End 로직을 유지한다.
  동일 tablist를 우측 상단 slot에 넣고 aria-controls/aria-labelledby를 유지한다.
- 일반 에센스 19계열×Greater/Perfect 38개를 한 행 4칸으로 묶는다.
  `essenceRows`의 계열 단위 검색은 보존하고 `essenceGridRows`가 두 계열을 한 행으로 합친다.
  마지막 행은 2개+빈칸 2개이며, 다른 에센스를 추가하지 않는다.
  사용자 표현 ‘4줄’은 함께 전달된 ‘4열’ 계약에 따라 **4열**로 해석했다.
  전체를 4개의 수평 행에 압축한다는 의미는 아니다. 검색 없음 기준 총 10행이다.
- 지원 특수 에센스 4개는 기존과 같이 별도 1행이며 검색 중에도 자리를 유지한다.
- 즐겨찾기는 2행×5칸이며 stash-canvas 내부 하단에 둔다. 우측 목록 scroll과 독립적이다.
  기존 935×550 좌표는 별도 content wrapper에 유지해 화폐/아이템 위치를 바꾸지 않는다.
- `fixedCurrencyIds`는 실제 왼쪽 `.currency-slot` ID 목록이다. 오른쪽 Currency에서만 제외한다.
  `excludedIds`는 기존 뼈 정책 등 전역 접근 제외이며 두 prop을 섞지 않는다.
  고정 Currency는 즐겨찾기에서도 계속 선택할 수 있다.
- `onActivateOmen(resource, pointer?)`는 목록/즐겨찾기 우클릭으로 호출한다.
  왼쪽 클릭은 기존 재료 들기/등록 동작을 유지한다. 콜백은 기존 호출부와의 호환을 위해
  optional이며, 통합 패치는 두 컴포넌트 모두 기존 toggleOmen에 연결한다.
- 새 문자열을 추가하지 않고 기존 6개 UI locale의 재료명·카테고리명을 사용한다.
  개발 진단이나 새 영어 설명을 제품 화면에 추가하지 않았다.

## 신규 접근 제외와 데이터 보존

`stash/visibilityPolicy.ts`의 실제 export 계약은 다음과 같다.

```ts
export function visibilityPolicy(resource: {
  id: string
  category?: MaterialTab
}): boolean

// 기준 SHA에서 펼친 전체 8개
export const legacyStashOmenIds: readonly string[] = [
  'Omen_of_Homogenising_Exaltation',
  'Omen_of_Homogenising_Coronation',
  'Omen_of_Sinistral_Alchemy',
  'Omen_of_Dextral_Alchemy',
  'Omen_of_Sinistral_Coronation',
  'Omen_of_Dextral_Coronation',
  'Omen_of_Greater_Annulment',
  'Omen_of_Corruption',
]

// 추가 제외 전체 5개
export const excludedStashOmenIds: readonly string[] = [
  'Omen_of_Answered_Prayers',
  'Omen_of_the_Hunt',
  'Omen_of_Secret_Compartments',
  'Omen_of_Reinforcements',
  'Omen_of_Putrefaction',
]
```

두 배열은 중복 없이 총 13개 ID다. **Corruption은 legacyStashOmenIds에 포함**된다.
실제 구현은 기존 `legacyOmenIds` 7개를 펼친 뒤 Corruption을 추가하며,
추가 제외 배열에는 Corruption 대신 별개의 Putrefaction이 포함된다.

`visibilityPolicy`는 UI 표시/신규 접근 정책으로 export한다.
기존 legacyOmenIds 7개에 `Omen_of_Corruption`을 더해 legacy 신규 접근을 차단한다.
legacy checkbox와 안내 영역은 제거했다. deprecated showLegacyOmens props를 전달해도
숨긴 항목에 다시 접근할 수 없다. backend의 역사적 legacy 계약은 변경하지 않는다.

5개 추가 제외 ID는 기존 한국어 gameTerms의 이름/출처로 확인했다.

| 한국어               | ID                          |
| -------------------- | --------------------------- |
| 응답받은 기도의 징조 | Omen_of_Answered_Prayers    |
| 사냥의 징조          | Omen_of_the_Hunt            |
| 비밀 공간의 징조     | Omen_of_Secret_Compartments |
| 보강의 징조          | Omen_of_Reinforcements      |
| 부패의 징조          | Omen_of_Putrefaction        |

`Omen_of_Corruption`은 **타락의 징조**이며 부패의 징조와 다른 ID다.
신규 Omen 목록은 기준 31개에서 legacy 8개·추가 5개를 제외한 18개다.
materials의 기존 180개 ID와 모든 이미지/출처/설명은 그대로 유지한다.
숨겨진 재료가 들어 있는 기존 즐겨찾기는 이름·이미지·값을 보존하고 신규 사용만 막으며,
사용자가 다른 허용 재료로 교체할 수 있다. 저장 기록·activeOmens를 자동 정리하지 않는다.
root는 공통 `choose`·`toggleOmen`·즐겨찾기의 신규 접근에 같은 정책을 적용한다.
기존 history의 소비 기록과 stored ID는 삭제·재작성하지 않는다.
기존 활성 징조의 해제는 신규 활성화와 구분하며, 제공한 toggleOmen 패치도 이를 허용한다.

## C 품질 화폐 계약

`additionalStashCurrencies`는 기존 catalog를 삭제/변형하지 않고 추가 표시 리소스를 제공한다.

| ID                    | C action              | 기존 이미지                                 |
| --------------------- | --------------------- | ------------------------------------------- |
| Armourers_Scrap       | ARMOURERS_SCRAP       | /assets/currency/CurrencyArmourQuality.webp |
| Blacksmiths_Whetstone | BLACKSMITHS_WHETSTONE | /assets/currency/CurrencyWeaponQuality.webp |

이미지 파일과 currency/sources.json의 출처·해시 기록이 이미 존재함을 확인했다.
visibilityPolicy는 이 두 ID만 기존 deferredServiceIds보다 우선 허용한다.
root는 공통 소유 `serviceScope.ts`의 deferredServiceIds에서 이 두 ID를 제거하기로 했다.
이 변경 후에도 A의 우선 허용 결과는 같으며, A는 해당 공통 원본을 수정하지 않았다.
현재 currencies.ts의 왼쪽 고정 목록에 없으므로 우측에만 추가된다.
기준 페이지의 우측 Currency는 중복 21개를 제외한 9개+품질 2개, 총 11개다.
C의 workbenchCurrencyActions 및 backend 구현과 최종 통합해야 실제 사용을 검증할 수 있다.
A는 수치나 게임 효과를 추측하지 않았고 action 구현을 수정하지 않았다.

## 검증 방식과 남은 통합 검증

공통 파일은 원본에서 수정하지 않고 `.verification-a/`의 자체 복사본에만 패치를 적용해
DOM/상호작용·정적 검사·build를 확인했다. 검증 보고서는 이 폴더 아래에 보존한다.
브라우저 자동 QA, 서버 시작/교체, Docker 소켓 접근, remote push는 수행하지 않았다.
운영 DB·Redis·볼륨·설정·54개 저장 파일에는 접근하거나 쓰지 않았다.
Backend/Windows 스크립트/Compose는 변경하지 않았다.
DB 통합 검사와 generateJooq는 기존 차단 및 이번 no-socket 범위로 미실행이다.
화면의 실제 픽셀 배치는 미검증이며 jsdom DOM/CSS 검사를 브라우저 QA로 표현하지 않는다.

### 최종 결과

- `npm ci --offline --no-audit --no-fund`: 통과. 기존 lockfile 변경 없음.
- 원본 전용 branch의 `npm run format:check`, `npm run typecheck`, `npm run lint`: 통과.
- 원본 전용 branch의 `npm run test -- --run src/features/crafting/stash/WorkbenchStash.test.tsx --maxWorkers=1`: **12/12 통과**.
- 공통 패치 3개를 적용한 자체 복사본의 `npm run test -- --run src/features/crafting/stash src/features/crafting/MaterialStash.test.tsx`: **40/40 통과**.
  이 검사는 단일 worker 지침 도착 전에 실행했다. 보고서: `.verification-a/stash-final.json`.
- 같은 복사본의 `npm run lint`, 최종 `npm run build`: 통과.
  기존 큰 번들 경고만 있으며 build 실패는 아니다.
- format 검사에서 임시 JSON 보고서가 검사 대상에 포함된 1회 실패는 결과를 복사본 frontend 바깥으로 옮겨 해소했다.
  소스의 최종 format 검사는 통과했다. 초기 공통 helper/툴팁 연결 실패도 패치로 수정했고 40개를 재실행해 통과했다.
- 전체 frontend suite는 통합 담당이 한 번 수행한다는 후속 지침에 따라 중단했다.
  전체 결과 JSON은 생성되지 않았으며 전수 통과로 주장하지 않는다.
- `git apply --ignore-space-change --check`로 세 패치의 기준 branch 적용 가능성을 확인했다.
- 자체 리뷰: 소유권 diff, 단일 탭 상태/키보드 연결, 실제 왼쪽 고정 Currency 중복 제외,
  에센스 4열/특수 별도 행, canvas 하단 즐겨찾기 경계, 드래그 ID 검증,
  숨겨진 즐겨찾기 보존/교체, 13개 Omen 신규 접근 차단을 확인했다.
- 원본 소유 소스/테스트 10개가 실제 검증 복사본과 일치함을 SHA-256으로 기록했다.
  증거: `.verification-a/verified-source-sha256.json`, `.verification-a/component-only.json`.

### 통합 담당의 남은 검사

세 패치를 최종 공통 페이지에 연결하고 C 품질 action을 합친 뒤 전체 frontend 검사를 실행한다.
기존 legacy checkbox/legacy 신규 접근을 기대하는 다른 담당 소유 테스트는 새 제품 요구사항과
비교해 통합 담당이 검토해야 한다. A는 소유 범위 밖 테스트를 삭제·skip·직접 수정하지 않았다.
최종 통합 후 서버 반영과 보존 검증은 통합 담당만 수행한다.
