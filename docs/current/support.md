# 지원 범위

기준: 애플리케이션 `dbd76a7`, 문서 통합 2026-10-28. 코드 연결을 확인한 명세이며 이번 문서 작업에서 앱 테스트를 재실행한 결과는 아닙니다.


## 베이스와 언어

Workbench 등록 베이스 식별자는 top-bases의 117개와 base-policies의 legacy 17개를 합친 **134개**다. 이는 등록 식별자 수이며 게임 전체 장비 수, 모든 화폐 적용 가능 수, 자동 탐색 성공 베이스 수가 아니다. 과거 113·125 같은 확장 중간 수치를 현행 집계로 사용하지 않는다.

등록 장비에는 방어구·장신구·무기·보조 장비·일부 주얼이 포함된다. 실제 화폐 자격은 베이스 family, 희귀도, 아이템 레벨, 옵션·품질·소켓 정책에 따라 달라진다. 자동 kernel은 검토 카탈로그와 지원 family 연결이 필요하므로 134개 등록만으로 자동 탐색 자격을 보장하지 않는다.

UI locale은 `en`, `ko`, `zh-CN`, `zh-TW`, `ja`, `es`이며 기본값은 `ko`다. 누락 번역은 영어/원문으로 돌아갈 수 있다. 영어 아이템 복사 텍스트 입력 지원과 UI 6개 언어 지원은 서로 다른 범위다.

## 자동 정책과 제한

정책 `approved-187-2026-10-28-six-affix-v5`의 187개 항목은 USE 33, CONDITIONAL 16, EXCLUDED 109, DEFERRED 29로 분류된다. 이는 자동 정책 분류이며 Workbench 재료 메뉴 수나 적용 가능한 방법 수와 같지 않다. 조건부 항목은 상태별 자격을 추가로 검사한다.

시작 상태와 목표의 명시 옵션 탐색 상한은 6개다. 장비·희귀도별 한도가 더 낮으면 그 한도가 우선한다. 7옵션이나 접두/접미 초과를 허용하는 변경이 아니다. 표시된 텍스트 줄 수가 옵션 인스턴스 수와 같지 않을 수 있다.

[2026-10-15 승인 원문](../crafting-currency-policy-approved-20261015.md)의 4옵션은 역사적 승인 경계다. 현행 6옵션과 기존 계산 예산·저장 호환은 [자동 탐색](automatic-planning.md)을 따른다.

## 미확인 경계

모든 베이스×화폐×목표 조합의 완전성, 실제 게임 확률, 최신 게임 패치와의 일치, 대형 목표의 예산 내 완료는 보장하지 않는다. 미구현·보류 자료를 구현 완료로 간주하지 않는다.


## 구현 근거

[BaseRegistry.java](../../backend/src/main/java/com/poe2craft/item/BaseRegistry.java) · [baseRegistry.ts](../../frontend/src/features/crafting/baseRegistry.ts) · [i18n.ts](../../frontend/src/shared/i18n/i18n.ts) · [AutomaticCurrencyPolicy.java](../../backend/src/main/java/com/poe2craft/crafting/domain/AutomaticCurrencyPolicy.java)

[문서 지도](../README.md)
