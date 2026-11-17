package com.poe2craft.workbench.application;

import com.poe2craft.shared.crafting.application.CraftingOperations;
import com.poe2craft.shared.item.*;
import com.poe2craft.shared.item.text.*;

/**
 * 가져오기·편집에서 한 번의 텍스트 분석과 매핑 결과를 공유한다.
 *
 * <p>기존 계약: One parse/mapping operation shared by imports and edits. No mutation of the supplied
 * checkpoint.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code crafting}: 제작 ({@code CraftingOperations}).
 *   <li>{@code mapper}: 입력·출력 표현을 변환하는 구성요소 ({@code SolarTextMapper}).
 *   <li>{@code parser}: 파서 ({@code ItemTextService}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code ItemTextPreparation(crafting, mapper, parser)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
 *       crafting}(제작), {@code mapper}(변환기), {@code parser}(파서).
 *   <li>{@code prepare(text, before)}: prepare 처리에서 {@code parser.parseText}, {@code
 *       mapper.map}을(를) 사용한다. 입력: {@code text}(텍스트), {@code before}(변경전). 반환: {@code Prepared}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Prepared}: {@code ItemTextPreparation}에서 사용하는 준비된의 불변 값이다.
 * </ul>
 */
public final class ItemTextPreparation {
  private final CraftingOperations crafting;
  private final SolarTextMapper mapper;
  private final ItemTextService parser;

  public ItemTextPreparation(
      CraftingOperations crafting, SolarTextMapper mapper, ItemTextService parser) {
    this.crafting = crafting;
    this.mapper = mapper;
    this.parser = parser;
  }

  /**
   * {@code ItemTextPreparation}에서 사용하는 준비된의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code parsed}: 분석결과 ({@code ItemModels.Item}).
   *   <li>{@code mapping}: 매핑 ({@code SolarTextMapper.Result}).
   * </ul>
   */
  public record Prepared(ItemModels.Item parsed, SolarTextMapper.Result mapping) {}

  public Prepared prepare(String text, ItemState before) {
    var parsed = parser.parseText(text);
    var mapped = mapper.map(parsed);
    if (mapped.mapped()
        && before != null
        && before.baseItemId().equals(mapped.state().baseItemId())) {
      boolean importingSanctified =
          parsed.flags().stream().anyMatch(line -> line.raw().equals("Sanctified"))
              && !before.conditions().contains(ItemState.Condition.SANCTIFIED);
      if (!importingSanctified) {
        var merged = ItemTextEditor.merge(before, mapped, crafting.abyssCatalog(before));
        // These existing edit restrictions mean a complete replacement, preserving the old film.
        boolean replacement =
            !merged.mapped()
                && !before.conditions().contains(ItemState.Condition.SANCTIFIED)
                && merged.issues().stream()
                    .anyMatch(
                        issue ->
                            issue.message().contains("Socket and Rune lines")
                                || issue
                                    .message()
                                    .contains("catalyst quality and special conditions")
                                || issue.message().contains("Modifier edits cannot reconstruct"));
        if (!replacement) mapped = merged;
      }
    }
    return new Prepared(parsed, mapped);
  }
}
