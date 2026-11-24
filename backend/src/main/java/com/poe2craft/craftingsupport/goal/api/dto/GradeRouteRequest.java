package com.poe2craft.craftingsupport.goal.api.dto;

import com.poe2craft.craftingsupport.goal.api.MalformedPayload;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter;
import com.poe2craft.craftingsupport.testroutes.application.RecipeRoutePlanner;
import com.poe2craft.shared.item.ItemState;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** item·goal로 카드를 복원하고, cardIndex의 기존 단계 순서를 선택한 화폐 등급으로만 재생한다. */
public record GradeRouteRequest(
    ItemState item, GoalFilter goal, Integer cardIndex, List<GradeStep> steps) {

  /** action은 선택한 등급의 화폐, omens·socketIndex는 재료 적용 조건, acquired·lost는 지켜야 할 목표 번호 기록이다. */
  public record GradeStep(
      String action,
      List<String> omens,
      List<Integer> acquired,
      List<Integer> lost,
      Integer socketIndex) {
    /** 응답에서 받은 확률을 입력으로 사용하지 않고 서버가 검증할 단계만 만든다. */
    public RecipeRoutePlanner.Step step() {
      if (action == null
          || action.isBlank()
          || omens == null
          || omens.stream().anyMatch(Objects::isNull)
          || !validNumbers(acquired)
          || !validNumbers(lost)
          || (socketIndex != null && socketIndex < 0)) throw new MalformedPayload();
      return new RecipeRoutePlanner.Step(action, omens, acquired, lost, socketIndex);
    }

    /** 목표 번호는 카드에 붙인 1부터 6 사이의 번호이고 한 단계에서 중복되지 않는다. */
    private static boolean validNumbers(List<Integer> numbers) {
      return numbers != null
          && numbers.size() <= 6
          && numbers.stream().allMatch(n -> n != null && n >= 1 && n <= 6)
          && new HashSet<>(numbers).size() == numbers.size();
    }
  }
}
