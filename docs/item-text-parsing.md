# ItemTextService 사용과 지원 범위

`new ItemTextService().parseText(text)`로 사용자 확인용 `ParsedItemText`를 얻는다.
`tokenizeText(text)`는 원문, 행 번호(1부터), 구분선 기준 section(0부터), 빈 행을 보존한다.
기존 빈 `tokenizerText` 메서드는 `tokenizeText`로 이름과 반환형을 변경했다.
DB/Spring 의존성이나 완성된 `ItemState`는 필요하지 않다.

- 영어/한국어 아이템 종류·희귀도·이름·표시 base·아이템 레벨·요구사항을 읽는다.
- Rare/Unique 두 이름 행의 둘째 행과 Normal 한 이름 행만 표시 base 후보로 제공한다. Magic 이름, 미확인 한 줄 Rare/Unique 이름은 base를 추측하지 않는다.
- 영어의 알려진 property key는 원래 단위와 값으로 보존한다. 다른 key(한국어 세부 property 포함)는 `unparsedLines`에서 보존한다.
- 영어 suffix marker `(rune)` 등 명시된 행만 `markedModifiers`에 둔다. marker는 문자열 관찰이며 검증된 modifier 계약이 아니다.
- `Corrupted`/`타락`, `Unidentified`/`미확인`은 표시된 행만 `flags`에 보존한다. 행이 없다고 게임 상태가 false인 것은 아니다.
- 일반 옵션, 여러 줄 옵션, 고급 복사의 `{ ... }` metadata, flavour, 알 수 없는 행은 `unparsedLines`에 남긴다. 옵션 ID/tier/prefix/suffix/roll을 추측하지 않는다. 원문 전체는 항상 `text.originalText`에 남는다.
- `properties`와 `requirements`는 중복을 잃지 않는 목록이다. Item Level 중복/문법 오류/정수 overflow는 null과 경고로 반환한다. 값의 게임상 유효 범위는 catalog 검증 단계 책임이다.
- null/blank, UTF-8 16 KiB 초과, 필수 header/name 없음, header section의 중복 header, 지원하지 않는 언어 header는 `IllegalArgumentException`이다. 미지원 rarity는 UNKNOWN과 경고다.
- 모든 결과에 `CATALOG_VALIDATION_REQUIRED` 경고가 있다. 이 결과를 바로 Crafting Engine 입력으로 사용하면 안 된다.

## 형식 관찰 근거

2026-09-23 확인. 아래 공개 게시물은 사용자가 붙여 넣은 clipboard 형식 관찰 근거이며 게임 규칙/확률의 권위가 아니다. 테스트 이름·능력치·숫자는 모두 synthetic이며 실제 아이템 fixture가 아니다.

- [GGG 포럼 영문 Rare 복사 예시](https://www.pathofexile.com/forum/view-thread/3851605): header, 이름 두 줄, 구분선, properties, Requires, sockets, Item Level, rune/desecrated marker.
- [PoB-PoE2 Unique clipboard issue](https://github.com/PathOfBuildingCommunity/PathOfBuilding-PoE2/issues/1967): Unique, skill/설명문 보존 필요.
- [PoB-PoE2 advanced clipboard issue](https://github.com/PathOfBuildingCommunity/PathOfBuilding-PoE2/issues/2117): brace metadata와 roll range.
- [한국어 게임 복사 게시](https://gall.dcinside.com/mgallery/board/view/?id=poe2&no=79471): 아이템 종류, 아이템 희귀도, 요구사항 및 레벨/지능, 아이템 레벨.
- [한국어 고급 복사 게시](https://enter.dcinside.com/mgallery/board/view/?id=poe2&no=435278&page=1): 요구 사항 한 줄, 고급 속성 metadata.
- [한국어 복사 기반 도구 문서](https://poe2tools.net/poe2-%EA%B1%B0%EB%9E%98%EC%86%8C-%EC%8B%9C%EC%84%B8-%EA%B2%80%EC%83%89%EA%B8%B0/): 고유/미확인/타락 표기.
- [한국어 희귀도 검색 도구](https://reim.kr/poe2/tools/regex): 일반/마법/희귀 표기.

TODO(domain): 텍스트 옵션의 catalog ID·tier·실제 roll 해석 / 검증된 snapshot과 locale별 modifier 문법 필요 / ItemState 변환 / 현재 미해석 행과 확인 경고로 보존.
