# 텍스트 파싱과 매핑

기준: 애플리케이션 `dbd76a7`, 문서 통합 2026-10-28. 코드 연결을 확인한 명세이며 이번 문서 작업에서 앱 테스트를 재실행한 결과는 아닙니다.


## 표시용 파싱

`POST /api/v1/items/parse`는 영어 PoE2 복사 텍스트를 표시용 구조로 변환한다. UTF-8 입력 한도는 16 KiB이며 원문·미해석 줄·경고를 보존한다. 이 결과 자체는 catalog 검증이나 화폐 적용 결과가 아니다. [파싱 OpenAPI](../openapi-item.yaml)는 이 API의 계약 자료다.

## Workbench 매핑

`POST /api/v1/crafting/workbench/map-text`는 파서 결과를 검토된 카탈로그에 매핑한다. 따라서 과거의 “파서와 catalog 미연결” 설명은 현행 전체 동작 설명으로 사용할 수 없다. 식별할 수 없는 베이스·옵션·수치를 임의로 보완하지 않으며, 제작 상태 검증은 [ItemState](item-state.md)를 따른다.

영어 텍스트 입력과 다국어 UI는 별개다. UI가 한국어여도 모든 언어의 게임 복사 텍스트를 지원한다고 가정하지 않는다. 파싱 API는 사용자 저장 작업 생성 API가 아니다.

## 근거와 유지보수

정규화·경고·필드의 상세 구현은 아래 서비스와 기존 API 계약을 기준으로 확인한다. 외부 자료는 [MIT 고지](../third-party/PathOfBuilding-LICENSE.txt)를 유지한다.


## 참조 소스와 라이선스

기존의 고정 참조 소스·조회 시점·checksum을 옮긴 기록이다. 입력 문법 검증과 게임 규칙 검증은 구분한다.

The display parsing approach is adapted from Path of Building Community PoE2 `ItemClass:ParseRaw` at commit `ce566eac45ea8a86477f513c7ee65a1ebe60014e`:

- https://github.com/PathOfBuildingCommunity/PathOfBuilding-PoE2/blob/ce566eac45ea8a86477f513c7ee65a1ebe60014e/src/Classes/Item.lua
- SHA-256 of retrieved Item.lua: `0ca39961256eefc960da7b30b4e5cb6eb45d08037aa72bc751201cc0630af2a4`.
- MIT copyright and permission notice: [PathOfBuilding-LICENSE.txt](../third-party/PathOfBuilding-LICENSE.txt).
- Actual advanced clipboard regression fixture: https://github.com/PathOfBuildingCommunity/PathOfBuilding-PoE2/issues/2117.
- `backend/src/test/resources/item-text/pob-issue-2117.txt` SHA-256: `a8f674d8b3b2f4641eb6d9ee6eb5f58bcb8b18ca7e1c465e30b62009a698e457`. Retrieved 2026-09-23, original report's game patch/snapshot unspecified. It verifies text grammar only, never current item rules or production catalog facts.

## 구현 근거

[ItemTextService.java](../../backend/src/main/java/com/poe2craft/shared/item/text/ItemTextService.java) · [WorkbenchController.java](../../backend/src/main/java/com/poe2craft/workbench/api/WorkbenchController.java)

[문서 지도](../README.md)

## 현재 아이템 텍스트 편집

편집기를 열 때마다 현재 아이템 카드 기준의 영문 게임 복사 텍스트를 불러온다. 적용 버튼 하나로 파싱·카탈로그 매핑·상태 검증을 수행하며, 실패한 문장과 원인을 표시한다. 닫기·Escape에서는 미적용 초안을 폐기한다. 기존 세션의 원문과 기록은 보존한다.

출력은 공통 표시 계산의 축성·기폭제·증강물 효과를 반영한다. 사이트 전용 숨겨진 JSON을 게임 복사 텍스트에 끼워 넣지 않는다. 가져온 값으로 가능한 현재 상태를 구성하되, 식별하지 못한 Rune는 관측 효과를 보존한다. 미공개 훼손은 일반 훼손으로 이어간다. 반올림으로 원래 roll을 유일하게 복원할 수 없으면 검증된 역산 범위와 관측 상태를 사용하며 제작 이력을 만들어내지 않는다.

기존 상태 편집과 새 아이템 가져오기의 서버 검증 경로는 유지하지만 UI에서 별도 분석/적용 버튼을 요구하지 않는다. 비동기 요청 중 상태·초안·세션이 바뀌면 늦은 결과를 적용하지 않는다. 입력 한도와 실제 게임 텍스트의 검증 계약은 파서에 유지한다.
