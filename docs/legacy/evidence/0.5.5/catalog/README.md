# 아이템·옵션 데이터 근거

- `modifiers`: 옵션 이름·원본 코드·레벨·stat 범위·가중치·생성 자격이다. 파일명은 pool, 옵션 종류, 이름과 원본 코드로 구별한다.
- `bases`: 기본 속성과 공통 원문 데이터다. 언어별 페이지는 `../i18n/sources`에 있다.
- `eligibility`: 희귀도·접두/접미 용량·선택 대상과 보류 사유다.
- `checks/content-integrity.json`: 이동 후 원문/값/경로 보존 검사다.

코드가 실제로 읽는 카탈로그는 `backend/src/main/resources/catalog`다. 여기 있는 원문을 앱 카탈로그와 임의로 바꾸지 않는다. 가중치의 후보 모집단·옵션 생성 자격과 simulator 가정을 함께 확인한다.
