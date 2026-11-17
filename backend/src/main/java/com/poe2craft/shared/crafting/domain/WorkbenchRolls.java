package com.poe2craft.shared.crafting.domain;

import com.poe2craft.shared.crafting.domain.WorkbenchSimulator.Assumption;
import com.poe2craft.shared.item.*;
import java.util.*;
import java.util.random.RandomGenerator;

/**
 * 같은 옵션의 실제 수치 굴림과 자동 탐색 분포를 한 곳에서 계산하며, 번들의 복합 분포만 캐시한다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code catalog}: 이 인스턴스가 사용하는 검증된 카탈로그 ({@code ItemCatalog}).
 *   <li>{@code coupledModifierIds}: 함께 굴리는 복합 수치 옵션 ID 집합 ({@code Set<String>}).
 *   <li>{@code automaticJointRolls}: 번들 옵션 ID별 복합 수치 분포 캐시 ({@code Map<String,
 *       List<CoupledStatRollModel.Cell>>}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code WorkbenchRolls(catalog, coupledModifierIds)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
 *       catalog}(카탈로그), {@code coupledModifierIds}(결합·옵션·식별자목록). 사용하는 예외: {@code
 *       IllegalArgumentException}.
 *   <li>{@code recordSelectionAssumptions(candidates, assumptions)}: 후보 선택에 사용한 가정을 응답 목록에 기록한다.
 *       입력: {@code candidates}(후보목록), {@code assumptions}(계산가정).
 *   <li>{@code automaticJointRolls(definition)}: 복합 수치의 유한 결합 분포를 구하고 번들 정의만 캐시한다. 입력: {@code
 *       definition}(정의). 반환: {@code Optional<List<CoupledStatRollModel.Cell>>}.
 *   <li>{@code roll(definition, random, assumptions)}: 정의된 수치 범위와 분포 가정에 따라 난수로 결과를 정한다. 입력: {@code
 *       definition}(정의), {@code random}(난수생성기), {@code assumptions}(계산가정). 반환: {@code
 *       ModifierInstance}. 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code singleStatRollAssumption(definition)}: 독립 단일 수치 굴림의 가정 정보를 구성한다. 입력: {@code
 *       definition}(정의). 반환: {@code Assumption}. 사용하는 예외: {@code IllegalArgumentException}.
 * </ul>
 */
final class WorkbenchRolls {
  private final ItemCatalog catalog;
  final Set<String> coupledModifierIds;
  private final Map<String, List<CoupledStatRollModel.Cell>> automaticJointRolls =
      new java.util.concurrent.ConcurrentHashMap<>();

  WorkbenchRolls(ItemCatalog catalog, Set<String> coupledModifierIds) {
    this.catalog = catalog;
    var coupled = new HashSet<>(coupledModifierIds);
    this.catalog.modifiers().values().stream()
        .filter(
            d ->
                (d.id().startsWith("equipment:") || d.tags().contains("augment_pool"))
                    && d.stats().size() > 1)
        .map(ModifierDefinition::id)
        .forEach(coupled::add);
    this.coupledModifierIds = Set.copyOf(coupled);
    for (var id : this.coupledModifierIds) {
      var definition =
          this.catalog
              .find(id)
              .orElseThrow(() -> new IllegalArgumentException("Unknown coupled modifier"));
      if (definition.stats().size() < 2)
        throw new IllegalArgumentException("Coupled opt-in requires multiple source stats");
    }
  }

  void recordSelectionAssumptions(
      List<ModifierDefinition> candidates, List<Assumption> assumptions) {
    var temporary = candidates.stream().filter(d -> d.tags().contains("augment_pool")).toList();
    if (!temporary.isEmpty()) {
      var policy = AugmentCraftingRules.poolWeightPolicy();
      assumptions.add(
          new Assumption(
              policy.id(),
              "eligible Augment modifier tier",
              temporary.size(),
              temporary.stream().map(ModifierDefinition::id).toList(),
              (long) policy.weightPerTier(),
              (long) policy.weightPerTier(),
              catalog.metadata().sourceUrl(),
              policy.description()));
      return;
    }
    if (catalog.metadata().weightPolicy().equals("POE2DB_AS_PUBLISHED")) {
      if (ReviewedBoots.supports(catalog.base().id())
          || ReviewedBodies.supports(catalog.base().id())
          || ReviewedHelmets.supports(catalog.base().id())
          || ReviewedGloves.supports(catalog.base().id()))
        assumptions.add(
            new Assumption(
                "poe2db-published-candidates-v1",
                "eligible ordinary modifier",
                candidates.size(),
                candidates.stream().map(ModifierDefinition::id).sorted().toList(),
                null,
                null,
                catalog.metadata().sourceUrl(),
                "Selection uses PoE2DB published weights after family, side and level restrictions."));
      return;
    }
    if (ReviewedBoots.supports(catalog.base().id()))
      assumptions.add(
          new Assumption(
              "boots-uniform-candidates-v1",
              "eligible per-base Boots ordinary modifier",
              candidates.size(),
              candidates.stream().map(ModifierDefinition::id).sorted().toList(),
              null,
              null,
              catalog.metadata().sourceUrl(),
              "USER-APPROVED SIMULATOR MODEL: equal 1/N after family, side and level restrictions. Actual game spawn weights and numeric roll probabilities remain unverified."));
    if (ReviewedBodies.supports(catalog.base().id()))
      assumptions.add(
          new Assumption(
              "body-uniform-candidates-v1",
              "eligible per-base Body ordinary modifier",
              candidates.size(),
              candidates.stream().map(ModifierDefinition::id).sorted().toList(),
              null,
              null,
              catalog.metadata().sourceUrl(),
              "USER-APPROVED SIMULATOR MODEL: equal 1/N after family, side and level restrictions. Actual game spawn weights and numeric roll probabilities remain unverified."));
    if (ReviewedHelmets.supports(catalog.base().id()))
      assumptions.add(
          new Assumption(
              "helmets-uniform-candidates-v1",
              "eligible per-base Helmet ordinary modifier",
              candidates.size(),
              candidates.stream().map(ModifierDefinition::id).sorted().toList(),
              null,
              null,
              catalog.metadata().sourceUrl(),
              "USER-APPROVED SIMULATOR MODEL: equal 1/N among eligible Helmet modifiers after family, side and level restrictions. Actual game spawn weights are unavailable; published DropChance is not a verified game weight."));
    if (BasicJewel.supportedCrafting(catalog.base().id())
        || ReviewedGloves.supports(catalog.base().id()))
      assumptions.add(
          new Assumption(
              ReviewedGloves.supports(catalog.base().id())
                  ? "gloves-uniform-candidates-v1"
                  : "sapphire-uniform-candidates-v1",
              ReviewedGloves.supports(catalog.base().id())
                  ? "eligible per-base Gloves ordinary modifier"
                  : "eligible per-base Jewel ordinary modifier",
              candidates.size(),
              candidates.stream().map(ModifierDefinition::id).toList(),
              null,
              null,
              catalog.metadata().sourceUrl(),
              ReviewedGloves.supports(catalog.base().id())
                  ? "USER-APPROVED SIMULATOR MODEL: equal 1/N among eligible Gloves modifiers after family, side and level restrictions. Actual game spawn weights are unavailable; published DropChance is not a verified game weight."
                  : "USER-APPROVED SIMULATOR MODEL: equal 1/N among the eligible normal-section candidates after family, side and level restrictions. Actual game spawn weights are unavailable; DropChance=1 is not a verified weight."));
  }

  Optional<List<CoupledStatRollModel.Cell>> automaticJointRolls(ModifierDefinition definition) {
    if (definition.stats().size() < 2
        || (!coupledModifierIds.contains(definition.id())
            && definition.stats().stream().anyMatch(s -> s.min() != s.max())))
      return Optional.empty();
    if (!catalog.find(definition.id()).filter(definition::equals).isPresent())
      return Optional.of(CoupledStatRollModel.cells(definition));
    return Optional.of(
        automaticJointRolls.computeIfAbsent(
            definition.id(), ignored -> CoupledStatRollModel.cells(definition)));
  }

  ModifierInstance roll(
      ModifierDefinition definition, RandomGenerator random, List<Assumption> assumptions) {
    if (coupledModifierIds.contains(definition.id()))
      return CoupledStatRollModel.roll(definition, random, assumptions);
    if (definition.stats().size() != 1)
      throw new IllegalArgumentException("Joint stat roll domain needs verification");
    var range = definition.stats().getFirst();
    long n = Math.addExact(Math.subtractExact(range.max(), range.min()), 1);
    long value = Math.addExact(range.min(), random.nextLong(n));
    var assumption = singleStatRollAssumption(definition);
    if (assumption != null) assumptions.add(assumption);
    return new ModifierInstance(definition.id(), Map.of(range.id(), value));
  }

  Assumption singleStatRollAssumption(ModifierDefinition definition) {
    if (definition.stats().size() != 1)
      throw new IllegalArgumentException("Joint stat roll domain needs verification");
    var range = definition.stats().getFirst();
    long n = Math.addExact(Math.subtractExact(range.max(), range.min()), 1);
    return n > 1
        ? new Assumption(
            coupledModifierIds.isEmpty()
                ? "uniform-integer-roll-v1"
                : "assumed-source-integer-roll-v1",
            range.id(),
            n,
            List.of(),
            range.min(),
            range.max(),
            definition.sourceUrl(),
            coupledModifierIds.isEmpty()
                ? "Each integer in this single-stat source range is a modeled candidate; no published roll weights."
                : "UNVERIFIED numeric model: equally sampled source-unit integers between verified bounds. Interior increments, display conversion and game distribution remain unverified; this is not an established game outcome domain.")
        : null;
  }
}
