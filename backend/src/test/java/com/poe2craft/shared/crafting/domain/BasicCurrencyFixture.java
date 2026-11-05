package com.poe2craft.shared.crafting.domain;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.shared.crafting.domain.BasicCurrencyTransitions.*;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.item.ModifierInstance;
import com.poe2craft.shared.item.SolarAmulet;
import com.poe2craft.shared.probability.Fraction;
import java.util.*;

/** Tiny independently enumerable test catalog; no production pool is pruned or rewritten. */
/** Independently enumerable common crafting data used by multiple service tests. */
public class BasicCurrencyFixture {
  public static final String RULESET = "synthetic-ruleset-one";
  public final ModifierDefinition implicit =
      new ModifierDefinition(
          "implicit",
          "Implicit",
          ModifierDefinition.Layer.IMPLICIT,
          ModifierDefinition.AffixType.NONE,
          Set.of("implicit"),
          1,
          0,
          0,
          "Implicit",
          List.of(new ModifierDefinition.StatRange("spirit", 5, 5)),
          Set.of(),
          "https://example.test/implicit");
  public final ModifierDefinition a = definition("a", ModifierDefinition.AffixType.PREFIX, 1, 2, 1);
  public final ModifierDefinition b = definition("b", ModifierDefinition.AffixType.SUFFIX, 3, 4, 3);
  public final ItemCatalog catalog = catalog(List.of(implicit, a, b));
  public final BasicCurrencyTransitions kernel = new BasicCurrencyTransitions(catalog, RULESET);

  public ModifierDefinition definition(
      String id, ModifierDefinition.AffixType type, long min, long max, int weight) {
    return new ModifierDefinition(
        id,
        id,
        ModifierDefinition.Layer.EXPLICIT,
        type,
        Set.of(id),
        1,
        weight,
        1,
        id,
        List.of(new ModifierDefinition.StatRange(id, min, max)),
        Set.of(),
        "https://example.test/" + id);
  }

  public ItemCatalog catalog(List<ModifierDefinition> definitions) {
    long p =
        definitions.stream()
            .filter(d -> d.affixType() == ModifierDefinition.AffixType.PREFIX)
            .count();
    long s =
        definitions.stream()
            .filter(d -> d.affixType() == ModifierDefinition.AffixType.SUFFIX)
            .count();
    long pw =
        definitions.stream()
            .filter(d -> d.affixType() == ModifierDefinition.AffixType.PREFIX)
            .mapToLong(ModifierDefinition::weight)
            .sum();
    long sw =
        definitions.stream()
            .filter(d -> d.affixType() == ModifierDefinition.AffixType.SUFFIX)
            .mapToLong(ModifierDefinition::weight)
            .sum();
    return new ItemCatalog(
        new ItemCatalog.Metadata(
            "synthetic-basic",
            "test-only",
            "https://example.test/catalog",
            "POE2DB_AS_PUBLISHED",
            "a".repeat(64),
            "b".repeat(64),
            (int) p,
            (int) s,
            pw,
            sw),
        new ItemCatalog.BaseItem(
            SolarAmulet.BASE_ID,
            "Synthetic Solar",
            "https://example.test/base",
            implicit.id(),
            1,
            1,
            3,
            3),
        definitions);
  }

  public ItemState item(ItemState.Rarity rarity, List<ModifierInstance> mods) {
    return new ItemState(
        catalog.metadata().snapshotId(),
        catalog.base().id(),
        82,
        rarity,
        List.of(new ModifierInstance("implicit", Map.of("spirit", 5L))),
        mods,
        Set.of());
  }

  public ModifierInstance mod(String id, long value) {
    return new ModifierInstance(id, Map.of(id, value));
  }

  public BasicCurrencyState state(ItemState item) {
    return new BasicCurrencyState(item, kernel.provenance());
  }

  public Result expand(ItemState item, WorkbenchCurrency action) {
    return kernel.expand(state(item), action, Set.of(), 1000);
  }

  public Map<ItemState, Fraction> masses(Result result) {
    var resultMap = new LinkedHashMap<ItemState, Fraction>();
    for (var outcome : result.outcomes())
      resultMap.put(outcome.state().item(), outcome.probability());
    return resultMap;
  }

  void normalized(Result result) {
    assertThat(
            result.outcomes().stream()
                .map(Outcome::probability)
                .reduce(Fraction.ZERO, Fraction::add)
                .add(result.unresolved()))
        .isEqualTo(Fraction.ONE);
  }
}
