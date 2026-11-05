# 아이템 상태

기준: 애플리케이션 `dbd76a7`, 문서 통합 2026-10-28. 코드 연결을 확인한 명세이며 이번 문서 작업에서 앱 테스트를 재실행한 결과는 아닙니다.


## 표시 데이터와 제작 상태

파서의 표시 결과와 catalog 식별자가 검증된 ItemState는 다르다. 제작 상태는 snapshot/base 식별자, 희귀도, 레벨, implicit/explicit modifier 인스턴스, 수치, 품질과 관련 상태를 검증한다. [텍스트 매핑](text-parsing.md)을 통과하지 않은 문장을 제작 옵션으로 추측하지 않는다.

## 검증 경계

- 알 수 없는 modifier, 잘못된 layer, 중복 ID, 겹치는 family, 잘못된 stat 집합과 범위 밖 수치를 거절한다.
- implicit는 해당 베이스 카탈로그 정의와 일치해야 한다. Unique 및 지원하지 않는 조건 조합을 일반 제작 상태로 허용하지 않는다.
- NORMAL은 명시 옵션 0개, MAGIC/RARE는 해당 카탈로그의 접두·접미 한도를 따른다. 모든 장비를 일괄 3+3으로 취급하지 않는다. 주얼에는 별도 용량 규칙이 있다.
- 기존 아이템 검증과 새 옵션 생성 자격 검증은 다르다. 생성에는 requiredItemLevel 조건도 적용된다.
- fractured implicit, 복수 fractured, Rare가 아닌 fractured 상태 등 지원 밖 조합을 거절한다. 기본 주얼의 소켓/fractured 제한도 별도다.

## 6옵션과 줄 수

상한은 explicit modifier 인스턴스 6개다. 복합 modifier의 여러 수치 줄을 여러 옵션으로 세지 않는다. 5·6옵션 시작 상태도 베이스·희귀도·family 검증을 통과해야 하며, 4→5→6 전이는 적법한 빈 슬롯과 후보가 있을 때만 가능하다. 목표의 조건 줄 수는 [목표](goals.md)의 최소 필요 옵션 계산으로 해석한다.

품질·소켓·기폭제 규칙은 베이스별이다. 상한 변경 시 기존 품질을 보존하는 정책은 [품질 보존 문서](../workbench-quality-preserve-2026-10-04.md)를 함께 읽는다.


## 구현 근거

[ItemStateValidator.java](../../backend/src/main/java/com/poe2craft/shared/item/ItemStateValidator.java)

[문서 지도](../README.md)
