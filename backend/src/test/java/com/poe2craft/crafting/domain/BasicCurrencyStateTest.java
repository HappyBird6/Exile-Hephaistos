package com.poe2craft.crafting.domain;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.item.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class BasicCurrencyStateTest {
  final BasicCurrencyTransitionsTest fixture = new BasicCurrencyTransitionsTest();

  @Test
  void reorderedStateCollectionsAreCanonicalWhileEveryValueAndProvenanceFieldIsRetained() {
    var mods = List.of(fixture.mod("a", 1), fixture.mod("b", 3));
    var item = fixture.item(ItemState.Rarity.RARE, mods);
    var reversed = fixture.item(ItemState.Rarity.RARE, List.of(mods.getLast(), mods.getFirst()));
    assertThat(fixture.state(item)).isEqualTo(fixture.state(reversed));
    assertThat(fixture.state(item).canonicalKey())
        .isEqualTo(fixture.state(reversed).canonicalKey());
    var explicitMap1 = new LinkedHashMap<String, Long>();
    explicitMap1.put("cold", 1L);
    explicitMap1.put("all", 2L);
    var explicitMap2 = new LinkedHashMap<String, Long>();
    explicitMap2.put("all", 2L);
    explicitMap2.put("cold", 1L);
    var joint1 =
        fixture.item(ItemState.Rarity.RARE, List.of(new ModifierInstance("joint", explicitMap1)));
    var joint2 =
        fixture.item(ItemState.Rarity.RARE, List.of(new ModifierInstance("joint", explicitMap2)));
    assertThat(fixture.state(joint1).canonicalKey())
        .isEqualTo(fixture.state(joint2).canonicalKey());
    var changes =
        List.of(
            new ItemState(
                "other-snapshot",
                item.baseItemId(),
                item.itemLevel(),
                item.rarity(),
                item.implicits(),
                item.explicits(),
                item.conditions()),
            new ItemState(
                item.snapshotId(),
                "other-base",
                item.itemLevel(),
                item.rarity(),
                item.implicits(),
                item.explicits(),
                item.conditions()),
            new ItemState(
                item.snapshotId(),
                item.baseItemId(),
                81,
                item.rarity(),
                item.implicits(),
                item.explicits(),
                item.conditions()),
            new ItemState(
                item.snapshotId(),
                item.baseItemId(),
                item.itemLevel(),
                ItemState.Rarity.MAGIC,
                item.implicits(),
                item.explicits(),
                item.conditions()),
            new ItemState(
                item.snapshotId(),
                item.baseItemId(),
                item.itemLevel(),
                item.rarity(),
                List.of(new ModifierInstance("implicit", Map.of("spirit", 6L))),
                item.explicits(),
                item.conditions()),
            new ItemState(
                item.snapshotId(),
                item.baseItemId(),
                item.itemLevel(),
                item.rarity(),
                item.implicits(),
                item.explicits(),
                Set.of(ItemState.Condition.CORRUPTED)),
            new ItemState(
                item.snapshotId(),
                item.baseItemId(),
                item.itemLevel(),
                item.rarity(),
                item.implicits(),
                item.explicits(),
                item.conditions(),
                0),
            new ItemState(
                item.snapshotId(),
                item.baseItemId(),
                item.itemLevel(),
                item.rarity(),
                item.implicits(),
                item.explicits(),
                item.conditions(),
                null,
                new CatalystQuality(CatalystQuality.Type.FLESH, 10)),
            fixture.item(ItemState.Rarity.RARE, List.of(fixture.mod("a", 2), fixture.mod("b", 3))),
            fixture.item(
                ItemState.Rarity.RARE,
                List.of(new ModifierInstance("a", Map.of("a", 1L), true), fixture.mod("b", 3))));
    for (var changed : changes)
      assertThat(fixture.state(changed).canonicalKey())
          .isNotEqualTo(fixture.state(item).canonicalKey());
    var next = new BasicCurrencyTransitions(fixture.catalog, "next-ruleset");
    assertThat(new BasicCurrencyState(item, next.provenance()).canonicalKey())
        .isNotEqualTo(fixture.state(item).canonicalKey());
  }

  @Test
  void catalogAndStatSourceArrayOrderAreIdentitySensitiveButSetsAreCanonical() {
    var reversed = fixture.catalog(List.of(fixture.b, fixture.a, fixture.implicit));
    assertThat(BasicCurrencyState.catalogDigest(reversed))
        .isNotEqualTo(BasicCurrencyState.catalogDigest(fixture.catalog));
    var ranges =
        List.of(
            new ModifierDefinition.StatRange("x", 0, 1),
            new ModifierDefinition.StatRange("y", 0, 1));
    var joint1 =
        new ModifierDefinition(
            "a",
            "a",
            ModifierDefinition.Layer.EXPLICIT,
            ModifierDefinition.AffixType.PREFIX,
            Set.of("a"),
            1,
            1,
            1,
            "a",
            ranges,
            Set.of("fire", "cold"),
            "https://example.test/a");
    var joint2 =
        new ModifierDefinition(
            "a",
            "a",
            joint1.layer(),
            joint1.affixType(),
            joint1.familyIds(),
            1,
            1,
            1,
            "a",
            List.of(ranges.getLast(), ranges.getFirst()),
            joint1.tags(),
            joint1.sourceUrl());
    var jointSet =
        new ModifierDefinition(
            "a",
            "a",
            joint1.layer(),
            joint1.affixType(),
            joint1.familyIds(),
            1,
            1,
            1,
            "a",
            ranges,
            new LinkedHashSet<>(List.of("cold", "fire")),
            joint1.sourceUrl());
    var c1 = fixture.catalog(List.of(fixture.implicit, joint1, fixture.b));
    var c2 = fixture.catalog(List.of(fixture.implicit, joint2, fixture.b));
    var c3 = fixture.catalog(List.of(fixture.implicit, jointSet, fixture.b));
    assertThat(BasicCurrencyState.catalogDigest(c1))
        .isNotEqualTo(BasicCurrencyState.catalogDigest(c2));
    assertThat(BasicCurrencyState.catalogDigest(c1))
        .isEqualTo(BasicCurrencyState.catalogDigest(c3));
    // Same advertised snapshot/digests cannot conceal a changed numeric range.
    var changed =
        fixture.catalog(
            List.of(
                fixture.implicit,
                fixture.definition("a", ModifierDefinition.AffixType.PREFIX, 1, 3, 1),
                fixture.b));
    assertThat(
            new BasicCurrencyTransitions(changed, BasicCurrencyTransitionsTest.RULESET)
                .provenance())
        .isNotEqualTo(fixture.kernel.provenance());
  }

  @Test
  void binaryLengthsSeparateAmbiguousTextAndConditionsOrderingIsStable() {
    var item = fixture.item(ItemState.Rarity.RARE, List.of());
    var first =
        new ItemState("a\nb", "c", 82, item.rarity(), item.implicits(), item.explicits(), Set.of());
    var second =
        new ItemState("a", "b\nc", 82, item.rarity(), item.implicits(), item.explicits(), Set.of());
    assertThat(fixture.state(first).canonicalKey())
        .isNotEqualTo(fixture.state(second).canonicalKey());
    var left =
        new ItemState(
            item.snapshotId(),
            item.baseItemId(),
            82,
            item.rarity(),
            item.implicits(),
            item.explicits(),
            new LinkedHashSet<>(
                List.of(ItemState.Condition.CORRUPTED, ItemState.Condition.MIRRORED)));
    var right =
        new ItemState(
            item.snapshotId(),
            item.baseItemId(),
            82,
            item.rarity(),
            item.implicits(),
            item.explicits(),
            new LinkedHashSet<>(
                List.of(ItemState.Condition.MIRRORED, ItemState.Condition.CORRUPTED)));
    assertThat(fixture.state(left).canonicalKey()).isEqualTo(fixture.state(right).canonicalKey());
  }
}
