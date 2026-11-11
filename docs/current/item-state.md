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

일반 희귀 장비의 기본 상한과 증강물에 의한 접미 한도 확장은 공통 capacity 규칙을 따른다. 모든 상태에 고정 6개 상한을 중복 적용하지 않는다. 복합 modifier의 여러 수치 줄을 여러 옵션으로 세지 않는다. 5·6옵션 시작 상태도 베이스·희귀도·family 검증을 통과해야 하며, 4→5→6 전이는 적법한 빈 슬롯과 후보가 있을 때만 가능하다. 목표의 조건 줄 수는 [목표](goals.md)의 최소 필요 옵션 계산으로 해석한다.

품질·소켓·기폭제 규칙은 베이스별이다. 상한 변경 시 기존 품질을 보존하는 정책은 [품질 보존 문서](../workbench-quality-preserve-2026-10-04.md)를 함께 읽는다.


## 구현 근거

[ItemStateValidator.java](../../backend/src/main/java/com/poe2craft/shared/item/ItemStateValidator.java)

[문서 지도](../README.md)


축성은 분열되지 않은 명시 속성에만 적용한다. 고정 옵션·분열 옵션·구조용 수치는 축성 계수 생성과 표시 계산에서 제외한다. 현재 정책은 `unfractured-explicit-stat-percent-v3`이며 구버전 계수는 저장 근거로 읽되 고정·분열 옵션의 계산에는 쓰지 않는다. 기폭제 등 다른 표시 배율은 각 기능의 기존 규칙을 따른다. Java와 브라우저의 축성 값 일치는 `contracts/sanctification/projection-cases.json`의 공통 사례로 검증한다.
