package com.poe2craft.crafting;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.crafting.domain.AugmentSocketRules;
import com.poe2craft.crafting.domain.QualityLimitRules;
import com.poe2craft.item.*;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ReviewedOffhandsTest {
  @ParameterizedTest
  @CsvSource({
    "tawhoan-tower-shield,123,26,264,0,0,26,115,0,0,Raise_Shield",
    "golden-targe,134,26,145,132,0,25,63,63,0,Raise_Shield",
    "blacksteel-crest-shield,132,26,145,0,40,25,63,0,63,Raise_Shield",
    "desert-buckler,118,94,0,192,0,20,0,115,0,Parry",
    "tasalian-focus,140,81,0,0,91,0,0,0,115,NONE"
  })
  void sourceClassDefencesAndStateRestrictions(
      String key,
      int ordinary,
      int classId,
      int armour,
      int evasion,
      int energyShield,
      int block,
      int str,
      int dex,
      int intelligence,
      String skill)
      throws Exception {
    var catalog = ItemCatalogLoader.loadTopBase(key);
    assertThat(catalog.base().id()).isEqualTo(ReviewedOffhands.BASES.get(key));
    assertThat(catalog.base().id())
        .contains("/Armours/")
        .doesNotContain("/Weapons/")
        .endsWith("Endgame");
    assertThat(catalog.modifiers().values().stream().filter(d -> d.weight() > 0)).hasSize(ordinary);
    assertThat(catalog.base().hasImplicit()).isFalse();
    assertThat(catalog.metadata().snapshotId()).contains("-20261005-");
    assertThat(catalog.metadata().retrievedAt()).startsWith("2026-10-05");
    var manifest =
        new ObjectMapper()
            .readTree(getClass().getResourceAsStream("/catalog/top-bases.json"))
            .get(key);
    assertThat(manifest.get("classId").asInt()).isEqualTo(classId);
    assertThat(manifest.get("armour").asInt()).isEqualTo(armour);
    assertThat(manifest.get("evasion").asInt()).isEqualTo(evasion);
    assertThat(manifest.get("energyShield").asInt()).isEqualTo(energyShield);
    assertThat(manifest.get("blockChance").asInt()).isEqualTo(block);
    assertThat(manifest.get("strength").asInt()).isEqualTo(str);
    assertThat(manifest.get("dexterity").asInt()).isEqualTo(dex);
    assertThat(manifest.get("intelligence").asInt()).isEqualTo(intelligence);
    assertThat(manifest.get("ordinarySocketMaximum").asInt()).isEqualTo(1);
    assertThat(manifest.get("requiredLevel").asInt()).isEqualTo(80);
    if (skill.equals("NONE")) assertThat(manifest.get("grantedSkill").isNull()).isTrue();
    else assertThat(manifest.get("grantedSkill").asText()).isEqualTo(skill);
    for (var locale : List.of("en", "ko", "ja", "zh-CN", "zh-TW", "es")) {
      assertThat(manifest.get("requirements").get(locale).asText()).isNotBlank();
      assertThat(manifest.get("sourceProperties").get(locale).size()).isGreaterThan(0);
      assertThat(manifest.get("skillLines").get(locale).size())
          .isEqualTo(skill.equals("NONE") ? 0 : 1);
    }
    var state =
        new ItemState(
            catalog.metadata().snapshotId(),
            catalog.base().id(),
            82,
            ItemState.Rarity.NORMAL,
            List.of(),
            List.of(),
            Set.of());
    assertThat(new ItemStateValidator(catalog).validate(state)).isEmpty();
    assertThat(QualityLimitRules.describe(state, catalog).maximumQuality()).isEqualTo(20);
    assertThat(CatalystQuality.supportedBase(state.baseItemId())).isFalse();
    assertThat(AugmentSocketRules.refusal(state)).isNotEmpty();
    var suppliedSocket =
        new ItemState(
            state.snapshotId(),
            state.baseItemId(),
            82,
            state.rarity(),
            state.implicits(),
            List.of(),
            Set.of(),
            1);
    assertThat(AugmentSocketRules.supportedState(suppliedSocket)).isFalse();
    assertThat(catalog.modifiers().values())
        .noneMatch(d -> d.id().contains("alloy") || d.id().equals(QualityLimitRules.BREACH_ID));
    if (classId == 81)
      assertThat(catalog.modifiers().values())
          .noneMatch(d -> d.familyIds().contains("IncreasedShieldBlockPercentage"));
    else
      assertThat(catalog.modifiers().values())
          .anyMatch(d -> d.familyIds().contains("IncreasedShieldBlockPercentage"));
  }
}
