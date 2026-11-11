# 지원 범위

수치·등록 목록의 원본은 아래 자동 생성 요약이며, 게임 확률 검증과 등록 여부는 구분한다.


## 현재 데이터 요약

아래 표는 공용 계약과 실행 카탈로그에서 생성한다. 직접 숫자를 고치지 않는다.

<!-- generated:public-contracts:start -->
| 항목 | 현행 값 |
|---|---|
| 등록 베이스 | 179 |
| UI 언어 | en, ko, zh-CN, zh-TW, ja, es, ru, th (기본 ko) |
| 증강물 | 등록 289 / 선택 244 |
| 화폐·징조 실행 정의 | 171 / 20 (보관함 표시 정책과 별개) |
| 카오스 반복 | 기본 1000, 범위 1–1000 |
| 반복 요청 본문 | 65536 UTF-8 bytes |
| 패턴 | 길이 512, 깊이 16, 문법 노드 512, 프로그램 4096 |
| 제작지원 정책 | approved-187-2026-10-30-strict-exalted-v8 |
<!-- generated:public-contracts:end -->

등록 수는 모든 화폐·목표의 적용 가능 수를 뜻하지 않는다. 실제 생성 자격은 베이스·희귀도·아이템 레벨·빈 슬롯·중복 계열·장착 증강물 규칙으로 결정한다. 제작지원 자동 탐색의 범위와 작업대 시뮬레이션의 범위는 구분한다.

## 미확인 경계

모든 베이스×화폐×목표 조합의 완전성, 실제 게임 확률, 최신 게임 패치와의 일치, 대형 목표의 예산 내 완료는 보장하지 않는다. 미구현·보류 자료를 구현 완료로 간주하지 않는다.


## 구현 근거

[BaseRegistry.java](../../backend/src/main/java/com/poe2craft/shared/item/BaseRegistry.java) · [baseRegistry.ts](../../frontend/src/features/crafting/baseRegistry.ts) · [i18n.ts](../../frontend/src/shared/i18n/i18n.ts) · [AutomaticCurrencyPolicy.java](../../backend/src/main/java/com/poe2craft/shared/crafting/domain/AutomaticCurrencyPolicy.java)

[문서 지도](../README.md)
