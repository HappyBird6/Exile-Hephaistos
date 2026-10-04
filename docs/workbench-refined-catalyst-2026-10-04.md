# Sapphire refined catalyst와 quality cap 변경 모델

기준 `1b8a8884aa1ce13e260c0b1d4422cfa6f9174349`, branch `workbench/20261002`, 단독 writer. 기존 실패·복구 이력과 제외 범위를 유지한다. 사용자의 중단 없는 후속 작업 및 가역 추천 결정 지시에 따라 구현한다. remote push/merge/deploy/release는 범위 밖이다.

## Sapphire 시작 아이템

[PoE2DB Sapphire](https://poe2db.tw/us/Sapphire)의 `Metadata/Items/Jewels/JewelInt`, `jewel/intjewel`, `Mods.enable_rarity=magic, rare, unique`를 확인했다. Unique와 특수 Jewel은 미지원이다. 새 시작 아이템은 Magic이며 편집에서 Magic/Rare를 선택할 수 있다. Normal로 시작하거나 기존 장비 3P/3S 용량을 상속하지 않는다.

시작 아이템 편집 범위는 옵션 없음 또는 실제 Sapphire ordinary 원문에서 확인한 `of Enchanting` suffix 하나다. 2–4% Cast Speed, level1, `IncreasedCastSpeed` family, `caster/speed` tags와 `intjewel` eligibility를 보존한다. `display_cast_speed_percent`는 해당 원문 표시 단위에서 만든 내부 key이며 game stat ID로 주장하지 않는다. tier1은 이 편집용 단일 행의 ordinal이다. catalog의 용량0P/1S는 보수적인 **제품 편집 범위**이지 Jewel의 실제 전체 slot 규칙이 아니다. weight0은 생성 제외를 뜻하며 원문의 DropChance1을 확률 weight로 사용하지 않는다. 193 mixed Jewel mods나 전체58 Sapphire ordinary rows를 pool로 복사하지 않는다.

ordinary13은 기존 Ring/Amulet, refined13은 이 Sapphire에만 적용한다. [Catalysts](https://poe2db.tw/us/Catalysts)의 Jewel 구분과 유형 교체, [Quality](https://poe2db.tw/us/Quality)의 기본 cap20을 근거로 한다. 모든 special Jewel에 cap20을 일반화하지 않는다. 기존 사용자 simulator 정책대로 한 번 적용하면 현재 cap까지 설정하고 원본 roll을 보존한다. HALF_UP은 중앙 정책에서 한 번만 계산한다. 선택 suffix에 Sibilant/Skittering은 일치하지만 다른11종은 NO_MATCH이며 빈 시작 아이템에도 affix를 만들어 넣지 않는다. NO_MATCH를 UI에서 명시한다.

General Jewel crafting은 Workbench initial/actions/apply에서 차단한다. 실제 전체 rarity/slot/eligibility/pool 증거가 확보되기 전에는 Annul/Chaos/Divine/Essence 등으로 확대하지 않는다. CraftSupport/StateExplorer에는 연결하지 않는다. 이름과 선택 suffix template는 metadata/family/range identity를 확인한 PoE2DB 여섯 언어만 사용한다. [수집 근거](evidence/sapphire-source-review-2026-10-04.json), bundled raw JSON과 SHA256을 보존한다. CDN hover 요청403은 기록하며 game stat identity를 추측하지 않았다.

## 가역 cap 변경 정책

`QualityCapChangePolicy.DEFAULT=CLAMP_TO_CURRENT_CAP`은 **미검증 simulator 모델**이다. accepted operation의 최종 affix 결과를 먼저 구한 뒤 `quality=min(existingQuality,newCap)`을 적용한다. 유형과 살아남은 원본 roll은 유지한다. Solar의 Breach +20 maximum prefix가 제거되면 quality40→20이다. cap이 다시40으로 늘어도 quality20을 자동 증가시키지 않으며 다음 catalyst 사용이40으로 설정한다.

cap modifier는 기존 제거 후보에 그대로 남고 Omen side/Whittling filter와1/N 제거·weighted replacement odds를 변경하지 않는다. 선택된 결과에서 cap이 감소한 경우만 `unverified-quality-cap-clamp-v1` ledger를 남긴다. source는 cap의 근거이며 제거 후 game 동작의 근거가 아니다. Frontend는 before/after cap, 유형, 정확한 amount, 실제 REMOVE event와 ledger bounds를 검사하며 누락·조작된 clamp 응답이나 자동 refill을 거절한다. film은 기존 schema로 before/after state와 ledger를 저장한다.

rollback은 `QualityCapChangePolicy.REJECT_OVERCAP` 선택으로 기존 전체 action 거절 모델을 복구할 수 있다. 데이터 migration/reset이 필요 없다. rounding 정책과 cap 모델은 독립적이다. 정책 수정 시 ruleVersion을 변경한다. 사용자 인게임 cap 삭제 검증 후 이 선택을 교체할 수 있다.

## 검증

### 최종 재개 결과

사용자의 명시된 계속 작업 지시에 따른 제한 복구를 완료했다. 기존 1485개 binding이 baseline과 정확히 같으며 유일한 추가는 `sapphire:suffix:of-enchanting`임을 확인했다. 고정 개수 검사 삭제 대신 기존 1485개와 정확한 추가 identity, source fixture의 원문·stat 범위 및 전체 6개 언어 coverage를 함께 검증한다.

최종 Backend 370 unit/ArchUnit + 6 integration PASS, Frontend npm ci/lint/typecheck/format/369 unit/build PASS, API 2090(640+1004+446), browser 565(125+224+113+103), page error0이다. 반복 실행의 부분 통과 건수는 더하지 않았다. 모든 6개 언어 Sapphire 1440/390px와 Rare 시작 편집, cap 제거 film 화면을 자체 검토했다. 체크 입력과 최종 `src` 파일 Backend215/Frontend107개가 동일하며 runtime JAR hash도 검증 JAR과 같다. [최종 evidence](evidence/workbench-refined-catalyst-validation-2026-10-04.json).

refined13 모두 Magic/Rare 및 빈 affix/검증된 suffix 상태에서 applied=true, 반복·유형 교체·원래 roll·cap20·NO_MATCH를 검증했다. 일반 catalyst의 Jewel 거부와 refined의 Ring/Solar 거부, 일반 Jewel crafting 거부를 유지한다. UI Rare 품질7 입력 보존과21 거부, New craft Magic, film/reload, cap40 이전/20 이후 frame 및 ledger를 확인했다. Omen filter별 후보/확률, cap 재증가 후 품질20 유지와 다음 catalyst40도 확인했다. 지원144는 최소 한 검증된 base에서의 positive 지원이며 모든 base×currency 지원이 아니다.

재개 중 추가 TypeScript fixture 존재 guard 실패와 browser harness의 탭 이름·좌클릭/우클릭·숨겨진 dialog의 중복 문구 selector 실패도 모두 보존했다. 제품 assertion을 skip하거나 완화하지 않았다. 최종 제품 구현은 첫 checkpoint와 같고 재개 변경은 coverage 테스트·QA harness·근거 정리다. 격리 Compose만 정상 종료하고 사용자18080/18081·데이터·프로필을 보존했다. heavy QA를 해제하며 로컬 commit까지만 수행한다. 아래 중단 기록은 이전 checkpoint의 이력이다.

### 이전 checkpoint 이력

현재 checkpoint는 **검증 미완료**다. Backend `spotlessApply check generateJooq bootJar`는 unit/ArchUnit 370건 및 Docker integration 6건을 실패·오류·skip 없이 통과했다. Frontend는 격리 Node24 환경에서 `npm ci`, lint, typecheck, format 검사를 통과했지만 전체 unit 결과는 368 PASS / 1 FAIL(50 files, 369 tests)이다. `localizedModifiers.test.ts:28`의 기존 고정 개수 1485가 Sapphire binding 추가 후 1486과 불일치한다. 이 검사를 skip하거나 기대값을 완화하지 않았다.

실패 이력은 격리 QA 디렉터리에 보존한다: 최초 Backend ArchUnit 경계 및 fixture 범위 실패 2건, 최초 Node22 engine 불일치, 첫 복구 Frontend fixture formatting 실패, 두 번째 복구 후 위 고정 개수 실패. 작업 전체 수정·재검증 최대 2회 경계에 도달하여 추가 수정과 재검증을 중단했다. Frontend build 및 최종 API/browser 실행·스크린샷 검토는 실행하지 않았으며 전체 지원 완료 또는 회귀 검증 PASS로 표시하지 않는다. 후속 재개 시 고정 개수만 단순 변경하기보다 기존 1485 identity 보존과 추가된 정확한 Sapphire identity를 함께 검증해야 한다.

새 QA runtime Compose는 시작하지 않았다. 기존 사용자 18080/18081, browser/localStorage, DB volumes는 변경하지 않았다. 재개용 API/browser harness는 `scripts/qa/refined-catalyst-20261004/`에 남긴다. 로컬 checkpoint commit은 완료 반영을 의미하지 않으며 원격 push/merge/deploy는 수행하지 않는다.

새 격리 QA 경로 `E:\WORK\Exile-Hephaistos\codex\refined-catalyst-20261004`. 원본 checkout은 검증 실행의 read-only 입력이며 build/runtime은 복사본에서 수행한다. 이전 output-overwrite 자동심사 거부를 우회하지 않는다. 사용자18080/18081, browser/localStorage, DB volumes 및 이전 evidence는 유지한다. heavy 명령은 순차 실행한다. 첫 source urllib403과 CDN403을 보존한다. 이번 후속 범위의 복구 최대2회와 이전 bundle의 실패 이력은 구분해서 기록한다. 최종 실제 검증 수와 실행하지 못한 항목은 별도 validation evidence에 기록한다.
