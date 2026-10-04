# 남은 6개 언어 표시 번역

기준은 UI 수정이 포함된 `18a2a3d4235e7ebf3e45c81f4fb6fe5f115c93b0`이다. 전용 branch는 `workbench/i18n-remaining-20261004`, worktree는 `codex/i18n-remaining-20261004`이다. 한국어 기본값과 English → Korean → zh-CN → zh-TW → Japanese → Spanish 순서 및 새 popover를 유지한다. 원래 live UI18081/API18080은 갱신하지 않는다. push·merge·deploy는 이번 작업에 포함하지 않는다.

## 현재 17개 베이스

| 분류 | 한국어 / English |
|---|---|
| 목걸이 | 태양의 목걸이 / Solar Amulet |
| 반지 | 철제 반지 / Iron Ring |
| 허리띠 | 생가죽 허리띠 / Rawhide Belt |
| 마법봉 | 조율된 마법봉 / Attuned Wand |
| 셉터 | 덜컹대는 셉터 / Rattling Sceptre |
| 활 | 조잡한 활 / Crude Bow |
| 갑옷 | 녹슨 흉갑 / Rusted Cuirass |
| 투구 | 녹슨 대헬름 / Rusted Greathelm |
| 장갑 | 다부진 미트 / Stocky Mitts |
| Basic Jewel | 루비 / Ruby; 에메랄드 / Emerald; 사파이어 / Sapphire; 다이아몬드 / Diamond |
| Time-Lost Jewel | 오래된 루비 / Time-Lost Ruby; 오래된 에메랄드 / Time-Lost Emerald; 오래된 사파이어 / Time-Lost Sapphire; 오래된 다이아몬드 / Time-Lost Diamond |

장비는 베이스별 검증된 화폐·Essence·Alloy 경로만 사용한다. 주얼 8개는 기본 화폐18종, Basic/Ancient 각각 Ruby·Emerald·Sapphire용 Liquid13종과 Diamond용3종을 지원한다. 주얼 Magic1P/1S·Rare2P/2S·Crafted 하나이며 조건부 passive-tree 효과는 계산하지 않는다. actual game weight가 없는 경로는 기존 공개 simulator 모델을 유지한다. CraftSupport/StateExplorer는 Solar Amulet 전용이다. 상세 경계는 [기존 Ancient 명세](workbench-ancient-liquid-2026-10-04.md)에 있다.

사용자 후속 범위에 따라 미래 베이스 확장은 무기·방어구의 종류별 최상위 중심이다. 현 조잡한 활·녹슨 흉갑·녹슨 대헬름 등은 초기 대표 베이스이며 이번 작업에서 제거·교체하지 않는다. 반지·목걸이는 서로 다른 implicit이 단순한 상하위 관계가 아니므로 향후 implicit별 대표 유지 여부를 정한다. 이 문서는 새 베이스를 추가하거나 기존 film을 재작성하는 승인이 아니다.

## 표시 변경과 실제 coverage

UI key 수와 visible coverage를 구분한다. [기준/현재 coverage](evidence/i18n-display-coverage-2026-10-04.json)는 Git 기준 파일과 실제 registry/catalog을 읽어 비교한다.

| 항목 | 이전 | 이번 표시 |
|---|---:|---:|
| 활성 재료 이름 | 144/170; Spanish143/170 | 170/170, 6개 언어 |
| deferred 이름 | 0/50 | 50/50, 6개 언어; 활성화하지 않음 |
| 제작 Liquid 이름·설명 | 사전0/26; English는 기존 canonical tooltip으로 표시 | 26/26, 6개 언어 |
| 베이스 이름 | 14/17 | 17/17, 6개 언어 |
| modifier binding | 2,277/2,314 | 2,314/2,314, 6개 언어 |
| 고유 template | 언어별1,653 | 언어별1,690 |
| 복합 roll 값 매핑 | 0/228 | 228/228 |
| 확인된 단일 stat 단위·부호 매핑 | raw source units | 103개 |

특수37개는 정확한 Code hover 또는 implicit base page를 조회하고 English text와 unchanged stats를 대조했다. [출처](evidence/i18n-special-sources-2026-10-04.json)를 남긴다. stat 배열 순서는 표시 숫자 순서가 아니므로 복합228개의 source 의미·ID·양 끝값으로 순서를 검증했다. [복합 매핑](evidence/i18n-compound-rolls-2026-10-04.json)은 divisor1만 사용한다. 단일103개는 분당 재생/60, permyriad 및 local critical chance/100, 명시된 reduced percentage 부호 반전의 양 끝값을 검증했다. [단위 근거](evidence/i18n-source-unit-display-2026-10-04.json). canonical roll·API·film·game precision·품질의 HALF_UP 정책을 변경하지 않는다. 변환된 분수는 기존 NumberFormat 표시를 사용하며 게임 엔진의 정밀도라고 주장하지 않는다.

Time-Lost implicit4개는 `[1000]`을 그대로 두고 locale별 “기본 반경 (출처 값)”에 해당하는 표시 label을 제공한다. 거리 단위나 Medium/Large를 추정하지 않는다. [표시 label의 자체 번역 근거](evidence/i18n-radius-display-2026-10-04.json).

item-card의 품질·기본 속성·확인되지 않은 속성 label과 품질 상세 안내도 번역한다. 품질 적용 Iron implicit의 기존 English 문자열 특례는 같은 검증된 display binding을 사용하도록 통합했다. 원문 import의 itemClass와 입력 텍스트는 그대로 보존한다.

## 공식 문구와 자체 번역/원문 예외

PoE2DB `us/kr/cn/tw/jp/sp`의 실제 locale 페이지를 읽고 stable asset/Metadata/Code identity로 연결한다. source URL·license·canonical ID는 번역 payload로 바꾸지 않는다. [재료 수집](evidence/i18n-remaining-terms-2026-10-04.json), [개별 Spanish 확인](evidence/i18n-spanish-gaps-2026-10-04.json).

- Spanish `Refined_Necrotic_Catalyst`는 category와 개별 페이지의 이름이 canonical slug로 남아 있다. 자체 이름 `Catalizador necrótico refinado`를 제공하고 provenance를 tooltip에 표시한다. 해당 tooltip과 ordinary Necrotic 설명은 개별 Spanish 페이지에 실제 번역이 있어 그 원문을 사용한다.
- Spanish deferred `Liquid_Verisium`, `Yaomacs_Orb_of_Sacrifice`, `Kopecs_Orb_of_Sacrifice`, `Kamasas_Orb_of_Sacrifice`, `Yuguls_Orb_of_Sacrifice`도 category/개별 페이지에 translated name이 없다. 자체 display name을 표시하고 공식 수집 번역이라고 주장하지 않는다. Liquid Verisium의 English 설명은 source language `en`으로 표시하며 deferred 범위를 유지한다.
- Spanish EssenceBreach/EssenceIncreasedManaPercent1/EssenceOnslaughtonKill1의 Code hover가 각각 lightning damage/gold/additional jewels라는 잘못된 문구를 돌려준다. 해당 재료의 Spanish source description으로 다시 확인해 Maximum Quality/Maximum Mana/Onslaught(Fervor)를 사용한다. 잘못된 hover와 올바른 대체 출처를 [semantic review](evidence/i18n-special-review-2026-10-04.json)에 보존한다.
- 이전 Ancient 자체 Spanish template들의 provenance는 [Ancient 명세](workbench-ancient-liquid-2026-10-04.md)를 유지한다. 이번 작업이 그 번역을 공식 문구로 재분류하지 않는다.
- source/evidence의 자유 서술·알 수 없는 서버 메시지·canonical policy IDs는 원문이다. 기존 API에 message code가 없는 경우 `serviceMessages.ts`의 exact-message/subject adapter를 사용한다. 아는 서비스 메시지는 stable display key로 번역하고 모르는 원문은 별도의 “서버·출처 원문” label과 함께 보존한다. API payload나 오류 상세를 새로 노출하지 않는다. 현재 지원 범위 밖의 자동 locale parser는 구현하지 않는다. 영어 게임 텍스트 IMPORT 제한은 유지한다.

## 검증 및 재실행

`npm test`의 `pretest`가 `scripts/check-display-coverage.mjs`를 실행한다. 실제 backend registry와 catalog에서 새 활성 재료 이름, 17개 베이스 identity, modifier의 원문·stat identity·locale template·숫자 placeholder를 검사하므로 key 수만 같은 경우 완료로 통과하지 않는다. `npm run test:translation-coverage`로 별도 실행할 수 있다. fixture에 새 active 이름이나 locale binding을 누락시키는 음성 검증도 수행한다. deferred는 실제 serviceScope를 따르며 번역 추가로 enabled가 되지 않는다.

격리 복사본 `codex/i18n-remaining-qa-20261004`, project `exile-i18n-remaining-20261004`, API/UI19180/19181을 사용한다. Backend 소스 변경이 없어 이전 UI QA의 동일 Backend JAR을 read-only로 복사하며 hash를 검증한다. FE npm ci/lint/typecheck/format/unit/build를 순차 실행하고 실제 API·headless browser context로 6개 언어의 검색·툴팁·베이스·roll·Alt·품질·film과 desktop/mobile overflow를 확인한다. screenshots는 직접 시각 검토한다. 기존 live18080/18081, 사용자 browser storage, DB/Redis volumes와 다른 프로젝트는 건드리지 않는다. 종료 시 소유 서비스만 정상 stop한다.

초기 lint 실패(컴포넌트/함수 export 분리 필요)와 새 테스트의 잘못된 한국어 기대 문구(“최대 생명력” → source의 “생명력 최대치”)를 보존한다. 단일 stat103개와 실제 품질 label을 발견한 뒤 최종 소스에 필요한 검증을 추가했다. 마지막 검증과 source 일치·스크린샷·음성 guard·종료 결과는 `evidence/i18n-remaining-validation-2026-10-04.json`에 기록한다. 자체 리뷰이며 독립 리뷰라고 주장하지 않는다.

수집/저작 scripts는 당시 원문 및 변경을 만든 기록이다. 특히 wire/split/prepare를 완성된 source 또는 기존 QA output에 재실행하지 않는다. 반복 검증용 script는 coverage guard와 browser harness다. 게임 구현 변경이나 배포 없이 이 branch의 표시 변경만 되돌릴 수 있다.

### 실제 화면에서 발견한 fallback 수정

새 special binding과 API 응답은 동일한 stat id/min/max를 갖지만 JSON 객체 property 순서가 달랐다. 기존 stringify 비교 때문에 Korean conditional Runic Ward 옵션 등이 English로 fallback했다. `verifiedTemplate`은 이제 stat 배열 순서와 각 id/min/max 값을 엄격히 비교한다. source 값 변경이나 stat 배열 순서 변경은 계속 거부하며 JSON property 순서만 의미에서 제외한다. 전체2314 binding 회귀 fixture를 API property 순서로 정규화하고 실제6언어 browser에서 확인했다. key coverage만 확인했으면 놓쳤을 표시 오류이며 실패 screenshots/JSON을 보존했다.

### Tooltip 시각 검토 보완

Traditional Chinese desktop tooltip에서 source HTML 공백 entity `&nbsp`가 문자로 노출되는 것을 발견했다. locale 전체의104개 공백 전용 줄을 display dictionary에서 제거하고 collector의 선택적 semicolon entity 처리를 수정했다. source HTML·해시·canonical payload는 보존한다. coverage guard는 같은 공백 artifact의 재유입도 거부한다. 최종 FE 검증 후 긴 Spanish/CJK tooltip을1440/390 viewport에서 재캡처했다. 기본 English import 예제의 `+15 to Spirit`은 원문 보존 범위이며, canonical Solar film의 implicit은6개 locale에서 별도로 번역 검증한다.
