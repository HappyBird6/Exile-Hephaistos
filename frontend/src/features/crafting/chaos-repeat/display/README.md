# Chaos 반복용 explicit 표시 자료

기준 `3f4495e664d8112011cc5dcd9f17330446167a97`. 이 폴더만 독립 추가한다. 기존 Page/formatter/i18n/CSS, 제작 규칙·기록·실행 UI에는 연결하거나 변경하지 않는다. regex 문법·case·줄/옵션/전체 매칭 단위·입력 상한·취소 정책은 결정하지 않는다.

## helper 계약

`projectExplicitOptionGroups(state, definitions, locale)`는 순수 표시 함수다. 반환은 `{ locale, groups, issues }`다. `groups`의 각 항목은 원본 `explicitIndex`, `modifierId`, definition의 `affixType`(`PREFIX`/`SUFFIX`), `fractured`, 최종 `text`, 실제 LF로만 나눈 `lines`, `textKind`를 보존한다. `lines`는 매칭 단위가 아니다. visual wrap·trim·Unicode/CRLF 정규화·줄 합치기를 하지 않는다.

입력은 `state.explicits`뿐이며 이름·고정옵션·요구조건·기본 스킬·장착 증강·tier/Alt 범위 미리보기·P1/S1 라벨·tooltip detail·미공개 훼손 문양을 포함하지 않는다. 공개 explicit이면 fractured/desecrated/crafted도 같은 접두/접미 계약으로 포함한다. 공개 훼손 slot 자체에서 modifier를 추가 추정하지 않는다. 미공개 slot은 그룹에 넣지 않고 issue로 알린다.

표시는 현재 Page의 일반(non-Alt) 체인과 같다.

1. `catalystProjection(definition, originalValues, state.catalystQuality, state)`를 재사용한다.
2. `SANCTIFIED`인 경우 `preciseModifierProjection(state, definition, modifier, 'explicits').values`로 대체한다. 이미 반올림된 Catalyst 값에 재곱하지 않는다.
3. `localizedModifierText(definition, displayedValues, locale)`에 명시한 locale을 전달한다.
4. 실제 `ItemCard.Lines`와 같이 `formatAttributeText(text, locale)`를 한 번 더 적용한다. 기존 locale edge case도 그대로 남긴다.
5. fractured 전체를 먼저 두고, 나머지를 접두·접미 순으로 안정 정렬한다. fractured끼리와 같은 affix끼리는 원본 explicit 순서를 유지한다. fractured suffix를 prefix 뒤로 재정렬하지 않는다.

원본 values·state·정의는 수정하지 않으며 반올림한 표시값을 반환 state나 저장용 값으로 사용하지 않는다. ambient `getLocale()`에 의존하지 않는다. 이 helper는 item/catalog validator나 반복 실행 자격 판정기가 아니다. 입력 stat 범위·family·base·희귀도·용량 등을 검증했다고 주장하지 않는다.

## 기존 fallback과 issue 해석

문장 생성은 기존 함수만 사용한다. helper가 복제하는 것은 private `verifiedTemplate`의 binding 식별 검사(정의 id, 영어 text, 순서가 있는 stat id/min/max)뿐이며, 이는 fallback 진단용이다. 실제 이름이나 문장을 새로 번역·치환하지 않는다.

- binding 또는 요청 locale의 일반 template이 없으면 `localizedModifierText`는 `rolledText`의 영어 표시를 `formatAttributeText(..., 'en')`로 처리한다. `UNVERIFIED_TEMPLATE_BINDING`/`ENGLISH_TEXT_FALLBACK`으로 알린다. locale=en의 기존 `modifierTranslationStatus`는 binding 없이도 `VERIFIED`를 반환하므로 진단에 그 값만 사용하지 않는다.
- `reveal:`는 요청 언어 template이 없으면 영어 template으로 돌아간다. 기존 `modifierTranslationStatus`도 이를 `VERIFIED`로 취급할 수 있으므로 실제 영어 fallback은 `ENGLISH_REVEAL_TEMPLATE_FALLBACK`으로 따로 알린다.
- `valueStats`가 있으면 source 값/divisor와 기존 `formatAttributeNumber`를 사용한다. source 값이 빠지면 기존 formatter가 영어 경로로 돌아갈 수 있다. helper는 선언된 stat 누락·비유한 값부터 issue로 반환하고 그룹을 만들지 않는다.
- 일반 template에 `valueStats`가 없는 경우, 확인된 단일 range만 실제 값으로 대체한다. 고정 stat은 게시 text를 유지한다. 변환을 알 수 없는 복합 값은 `rolledText`의 `Affix name: stat id = value (source units)` 경로로 남고, 확인된 template이 있으면 affix 이름만 번역될 수 있다. `SOURCE_UNIT_FALLBACK`과 `textKind: source-units`는 **옵션 문장 확정이 아니다. affix 이름을 매칭 문구로 간주하면 안 된다.** 원본 표시를 임의로 지우거나 정리하지 않는다.
- Jewel 특별 분기는 `valueStats`가 없을 때 safe integer만 template 숫자에 반영한다. SANCTIFIED/Jewel effect/quality가 소수를 만들면 현재 일반 ItemCard에도 원래 range가 남을 수 있다. `UNRESOLVED_ROLL_TEXT`로 알리고 그대로 golden에 보존한다. `jewel-sanctified-fraction`이 실제 예다. 이 폴더에서 기존 버그를 고치거나 지원 완료로 바꾸지 않는다.
- `UNREVIEWED_NUMERIC_SEMANTICS`와 `JEWEL_EFFECT_AND_QUALITY_PROVISIONAL`은 기존 projection status를 그대로 전달한다. 반복 기능의 허용/거부 정책을 새로 정하지 않는다.
- 없는 정의, 잘못 연결된 정의 id, affix 없는 explicit, 빠진/비유한 stat은 해당 그룹을 만들 수 없다는 issue를 반환한다. 원본 정수가 아닌 synthetic 값은 `NONINTEGER_SOURCE_ROLL`로 알리되 기존 표시 함수가 처리할 수 있으면 그 표시를 보존한다. projection이 실패하면 내부 예외 대신 `DISPLAY_PROJECTION_UNAVAILABLE`를 반환한다. 해결되지 않은 `{vN}`은 `UNRESOLVED_TEMPLATE_VALUE`다.

`issues`는 원본 explicit 순서, 그 뒤 미공개 slot 순서다. `groups`는 표시 순서다. 일부 그룹이 나왔거나 issues가 비었다고 상태/실행/매칭 지원 완료를 뜻하지 않는다. 서버 통합에서는 기존 item 검증과 별도로 fallback·미공개·미해결 issue를 검사하고 미확정 문장에 regex를 적용하지 않도록 판단해야 한다.

## machine-readable golden과 서버 대조

- `goldens/inputs.json`: 선택 정의 11개(실제 10개·id 변형 1개)와 원본 출처, display-only 합성 state 19개. 정상 범위 source roll 조합과 음수/음수 zero/소수 tie/Spanish grouping 등 범위 밖 synthetic formatter 경계를 `scope`로 구분한다. 유일한 synthetic 정의는 실제 Adherent Cuffs 정의의 id만 바꿔 binding 부재를 검사한다. 새 게임 효과·확률·weight 의미를 정의하지 않는다.
- `goldens/expected.json`: `inputId` + 명시 `locale`에 대한 반환 객체. 19 × 6 = 114개 조합이다. 각 input의 definitions/state와 연결해 서버 projector 출력을 field-by-field 비교한다. 배열 순서·정확한 UTF-8 text·LF·빈 줄을 비교하며, `lines`를 다시 붙이거나 문구를 보정하지 않는다.
- `goldens/sources.json`: 기준 commit, Node/ICU/CLDR/Unicode 버전과 표시 체인·선택한 원본 catalog·입력·helper의 SHA256. 약 3.77MB인 전체 `modifierTemplates.json`은 기존 파일 경로와 hash만 기록하고 복사하지 않는다. 라이선스·원 출처는 기존 catalog/third-party 기록을 따른다.

서버 port는 원본 integer와 divisor, Catalyst/Jewel effect/SANCTIFIED의 기존 계산 순서를 보존해야 한다. `formatAttributeText`의 decimal-span 처리와 `formatAttributeNumber`의 Intl 처리도 같지 않다. 예를 들어 원본 integer 문구는 항상 grouping되는 것이 아니며, Spanish는 4자리 `1234,6`, 5자리 `12.345,7`이 다르다. `synthetic-es-grouping-*`와 부호/0.05 경계 vector로 JVM formatter 차이를 확인한다. literal 영문 fallback이나 미해결 Jewel range를 서버에서 그럴듯한 문장으로 바꾸면 현재 UI와 동등하지 않다.

Frontend에서 다음으로 검증한다.

```powershell
npm run test -- --run src/features/crafting/chaos-repeat/display/projectExplicitOptionGroups.test.tsx --maxWorkers=1
```

golden 재생성은 같은 경로에서 `UPDATE_CHAOS_DISPLAY_GOLDENS=1` 환경변수를 설정해 위 검사를 실행한다. 일반 검사에는 파일 쓰기가 없다. 생성 모드에서도 독립적으로 복사한 Page(non-Alt) 체인 → 실제 ItemCard 렌더링과 먼저 동등성을 검사한다. 이름·라벨·details·실제 미공개 rune DOM은 렌더 결과에서 제외해 비교한다. fixture의 mixed-base 또는 범위 밖 상태를 실제 게임/서버 유효 상태로 주장하지 않는다. 임의 재생성으로 기존 실패를 덮어쓰지 말고 source hash와 변경된 vector를 함께 리뷰한다.

## 검증 · 2026-11-04

- Golden 생성 모드: 최종 **1 file / 118 tests 통과**. 일반 읽기 검증 모드와 직접 의존 영역(`localizedModifiers`, `attributeNumbers`, `preciseModifierProjection`, `ItemCard`)을 합쳐 **5 files / 148 tests 통과**했다. 결과는 이 worktree의 `frontend/node_modules/.cache/ui-a/chaos-repeat-display-final.json`이다. 실패·skip은 없었다.
- `npm run lint`, `npm run typecheck`, `npm run format:check`, `npm run build` 통과. 기준과 같은 package/lockfile 및 이전 `npm ci` 성공의 node_modules를 재사용했다. build의 기존 500 kB chunk 경고는 남는다.
- 기존 추적 제품 파일 diff는 없고, 최종 dist 379개 파일의 SHA256은 완료된 UI `3f4495e`의 dist와 모두 같다. 이 helper는 아직 Page에서 import하지 않는다. 명시한 새 display 폴더 밖의 기존 UI/formatter/i18n/backend/history/receipt/CSS 파일은 수정하지 않았다.
- 전체 Vitest는 부모의 최종 통합 단계에서 수행하므로 이 작업에서는 실행하지 않았다. 브라우저·서버·Docker·DB·Redis·원격 push는 실행하지 않았고, 실제 저장 파일·설정은 접근하거나 변경하지 않았다. worktree·의존성·dist·검증 로그를 리뷰까지 보존한다.
