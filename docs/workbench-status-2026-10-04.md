# Workbench 최종 사용자 상태 — 2026-10-04

Workbench의 요청된 핵심 UX와 제한된 제작 모델은 검증됐지만, 전체 게임 제작기가 완성된 상태는 아니다. 기본 지원111개와 명시적으로 켜는 legacy2개를 제공하며 개발 범위155개 중42개는 미완료다. typed Catalyst 품질은 기존 값 입력·보존·제한된 표시를 지원하지만 기폭제 사용과 화폐 상호작용은 구현하지 않았다.

문서 날짜는 요청한 보고서 이름을 따른다. 시작 기준 및 검토한 실행 코드 HEAD는 `bbe608d1f673667269c26ab473bcb5194b38f103`, branch는 `workbench/20261002`다. 이번 묶음은 문서·검토 증거만 변경하며 executable code/catalog/lockfile/migration은 변경하지 않는다. 보고서 자체의 로컬 commit은 `git log -1 --format=%H -- docs/workbench-status-2026-10-04.md`로 확인한다. 최종 commit SHA는 상위 codex의 재시작 인계에도 기록한다. 원격 push/merge/deploy는 수행하지 않았다.

## 지원 숫자와 9베이스

| 구분 | 개수 | 의미 |
|---|---:|---|
| 등록 inventory |220|등록된 재료 전체이며 현재 획득 가능 개수가 아니다|
| 현재 개발 scope |155|기본 지원111 + opt-in legacy2 + 미완료42|
| 개발 scope 구현 |113|111과 legacy2의 합; 모든 베이스에서 모든 조합이 지원된다는 뜻은 아니다|
| 사용자 보류 |65|미구현57 + 이전 구현을 코드에 보존한 Alloy8|
| registry 전체 구현 |121|scope113 + 보존 Alloy8; 기본 현재 지원 숫자와 구분|
| registry 전체 미구현 |99|scope 미완료42 + 보류 미구현57|

베이스는 **Solar Amulet, Stocky Mitts, Crude Bow, Attuned Wand, Rusted Cuirass, Rattling Sceptre, Rawhide Belt, Rusted Greathelm, Iron Ring**이다. 9 catalog의 원문 modifier 정의1487개를 보존한다. 보류된 기존 구현8개는 Runic/Adaptive/Expansive/Swift/Cyclonic/Prismatic/Mystic/Sovereign Alloy이며 이번 범위를 확장하거나 제거하지 않았다.

## 사용자 동선과 안전 경계

- 재료 선택 후 중앙 아이템에 적용한다. 성공한 일반 클릭은 선택을 해제하고 Shift 클릭은 선택을 유지한다. 빈 비조작 영역 클릭과 Escape로 취소한다. 꽉 찬 슬롯·미지원 재료·통신 실패는 제작 성공이나 새 frame으로 기록하지 않는다.
- Omen은 즐겨찾기에 등록한 뒤 우클릭으로 붉은 활성 상태를 전환한다. 미확인 same-trigger 조합은 두 번째 활성화를 막고 이유를 알린다. 서로 다른 trigger는 해당 작업에 필요한 Omen만 소비한다. 확인된 Homogenising Exaltation + Greater Exaltation 병용은 별도 제한을 따른다.
- desktop은 stash와 아이템 영역을 좌우로 제공한다. 390px 화면에서는 재료 영역 내부 스크롤을 사용하며 문서 전체 가로 넘침과 중앙 버튼 잘림이 없다. Alt를 누르면 표시 숫자 대신 전체 source 범위를 보여주고 떼면 저장 roll 표시로 돌아온다.
- 새 base를 놓으면 새 local film을 시작한다. 과거 frame을 구경하는 것만으로 분리되지 않으며 과거에서 새 제작을 하면 새 film을 만들고 원본 미래를 보존한다. 원본 film 선택·앞뒤 이동·reload가 가능하다. 각 step의 source/rule/assumption evidence를 보존한다.
- quota 저장 실패는 경고하고 현재 실제 제작 결과와 메모리 내 이동을 유지한다. 저장 실패 후 reload까지 보존된다는 보장은 없다. 손상된 저장 내용은 경고하고 덮어쓰지 않는다. 미확인 snapshot·특수 상태·잘못된 응답은 안전하게 거부한다. 전체 조합의 무손실을 입증한 것은 아니며 검증한 경계는 아래 증거에 한정한다.

Homogenising 두 Omen은 기본 목록에서 숨긴다. `Show legacy Homogenising Omens`를 켜면 `(Legacy)` 이름과 **0.4 drop 중단·기존 개체 작동** 안내를 표시한다. 현재 drop 재료처럼 소개하지 않는다. ordinary Exalted/Regal과 확인된 병용만 지원하며, 실패 소비·다른 same-trigger 조합·강화 화폐·typed quality interaction은 미확인이다. Workbench의 원자적 거부/자원 보존 정책을 실제 게임 실패 소비 사실로 해석하면 안 된다. [근거와 제한](workbench-homogenising-legacy-2026-10-03.md).

Solar/Iron의 `Existing catalyst quality`는 이미 존재하는 type/amount를 입력하는 기능이다. 별도의 Catalyst 사용 버튼이 아니다. 원본 roll을 저장하고 source-reviewed 정수 stat만 별도로 투영하며 unscalable/미검토 숫자 의미는 유지한다. 명시적으로 입력한 품질0도 화폐 상호작용은 거부한다. film에 type/amount와 원본 값을 보존하며 null/missing 품질을 임의의0으로 바꾸지 않는다. [품질 계약](workbench-catalyst-quality-2026-10-04.md).

## 확률·출처·가정

자연 ADD의 안내는 `Model probability ... not verified game odds`다. PoE2DB 표의 **DropChance**는 완전한 해당 snapshot 모델 weight이며 detail의 ordered **Spawn Tags**는 eligibility다. 두 수치의 유래 차이는 미확정이며 실제 게임 확률로 주장하지 않는다. 재료 보장 결과는 자연 weighted draw로 표시하지 않는다. modifier group/family/tag는 catalog에 기록된 후보 제한을 유지하며 게임의 숨겨진 group/tag 정밀도까지 검증했다고 확대하지 않는다.

WB-001 공통 ratio10001 tick/HALF_UP, WB-003 source-unit 숫자 정밀도, 일부 균등1/N은 명시적인 미검증 모델이다. tick 확률은 서로 다른 rounded tuple의 균등 확률이 아니다. WB-028 등의 낮은 ilvl 거부는 source modifier level에 근거한 보수적 모델 경계이며 실제 게임 최소 ilvl 확정이 아니다. Catalyst 표시의 secondary source도 공식 engine precision 보증으로 표시하지 않는다. 자체 코드 검토와 실제 최신 화면 확인에서 정확한 게임 odds를 주장하는 새 결함은 발견하지 않았다.

## 미완료42 — 정확한 분류

| 분류 | 개수 | 필요한 조건 |
|---|---:|---|
| 품질 사용 |29|일반 Catalyst13 + Refined13 + 장비 품질 화폐3; per-use 증가·type 전이·ilvl/unique 예외·숫자 정밀도·craft interaction. Refined는 Jewel 대상이며 현재9베이스에는 대상 없음|
| Delirium Notable |1|완전한 eligible Notable pool·충돌·분포 및 무손실 표현|
| Insanity |1|enchantment/corruption 결과·분포·상태; 보류한 Vaal 의존성|
| Omen 후보·상호작용 |3|Catalysing의 all-quality 소비/전이와 bias 함수; Sinistral/Dextral Necromancy의 ordinary+exclusive Desecrated pool·생성/공개·분포·포화/특수 상태|
| 기존 retired Omen |5|각 항목의 공식 가용성 또는 기존 개체 동작 및 완전한 효과·조합 근거; Homogenising 근거를 다른5개에 전용하지 않음|
| Wisdom |1|이미 존재하는 UNIDENTIFIED marker와 별개로 실제 reveal/retention 의미·source-bound hidden payload·known/unknown 처리|
| Chance |1|base별 완전한 unique/파괴 결과·eligibility·분포·상태|
| Extraction |1|추출 대상·Augment inventory 결과·보존/충돌·상태|
| 합계 |42|완전한 사실이 준비돼 코드만 남은 재료를 확인하지 못함|

정확한29 품질 ID는 일반 `Flesh_Catalyst`, `Neural_Catalyst`, `Carapace_Catalyst`, `Uul-Netols_Catalyst`, `Xophs_Catalyst`, `Tuls_Catalyst`, `Eshs_Catalyst`, `Chayulas_Catalyst`, `Reaver_Catalyst`, `Sibilant_Catalyst`, `Skittering_Catalyst`, `Adaptive_Catalyst`, `Necrotic_Catalyst`와 각각에 `Refined_`를 붙인13개, `Blacksmiths_Whetstone`, `Arcanists_Etcher`, `Armourers_Scrap`이다.

나머지13 ID는 `Essence_of_Delirium`, `Essence_of_Insanity`, `Omen_of_Catalysing_Exaltation`, `Omen_of_Sinistral_Necromancy`, `Omen_of_Dextral_Necromancy`, `Omen_of_Sinistral_Alchemy`, `Omen_of_Dextral_Alchemy`, `Omen_of_Sinistral_Coronation`, `Omen_of_Dextral_Coronation`, `Omen_of_Greater_Annulment`, `Scroll_of_Wisdom`, `Orb_of_Chance`, `Orb_of_Extraction`이다. [과거44 ID audit](evidence/workbench-pending44-fact-implementation-audit-2026-10-03.json)에서 Homogenising 두 항목만 완료로 제외한다. 과거의 applied-quality state 전부 부재라는 설명은 현재 Solar/Iron 기반으로 대체됐지만29개 사용 동작은 계속 미구현이다. 외부 사실 부족은 개발 조사 조건이며 사용자에게 게임 규칙 선택을 요구하지 않는다.

## 검증 통과와 미실행

이번 리뷰: 최신 실행 JAR `780033645f4e63c1ea133a1c2d421a1711cf1a52584849df25a97aaeb1a75d75`, HTML의 실제 bundle `index-DtvWs21k.js`/`index-Di5BXYtv.css`를 확인했다. 핵심 UX browser30 및 표시 경계12 PASS/page errors0, 390px screenshot 자체 리뷰 완료. 추가12개는 자연 weight/eligibility 안내, legacy 기본 숨김/opt-in/다시 숨김, Delirium 미지원 거부, typed quality0 입력·무요청 거부·film reload를 확인했다. 새 probe의 CSS locator timeout과 좌클릭/우클릭 계약 오류 timeout은2회 교정 후 완료했고 품질 locator도 실제 문구에 맞췄다. product 코드나 기존 guard를 완화하지 않았다. [이번 증거](evidence/workbench-final-user-review-2026-10-04.json)에 정확한 결과를 기록한다. 합성 QA 아이템과 격리 browser storage만 사용했다.

기존 검증은 새 실행으로 합산하지 않는다:

- Homogenising: Backend359(353 unit+6 integration, skip0), check/generateJooq/bootJar PASS. FE npm ci/lint/typecheck/format/build PASS, 고유333개는 전체331 통과 후 관련30 통과의 합이며 마지막 단일333 full run이 아니다. API269, catalog1487, browser83+layout10 PASS. [원래 증거](evidence/workbench-homogenising-validation-2026-10-03.json).
- typed quality: BE354, FE 기존320 full + 새9 targeted, API74, browser56+focus5 PASS. 현재 후속359/333과 별개인 이력이다. [원래 증거](evidence/workbench-catalyst-quality-validation-2026-10-04.json).
- continuation: browser993=30+362+214+11+376 PASS 및 cachepersist 재시작 전후 동일성 PASS. 9베이스 대표 material/Omen/Fracture/old film 동선을 포함하며 모든 permutation 검증을 뜻하지 않는다. [원래 증거](evidence/workbench-continuation-validation-2026-10-03.json).

이번에는 executable 변경이 없어 BE/FE 전체검사·API269·browser83/993·cache 재시작을 반복하지 않았다. Windows script와 Compose도 변경하지 않아 관련검사를 재실행하지 않았다. 품질 실제 사용, hidden reveal, unique/파괴, Desecration은 구현 경로가 없으므로 성공 검증을 주장하지 않는다. COM 원인은 미확정이며 Windows process/COM 설정을 변경하지 않았다. Docker 기본 sandbox 읽기는 pipe 접근 거부였고 승인된 `require_escalated` 경로로 QA 확인·browser를 실행했다. 제한 우회나 자격증명 열람을 하지 않았다.

## 추천안과 사용자 판단

[ISSUES](../ISSUES.md) WB-039에 이번 자체 리뷰와 가역 추천을 기록한다. 추천은 Workbench 우선순위·현재9베이스·legacy opt-in·미확인 상태의 안전한 거부를 유지하고,42개는 명시한 근거가 확보된 항목부터 진행하는 것이다. 같은 실패 조사를 반복하거나 범위를 늘려 완료 숫자를 채우지 않는다. 새 Support/Explorer/전투 계산기는 추가하지 않는다.

WB-001 모델과 WB-008 evidence 저장량 추천은 이미 사용자 승인됐으므로 재승인 대상이 아니다. WB-003/004/028/030/036/037/038의 게임 사실은 개발 조사 대상으로 남는다. 지금 완료를 막는 미답 제품 결정은 없다. 사용자가 향후 Insanity를 위해 Vaal 보류를 해제하거나 다른 보류65/새베이스/서버 저장소/원격 반영을 요청할 때에만 별도 scope 판단이 필요하다. 현 추천안을 되돌리는 것은 future plan 변경으로 가능하며 저장 film 삭제·DB 초기화가 필요하지 않다.

## 테스트 주소와 재실행

현재 테스트 UI: **http://127.0.0.1:18081/**, API liveness: **http://127.0.0.1:18080/actuator/health/liveness**. 기존 QA4서비스를 유지했다. 원본 checkout의8081/8080 실행 환경과 구분한다. QA의 DB volume·원본 .env·원본 checkout은 보존했다.

중단된 QA만 같은 구성으로 재개할 경우 상위 `E:\WORK\Exile-Hephaistos\codex`에서 다음을 사용한다. 기존 JAR/FE가 준비돼 있다는 전제이며 이 명령 자체가 최신 build를 생성하지 않는다.

```powershell
docker compose -f qa-20261002/compose.yaml -f qa-20261002/homogenising-compose.yaml up -d
docker run --rm --name exile-workbench-final-ux-20261004 -v E:/WORK/Exile-Hephaistos/codex/qa-20261004/final-review:/evidence -v E:/WORK/Exile-Hephaistos/codex/workbench-20261002:/repo:ro exile-workbench-browser:20261002 node /evidence/ux-browser.cjs
docker run --rm --name exile-workbench-final-boundary-20261004 -v E:/WORK/Exile-Hephaistos/codex/qa-20261004/final-review:/evidence exile-workbench-browser:20261002 node /evidence/boundary-browser.cjs
```

무거운 검사/browser는 순차 실행한다. 임시 browser는 정상 종료/`--rm` 정리한다. `down -v`, volume reset, 원본 stack 재시작은 실행하지 않는다. 전체 소스 재빌드가 필요하면 [README 실행·검증 방법](../README.md)과 [운영 명세](solo-workflow/MULTI_SESSION_WORKFLOW.md)를 따른다. 로컬 commit까지가 완료 경계이며 master/원격 반영은 별도 지시 전까지 미실행이다.
