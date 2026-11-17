package com.poe2craft.shared.crafting.domain;

import com.poe2craft.shared.item.*;
import java.util.*;
import java.util.random.RandomGenerator;

/**
 * 미공개 훼손 슬롯을 유지하면서 계획 수립·분기 선택·수치 굴림을 수행한다. 공개 확률을 추정하지 않는다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code catalog}: 이 인스턴스가 사용하는 검증된 카탈로그 ({@code ItemCatalog}).
 *   <li>{@code materials}: 시작 시 해결한 재료별 추가·교체 후보 ({@code WorkbenchMaterials}).
 *   <li>{@code rolls}: 수치 굴림과 유한 결합 분포 계산기 ({@code WorkbenchRolls}).
 *   <li>{@code validator}: 상태·입력 검증기 ({@code ItemStateValidator}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code AbyssWorkbench(catalog, materials, rolls)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
 *       catalog}(카탈로그), {@code materials}(제작재료목록), {@code rolls}(수치굴림목록).
 *   <li>{@code abyssPlan(state, action, activeOmens)}: 훼손 아이템의 다음 화폐 실행 가능성과 분기를 구한다. 입력: {@code
 *       state}(상태), {@code action}(행동), {@code activeOmens}(활성·징조목록). 반환: {@code
 *       AbyssCraftingPlan}. 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code applyAbyss(state, action, omens, random)}: 훼손 슬롯을 보존하며 화폐를 실행한다. 입력: {@code
 *       state}(상태), {@code action}(행동), {@code omens}(징조목록), {@code random}(난수생성기). 반환: {@code
 *       AbyssCraftingState}.
 *   <li>{@code applyCustomExalt(state, action, omens, random, modifierId)}: 사용자가 고른 옵션을 공통 엑잘티드
 *       규칙으로 부여한다. 입력: {@code state}(상태), {@code action}(행동), {@code omens}(징조목록), {@code
 *       random}(난수생성기), {@code modifierId}(옵션·식별자). 반환: {@code AbyssCraftingState}. 사용하는 예외: {@code
 *       IllegalArgumentException}.
 *   <li>{@code applyAbyss(state, action, omens, random, forcedModifierId)}: 훼손 슬롯을 보존하며 화폐를 실행한다.
 *       입력: {@code state}(상태), {@code action}(행동), {@code omens}(징조목록), {@code random}(난수생성기),
 *       {@code forcedModifierId}(forced·옵션·식별자). 반환: {@code AbyssCraftingState}. 사용하는 예외: {@code
 *       IllegalArgumentException}.
 * </ul>
 */
final class AbyssWorkbench {
  private final ItemCatalog catalog;
  private final WorkbenchMaterials materials;
  private final WorkbenchRolls rolls;
  private final ItemStateValidator validator;

  AbyssWorkbench(ItemCatalog catalog, WorkbenchMaterials materials, WorkbenchRolls rolls) {
    this.catalog = catalog;
    this.materials = materials;
    this.rolls = rolls;
    validator = new ItemStateValidator(catalog);
  }

  public AbyssCraftingPlan abyssPlan(
      AbyssCraftingState state, WorkbenchCurrency action, Set<String> activeOmens) {
    if (!state.item().conditions().isEmpty()) return AbyssCraftingPlan.blocked("ITEM_CONDITION");
    state.validate(catalog);
    if (!AugmentSocketRules.supportedState(state.knownItem())
        || !validator.validate(state.knownItem()).isEmpty())
      throw new IllegalArgumentException("Unsupported concrete item state");
    if (state.offer() != null) return AbyssCraftingPlan.blocked("RESOLVE_OFFER_BEFORE_CURRENCY");
    if (action == null || state.item().rarity() != ItemState.Rarity.RARE)
      return AbyssCraftingPlan.blocked("RARITY_NOT_APPLICABLE");
    var omens = WorkbenchOmens.parseOmens(activeOmens);
    var matches = WorkbenchOmens.matching(action, omens);
    if (matches.size() > 1
        || matches.size() != omens.size()
        || matches.contains(WorkbenchOmen.GREATER_EXALTATION))
      return AbyssCraftingPlan.blocked("ABYSS_OMEN_COMBINATION_UNSUPPORTED");
    if (matches.contains(WorkbenchOmen.WHITTLING))
      return AbyssCraftingPlan.blocked("UNREVEALED_MODIFIER_LEVEL_UNKNOWN");
    if (state.item().itemLevel() < action.minimumModifierLevel()
        || (action.minimumModifierLevel() > 0
            && matches.stream().anyMatch(o -> !o.supportsTieredCurrency())))
      return AbyssCraftingPlan.blocked("ITEM_LEVEL_OR_OMEN_UNSUPPORTED");
    var omen = matches.isEmpty() ? null : matches.getFirst();
    boolean addition = action.baseAction() == CraftingAction.EXALTED;
    boolean annulment = action == WorkbenchCurrency.ANNULMENT;
    boolean replacement = !materials.replacementTargets(action).isEmpty();
    if (replacement
        && state.knownItem().explicits().stream()
                .filter(m -> AugmentCraftingRules.crafted(m, catalog))
                .count()
            >= AugmentCraftingRules.craftedLimit(state.item()))
      return AbyssCraftingPlan.blocked("CRAFTED_LIMIT");
    if ((!addition && !annulment && !replacement)
        || action == WorkbenchCurrency.ESSENCE_ABYSS
        || action.isLiquid()) return AbyssCraftingPlan.blocked("ABYSS_OPERATION_UNSUPPORTED");
    var branches = new ArrayList<AbyssCraftingPlan.Branch>();
    if (replacement && materials.targetFirstEssence(action)) {
      var plan = materials.essenceReplacementPlan(state, action, omen);
      if (!plan.available()) return AbyssCraftingPlan.blocked(plan.reason());
      for (var outcome : plan.outcomes())
        for (var removed : outcome.removals()) {
          long denominator = (long) plan.outcomes().size() * outcome.removals().size();
          branches.add(
              new AbyssCraftingPlan.Branch(
                  removed.id(),
                  1.0 / denominator,
                  state.remove(removed.id(), null, catalog),
                  List.of(outcome.target()),
                  true,
                  denominator));
        }
      return new AbyssCraftingPlan(true, "", branches);
    }
    if (addition) {
      var candidates = new AbyssProbabilityComparison(catalog).pool(state, action, omen);
      if (candidates.isEmpty()) return AbyssCraftingPlan.blocked("NO_ELIGIBLE_ADDITION");
      branches.add(new AbyssCraftingPlan.Branch(null, 1, state, candidates, false));
    } else {
      var removals = state.removalCandidates(omen == null ? null : omen.affix());
      if (removals.isEmpty()) return AbyssCraftingPlan.blocked("NO_ELIGIBLE_REMOVAL");
      for (var removed : removals) {
        var remaining = state.remove(removed.id(), omen == null ? null : omen.affix(), catalog);
        var targets = new ArrayList<ModifierDefinition>();
        for (var id : annulment ? List.<String>of() : materials.replacementTargets(action)) {
          var target = catalog.find(id).orElse(null);
          if (target == null) return AbyssCraftingPlan.blocked("UNREVIEWED_REPLACEMENT");
          var values = new HashMap<String, Long>();
          target.stats().forEach(stat -> values.put(stat.id(), stat.min()));
          try {
            remaining.append(removed.id(), new ModifierInstance(id, values), catalog);
          } catch (IllegalArgumentException invalid) {
            return AbyssCraftingPlan.blocked("REPLACEMENT_BRANCH_CONFLICT");
          }
          targets.add(target);
        }
        branches.add(
            new AbyssCraftingPlan.Branch(
                removed.id(), 1.0 / removals.size(), remaining, targets, true));
      }
    }
    return new AbyssCraftingPlan(true, "", branches);
  }

  public AbyssCraftingState applyAbyss(
      AbyssCraftingState state,
      WorkbenchCurrency action,
      Set<String> omens,
      RandomGenerator random) {
    return applyAbyss(state, action, omens, random, null);
  }

  public AbyssCraftingState applyCustomExalt(
      AbyssCraftingState state,
      WorkbenchCurrency action,
      Set<String> omens,
      RandomGenerator random,
      String modifierId) {
    if (action == null || action.baseAction() != CraftingAction.EXALTED || modifierId == null)
      throw new IllegalArgumentException("Custom Exalted modifier required");
    return applyAbyss(state, action, omens, random, modifierId);
  }

  private AbyssCraftingState applyAbyss(
      AbyssCraftingState state,
      WorkbenchCurrency action,
      Set<String> omens,
      RandomGenerator random,
      String forcedModifierId) {
    var plan = abyssPlan(state, action, omens);
    if (!plan.available()) throw new IllegalArgumentException("Abyss currency unavailable");
    var branch = plan.branches().getLast();
    if (plan.branches().stream().allMatch(b -> b.probabilityDenominator() == 0)) {
      branch = plan.branches().get(random.nextInt(plan.branches().size()));
    } else {
      double drawBranch = random.nextDouble();
      for (var candidate : plan.branches()) {
        drawBranch -= candidate.removalProbability();
        if (drawBranch < 0) {
          branch = candidate;
          break;
        }
      }
    }
    if (branch.candidates().isEmpty()) return branch.remaining();
    ModifierDefinition chosen;
    if (branch.uniformSelection()) {
      chosen = branch.candidates().get(random.nextInt(branch.candidates().size()));
    } else {
      long draw =
          random.nextLong(branch.candidates().stream().mapToLong(ModifierDefinition::weight).sum());
      chosen = branch.candidates().getLast();
      for (var candidate : branch.candidates()) {
        draw -= candidate.weight();
        if (draw < 0) {
          chosen = candidate;
          break;
        }
      }
    }
    if (forcedModifierId != null)
      chosen =
          branch.candidates().stream()
              .filter(d -> d.id().equals(forcedModifierId))
              .findFirst()
              .orElseThrow(() -> new IllegalArgumentException("Ineligible custom modifier"));
    var instance = rolls.roll(chosen, random, new ArrayList<>());
    if (!materials.replacementTargets(action).isEmpty()) instance = instance.asCrafted();
    String id = branch.removedSlotId();
    if (id == null) {
      int index = 0;
      var occupied = state.slots().stream().map(AbyssCraftingState.Slot::id).toList();
      while (occupied.contains("added-" + index)) index++;
      id = "added-" + index;
    }
    return branch.remaining().append(id, instance, catalog);
  }
}
