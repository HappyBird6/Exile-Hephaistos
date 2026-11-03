# B — 요청 4·5·6·18 통합 계약

기준 SHA: `4fb6b89d0f2319444742aac336f9b1a1832b515c`.
실제 구현 branch: `codex/abyss-25-corrections-b-local-20261103`.
실제 구현 경로: `C:\Users\SSAFY\.codex\worktrees\fdcc\Exile-Hephaistos`.
초기 지시에 따라 생성한 `task-4/abyss-25-corrections-b`는 기준 SHA 그대로 보존했고 구현에는 사용하지 않았다. 이후 통합자의 환경 정정을 반영했다.

공통 화면·공통 엔진·Controller·catalog·seal·manifest·registry·설정은 직접 수정하지 않았다. 기존 룬 이미지와 과거 문서는 보존했다. 모델/High는 통합자의 지정 사항이며 실제 runtime 및 standard tier를 조회/변경하는 도구는 없어 실제 설정을 보증하지 않는다.

## 적용 순서

아래 patch는 기준 소스에 `git apply --check`로 확인한 제안이며 이 branch에서 공통 파일에 적용하지 않았다. A/C/D 통합 후 문맥 충돌은 통합자가 같은 동작으로 수동 반영한다.

1. B commit을 통합한다.
2. C의 ItemState/WorkbenchSimulator/WorkbenchService 및 Sanctification 변경을 통합한다.
3. `patches/WorkbenchService.patch`, `patches/WorkbenchController.patch`를 반영한다. 별도 simulator를 재생성하지 않고 Service의 기존 base별 simulator와 reviewed overrides를 그대로 사용한다.
4. **필수:** `patches/AbyssCraftingState-sanctification.patch`를 반영한다. C의 `withSanctification`/`sanctification` API가 추가된 후 사용하는 B 소유 projection 보강이다. 기존 생성자 호환만으로는 새 메타가 소실되므로 이 patch 없이 Sanctification 통합 완료로 판단하면 안 된다.
5. `patches/CraftingPage.patch`를 반영하고 formatter를 실행한다. 숨은 슬롯이 있다는 이유만으로 canCraft를 false로 만드는 조건과 notice.use_abyss 조기 반환을 제거한다. 대신 실제 화폐 전이를 `/abyss/currency`로 보낸다.
6. `patches/currencies.patch`의 Preserved Cranium 매핑을 A의 최종 currency 목록에 반영한다. 실제 뼈 위치는 `materials.ts`가 아닌 `currencies.ts`에 있다. A가 목록을 재구성했다면 `abyssCurrencyImages[id] ?? existingImage`를 같은 위치에 사용한다.
7. C 통합 후 `patches/AbyssContinuousCurrency-sanctification-test.patch`를 반영해 메타 보존과 후속 조작 거부를 확인한다. 이 신규 C 연동 테스트 patch는 B 기준 소스에서 실행하지 않았다.
8. `patches/DesecrationCurrency-ui-test.patch`는 제거한 접두·접미 입력 UI의 기존 테스트를 함께 바꾼다. 이 branch의 관련 테스트는 공통 화면을 수정하지 않은 상태에서 실행한 결과다.

## 4 — 뼈 화폐와 드러내기의 분리

`DesecrationCurrency.apply(..., RandomGenerator)`는 side/removal 생략을 허용한다. 기존 observed side/removal 요청도 호환한다. Controller patch는 주입된 random을 전달한다.

- 분포가 공개되어 있다고 주장하지 않는다. 구현 정책은 **전체 검증을 통과하는 제거 대상 중 균등 선택 → 그 대상에서 가능한 접두·접미 중 균등 선택**이다. 빈 슬롯이 남은 경우 제거하지 않는다. 두 side가 가능하면 양쪽이 나올 수 있고, 한쪽만 가능하면 그쪽만 선택한다. 후보가 없는 쪽, 분열 제거, 잘못된 class/level/pool, 슬롯 충돌은 제외/거절한다.
- Necromancy가 있으면 해당 side만 가능하다. 반대 징조 동시 활성은 거부한다. side를 강제로 선택하는 일반 UI는 root patch에서 제거한다. 가득 찬 아이템의 제거 입력도 필수가 아니며 root patch에서 제거한다.
- 뼈 사용 결과는 항상 신규 slot의 `modifier: null`, `offer: null`이다. reveal modifier나 offer 분포를 샘플링하지 않는다. 이후 사용자의 ‘드러내기’와 직접 선택을 유지한다. Putrefaction의 관측 슬롯 수 입력은 별도 계약으로 보존한다.
- 6개 언어의 boneText는 시뮬레이터 정책과 미검증 게임 확률을 구분한다. 이 설명은 사용자에게 필요한 동작 정보이며 내부 식별자/개발 진단을 표시하지 않는다.

근거: [GGG Rise of the Abyssals](https://www.pathofexile.com/forum/view-thread/3826682/filter-account-type/staff), 기존 `docs/evidence/desecration-currency-20261102.md`, [PoE2DB Desecrated Modifiers](https://poe2db.tw/us/Desecrated_Modifiers), [Necromancy](https://poe2db.tw/us/Omen_of_Sinistral_Necromancy). 슬롯을 점유하는 숨은 옵션/side 제한과 실제 분포는 별개다. 확인한 자료에는 side/제거의 수치 분포가 없어 위 정책을 명시했다.

## 6 — 일반 화폐 연속 적용

`AbyssCurrencyExecutor`는 `AbyssState + action + activeOmens -> Result`를 처리한다. 결과는 `state`, `action`, `applied`, `reason`, `consumedOmens`, `remainingOmens`다. Controller는 기존 엄격한 AbyssRequest/abyssState 파서를 재사용한다. 사용 가능한 카탈로그와 base별 simulator를 Service에서 전달한다.

- 숨은 슬롯이 있는 Exalted/Annulment/검토된 replacement essence/alloy는 기존 slot-aware Abyss plan/apply를 사용한다. 슬롯 ID, 공개 modifier의 실제 값, 최소 modifier level, revealPool, 분열 및 가족 충돌을 보존한다.
- 숨은 슬롯이 있는 Chaos는 실제 제거 후보에 숨은 슬롯을 포함한다. 기존 source 기반 조건부 addition pool/최소 level을 사용하고, 새 수치 roll은 기존 concrete simulator에 위임한다. 숨은 옵션의 값을 만들어 넣지 않는다. 제거는 기존 simulator의 균등 정책이며 게임 확률로 확정하지 않는다.
- 모든 슬롯이 공개된 상태의 일반 화폐 및 숨은 상태의 Divine/Artificer/catalyst는 기존 concrete `apply`를 재사용하고 원래 슬롯과 제약을 다시 연결한다. 품질/소켓·원문 관측은 sampler 결과와 원래 슬롯에 보존한다. 검증을 통과하지 못하면 원본을 반환한다.
- 미공개 modifier level이 필요한 Whittling, 기존 slot-aware sampler의 미검증 조합/개별 화폐는 해당 행동만 거부한다. 이를 모든 일반 제작의 금지로 확대하지 않는다. 후보 offer가 처리 중인 슬롯 변경은 기존 계약대로 먼저 선택을 요구한다. 품질 등의 상태와 조합은 실제 기존 sampler가 허용하는지 확인한다.
- 불가 결과는 원본 state와 모든 omen을 유지한다. 예외/취소/늦은 응답은 film을 변경하지 않는다. root patch는 기존 AbortController·baseRevision·source guard 후에만 recordAbyss를 호출한다. 기존 film fork/원문·메모·제목은 재작성하지 않는다.
- `currencyMessages.ts`는 내부 reason을 사용자용 6개 언어로 바꾼다. root patch는 코드 문자열을 직접 사용자에게 노출하지 않는다.
- Sanctified 상태는 read/restore가 가능하되, add/observe/select/choose/append/remove가 모두 requireMutable로 거부된다. UI도 조작을 비활성화한다. 일반 화폐는 기존 조건 제약을 존중한다. C의 메타 필드는 위 필수 projection patch로 손실 없이 보존해야 한다.

## 18 — 우클릭 활성화/소비/기록

A의 `onActivateOmen(id)`를 root의 기존 `toggleOmen(id)`에 연결한다. A의 새 비활성 목록 필터는 목록/신규 선택에만 적용하고 과거 record를 삭제하지 않는다.

`applyBone`은 boneConsumedOmens에 해당하는 Necromancy/Lich/Putrefaction만 서버에 전달한다. root patch는 성공 후 해당 ID만 consumedOmens로 film에 기록하고 boneRemainingOmens만 draft에 남긴다. 실패/취소는 기존 활성 상태를 유지한다. `Omen_of_Sanctification` 등 다른 화폐의 징조는 그대로 남긴다. 일반 심연 화폐 helper도 뼈 전용 징조를 보존하고, 실제 backend 결과의 consumed/remaining을 검증한다. C의 Sanctification ID·메타 정의는 수정하지 않았다.

## 5 — 자산과 첨부 요청

정확한 URL·출처·SHA는 `frontend/public/assets/abyss-currency/sources.json`에 있다. Preserved Cranium은 다운로드·이미지 확인을 완료했다. 기존 룬 PNG/CSS는 변경하지 않았다. 기존 거부 URL을 다른 경로/헤더/프록시로 우회하지 않았다.

사용자에게 첨부를 요청할 정확한 이름:

- Preserved Rib, Ancient Rib
- Preserved Jawbone, Ancient Jawbone
- Preserved Collarbone, Ancient Collarbone
- Altered Collarbone

사용자가 제외한 Gnawed Rib/Jawbone/Collarbone 3종은 신규 UI 첨부 요청에 포함하지 않는다.

첨부할 채팅: **Exile-Hephaistos | B | 01a19f11-e072-7c60-876e-2a51b8fd9807** (현재 B 채팅).
새 다운로드/이미지 fetch가 실패한 항목은 재시도하지 않았다. 첨부 전까지 임의 아이콘이나 거부된 외부 URL을 product fallback으로 연결하지 않는다.

## 검증/완료 경계

검사 결과는 [VERIFICATION-25.md](./VERIFICATION-25.md)에 기록한다. 이 결과는 Worker 자체 리뷰이며, 공통 patch/C 메타/전체화면을 반영한 최종 통합 검사와 구분한다. 브라우저 자동 QA·운영 서버·DB/Redis·볼륨/54개 저장파일·설정 변경·remote push·Docker socket mount를 수행하지 않았다.

자동 승인 검토가 통합 채팅 메시지 전송을 ‘명시적 메시징 승인 근거 부족’ 사유로 거부했다. 우회 전송하지 않고 이 문서와 완료 응답으로 전달한다.
