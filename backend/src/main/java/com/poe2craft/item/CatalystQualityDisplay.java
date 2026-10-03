package com.poe2craft.item;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Bounded display projection. Stored values, source ranges and weights are never scaled. */
public final class CatalystQualityDisplay {
  public static final String VERSION = "catalyst-quality-display-v1";
  // Reviewed whole-number display units only. Regeneration/leech source units and skill levels
  // deliberately stay out; a matching tag alone does not prove scalable integer display units.
  private static final Set<String> INTEGER_STATS =
      Set.of(
          "base_maximum_life",
          "base_maximum_mana",
          "base_maximum_energy_shield",
          "base_evasion_rating",
          "additional_strength",
          "additional_dexterity",
          "additional_intelligence",
          "additional_all_attributes",
          "base_fire_damage_resistance_%",
          "base_cold_damage_resistance_%",
          "base_lightning_damage_resistance_%",
          "base_chaos_damage_resistance_%",
          "base_resist_all_elements_%",
          "spell_damage_+%",
          "fire_damage_+%",
          "cold_damage_+%",
          "lightning_damage_+%",
          "chaos_damage_+%",
          "physical_damage_reduction_rating_+%",
          "maximum_energy_shield_+%",
          "evasion_rating_+%",
          "base_cast_speed_+%");

  private CatalystQualityDisplay() {}

  public static List<Projection> describe(ItemState state, ItemCatalog catalog) {
    if (!new ItemStateValidator(catalog).validate(state).isEmpty())
      throw new IllegalArgumentException("Unsupported state for quality display");
    return java.util.stream.Stream.concat(state.implicits().stream(), state.explicits().stream())
        .map(m -> project(m, catalog.find(m.modifierId()).orElseThrow(), state.catalystQuality()))
        .toList();
  }

  private static Projection project(
      ModifierInstance m, ModifierDefinition d, CatalystQuality quality) {
    if (quality == null)
      return new Projection(m.modifierId(), m.values(), m.values(), "NO_TYPED_QUALITY");
    if (m.values().containsKey("local_maximum_quality_+") || d.tags().contains("unscalable"))
      return new Projection(m.modifierId(), m.values(), m.values(), "UNSCALABLE");
    if (!quality.type().matches(d.tags()))
      return new Projection(m.modifierId(), m.values(), m.values(), "NO_MATCH");
    boolean ironImplicit =
        d.id().equals("iron-ring:implicit:added-physical-damage-to-attacks")
            && m.values()
                .keySet()
                .equals(
                    Set.of(
                        "attack_minimum_added_physical_damage",
                        "attack_maximum_added_physical_damage"));
    boolean reviewed =
        ironImplicit
            || (d.weight() > 0
                && d.stats().size() == 1
                && INTEGER_STATS.contains(d.stats().getFirst().id()));
    if (!reviewed
        || d.stats().stream().anyMatch(s -> s.min() < 0)
        || m.values().values().stream().anyMatch(v -> v < 0))
      return new Projection(m.modifierId(), m.values(), m.values(), "UNREVIEWED_NUMERIC_SEMANTICS");
    var scaled = new java.util.TreeMap<String, Long>();
    m.values()
        .forEach(
            (id, value) ->
                scaled.put(
                    id,
                    BigInteger.valueOf(value)
                        .multiply(BigInteger.valueOf(100L + quality.amount()))
                        .divide(BigInteger.valueOf(100))
                        .longValueExact()));
    return new Projection(m.modifierId(), m.values(), scaled, "SCALED_INTEGER");
  }

  public record Projection(
      String modifierId,
      Map<String, Long> originalValues,
      Map<String, Long> displayedValues,
      String status) {
    public Projection {
      originalValues = Map.copyOf(originalValues);
      displayedValues = Map.copyOf(displayedValues);
    }
  }
}
