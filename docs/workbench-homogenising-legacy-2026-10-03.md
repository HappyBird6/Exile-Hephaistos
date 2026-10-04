# Homogenising legacy 징조 — 2026-10-03

> 최신 사용자 지시 (2026-10-04): 기폭제 서비스 제외 취소. 현재144 = 기본111 + legacy7 + 기폭제 미구현26, 보류76 = 기존65 + 신규11, registry220/구현체126. 기존 기폭제 메뉴·typed 품질 입력/API·film·모델·테스트 보존. 실제 적용과 UX는 사용자 피드백 수집 후 조정. Catalysing 및 Necromancy2·훼손 신규 개발은 계속 보류. 이번5는 구현 checkpoint이며 최종 FE/API/browser 검증 미완료. [최신 범위](workbench-service-scope-2026-10-04.md).


현재 체크포인트: **단위/API/browser 검증 완료, 로컬 commit 대상**. 기본 현재 재료111과 opt-in legacy2를 구분한다. 이전 중단과 사용자 승인에 따른 제한 재개 이력은 검증 기록과 WB-038에 보존한다.

## 근거와 가용성

[공식 0.4 Item Changes](https://www.pathofexile.com/forum/view-thread/3883495/filter-account-type/staff)는 Homogenising Exaltation / Coronation의 drop 중단과 기존 개체 작동을 함께 명시한다. 최신 재도입의 긍정 근거는 이번 검색에서 발견하지 못했다. 이것은 완전한 미래 패치 조사나 현재 drop 가능 주장으로 확대하지 않는다. 기존 획득 중단 다섯 징조에는 이 문장이 적용되지 않는다.

[PoE2DB Omen](https://poe2db.tw/us/Omen)의 카드 효과와 [Exaltation Wiki](https://www.poe2wiki.net/wiki/Omen_of_Homogenising_Exaltation), [Coronation Wiki](https://www.poe2wiki.net/wiki/Omen_of_Homogenising_Coronation)의 Mechanics를 직접 읽었다. Wiki는 공개 커뮤니티 근거이며 공식 서버 확률 측정으로 취급하지 않는다. tagless 설명의 각주는 [사용자 bug report](https://www.pathofexile.com/forum/view-thread/3851647)이며 GGG의 확인 답변이 없다. 사용자 요청에서 지정한 tagless 동작을 이 제한된 공개 근거의 모델로 구현하고 출처의 강도를 유지한다.

원문은 상위 `codex/qa-20261003/homogenising`에 보존한다. Wiki Docker 직접 읽기 HTTP200, PoE2DB HTTP200. 공식 페이지는 web 도구로 읽었고 Docker 직접 캡처는 HTTP403이었다. 우회하지 않았다. manifest는 성공 본문의 SHA-256과 거부된 캡처 경로를 구분한다.

## 확정 구현 범위

- 기존 9베이스의 ordinary Exalted / Regal 적격 후보·ilvl·양수 weight·family 충돌·prefix/suffix 용량 조건을 그대로 공유한다. 후보의 modifier `tags`와 기존 modifier tag 합집합이 하나라도 겹치는 경우만 남긴다. Spawn Tags, family, affix 방향을 tag로 대체하지 않는다. 기존 implicit과 explicit의 저장된 modifier tags를 사용하며 catalog 데이터를 보정하지 않는다.
- 기존 tag가 전혀 없으면 ordinary pool을 사용하고 성공 시 해당 징조를 한 번 소비한다. tag가 있는데 적격 교집합 후보가 없으면 적용을 거부한다.
- Homogenising Exaltation + Greater Exaltation만 확인된 same-trigger 병용으로 허용한다. 두 단계의 tag 집합은 시전 전 상태로 고정한다. 첫 추가 뒤 family/slot 적격 후보는 다시 계산하지만 첫 추가의 새 tag는 둘째 풀을 넓히지 않는다.
- double-add는 적어도 두 explicit 슬롯이 남고 모든 첫 선택 가지에 둘째 적격 후보가 있는 경우만 지원한다. 특정 막힌 가지를 제거하고 재정규화하거나 결과를 재추첨하지 않는다. one-slot, 불완전한 두 번째 가지는 미지원이다.
- 서로 다른 trigger의 활성 징조는 유지하고 해당 trigger만 소비한다. 다른 same-trigger 병용, Greater/Perfect 화폐, typed catalyst quality 상태, special conditions는 기존 미지원 경계를 유지한다.
- 실패한 요청은 Workbench의 원자적 거부로 상태·film·활성 징조·ledger를 보존한다. 실제 게임에서 실패 시 화폐나 징조가 소비되는지는 확인되지 않았으며 게임 사실로 주장하지 않는다.
- 표의 원문 weight 비율을 남은 적격 후보 안에서 사용한다. tag를 먼저 동일 확률로 뽑거나 multi-tag 후보에 여러 weight를 더하지 않는다. PoE2DB source model이며 실제 game odds와 numeric precision 불확실성은 기존대로 남는다.

## 가역적 제품 결정

사용자가 부재 중 가역적 추천안 적용을 승인했다. 두 재료는 Omen 탭에서 기본 숨김, `Show legacy Homogenising Omens`를 명시적으로 켜면 `(Legacy)` 이름과 drop 중단·동작·미확인 경계를 표시한다. 기존 favorites/film은 삭제하지 않는다. opt-in은 화면 상태이며 영구 저장 형식이나 film v1을 바꾸지 않는다. rollback은 두 enum/규칙/UI/registry 변경 commit을 되돌리면 된다. 저장된 film을 변환하거나 삭제하지 않는다.

분모는 사용자 현재 개발 inventory155 / 보류65를 유지한다. 현재 개발 구현113에는 기존111 + legacy 전용2가 포함된다. 기본 현재 지원111, 별도 legacy 지원2, 미완료42, 전체 구현121/220이다. 이것은 현재 drop 가능 재료가 113개라는 의미가 아니다. 이전 pending44 문서와 JSON은 dated audit이며 이 문서가 두 항목의 최신 상태만 대체한다. 기존 legacy5, typed quality의 미구현 소비/전이, 사용자 보류65는 그대로다.

## 추가 조사 — 이번 구현 제외

[PoE2DB Desecrated Modifiers](https://poe2db.tw/us/Desecrated_Modifiers)와 [Desecrated Wiki](https://www.poe2wiki.net/wiki/Desecrated_modifier)를 직접 읽었다. Sinistral / Dextral Necromancy는 다음 Desecration의 prefix/suffix 방향을 정하며 미공개 상태에서도 해당 슬롯을 점유한다. 공개 후보에는 일반 affix와 exclusive Desecrated 결과가 함께 들어간다. exclusive 목록만 eligible pool로 사용하면 잘못된 모델이다. 세 후보의 생성/공개 시점, 분포, 포화 상태의 교체와 특수 조건에 대한 충분한 증거 및 operational state가 없다. 두 징조는 미구현으로 남는다.

Catalysing Exaltation 카드는 all catalyst quality 소비와 관련 type의 chance 증가를 설명한다. quality-to-weight bias 함수는 확인되지 않았다. typed quality 표현 기반을 갖췄어도 소비/전이와 bias가 구현된 것은 아니다. 1/N으로 bias를 없애거나 일반 Exalted로 처리하여 완료 집계하지 않는다.

## 검증

실행 결과와 실제 API/browser 대표 경계, catalog 원문 보존은 [검증 기록](evidence/workbench-homogenising-validation-2026-10-03.json)에 기록한다. 이전 통과 결과를 이번 통과로 재사용하지 않는다. Backend 첫 검사에서 새 API 테스트가 StateBucket을 ItemState 인수로 넘기는 컴파일 오류가 발생했고 테스트 입력을 올바른 concrete state로 고쳤다. 기대값이나 production guard를 완화하지 않았다.

Backend unit353 + integration6, formatter/check/generateJooq/bootJar PASS. Frontend npm ci/lint/typecheck/format/build PASS. 재개 전체333개 중331 PASS, 변경된 legacy UI 계약2개를 고친 뒤 관련3파일30개 PASS로 총333개 고유 테스트를 검증했다. 마지막 전체333개 재실행이라고 주장하지 않는다. Runtime API269, 원문 catalog 정의1487, browser83개 검증 PASS. 최종 좁은 화면 안내 높이와 목록/검색/출처 focus는 별도 layout 검증으로 확인한다. 모든 임시 검증은 순차 실행했고 DB/volume/.env와 원본 checkout을 보존했다.

최종 UI 보정 검증: 기존 node:24-alpine에서 lint/typecheck/format:check, 관련3파일30개, build PASS. 최종 build layout10개(390px/1440px)와 screenshot 자체 리뷰 PASS. 추가 node:22-bookworm 선택 오류는 test startup 이전 native binding 실패로 기록하며, lockfile/의존성은 바꾸지 않았다.
