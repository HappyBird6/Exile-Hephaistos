# 텍스트 파싱과 매핑

기준: 애플리케이션 `dbd76a7`, 문서 통합 2026-10-28. 코드 연결을 확인한 명세이며 이번 문서 작업에서 앱 테스트를 재실행한 결과는 아닙니다.


## 표시용 파싱

`POST /api/v1/items/parse`는 영어 PoE2 복사 텍스트를 표시용 구조로 변환한다. UTF-8 입력 한도는 16 KiB이며 원문·미해석 줄·경고를 보존한다. 이 결과 자체는 catalog 검증이나 화폐 적용 결과가 아니다. [파싱 OpenAPI](../openapi-item.yaml)는 이 API의 계약 자료다.

## Workbench 매핑

`POST /api/v1/crafting/workbench/map-text`는 파서 결과를 검토된 카탈로그에 매핑한다. 따라서 과거의 “파서와 catalog 미연결” 설명은 현행 전체 동작 설명으로 사용할 수 없다. 식별할 수 없는 베이스·옵션·수치를 임의로 보완하지 않으며, 제작 상태 검증은 [ItemState](item-state.md)를 따른다.

영어 텍스트 입력과 다국어 UI는 별개다. UI가 한국어여도 모든 언어의 게임 복사 텍스트를 지원한다고 가정하지 않는다. 파싱 API는 사용자 저장 작업 생성 API가 아니다.

## 근거와 유지보수

정규화·경고·필드의 상세 구현은 아래 서비스와 기존 API 계약을 기준으로 확인한다. 이전 설명·참고 출처는 [원문](../archive/2026-10/spec-snapshots-49190b5/docs__item-text-parsing.md.txt)에 보존했다. 외부 자료는 [MIT 고지](../third-party/PathOfBuilding-LICENSE.txt)를 유지한다.


## 구현 근거

[ItemTextService.java](../../backend/src/main/java/com/poe2craft/item/testparser/ItemTextService.java) · [WorkbenchController.java](../../backend/src/main/java/com/poe2craft/crafting/presentation/WorkbenchController.java)

[문서 지도](../README.md)
