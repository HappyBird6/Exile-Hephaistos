package com.poe2craft.workbench.repeat;

import static com.poe2craft.workbench.repeat.ChaosRepeatProtocol.*;
import static com.poe2craft.workbench.repeat.RepeatTestWorkbench.*;
import static org.assertj.core.api.Assertions.*;

import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ItemStateValidator;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.item.ModifierInstance;
import com.poe2craft.workbench.api.ChaosRepeatController;
import com.poe2craft.workbench.display.DisplayLocale;
import com.poe2craft.workbench.display.DisplayNumbers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ChaosRepeatDisplayIntegrationTest {
  private static String literal(String text) {
    var result = new StringBuilder();
    text.codePoints()
        .forEach(
            cp -> {
              if (".^$|?*+()[]{}\\".indexOf(cp) >= 0) result.append('\\');
              result.appendCodePoint(cp);
            });
    return result.toString();
  }

  @ParameterizedTest
  @EnumSource(DisplayLocale.class)
  void matchesActualPostApplyLocalizedValuesAndKeepsNumericPunctuation(DisplayLocale locale) {
    var before = rare();
    var next = WORKBENCH.apply(before, WorkbenchCurrency.CHAOS, Set.of(), new Random(42)).state();
    var catalog = WORKBENCH.abyssCatalog(next);
    var text = DISPLAY.format(next, catalog, locale).groups().getFirst().text();
    var result =
        new ChaosRepeatService(WORKBENCH, DISPLAY, "test")
            .run(
                input(
                    new Concrete(before),
                    WorkbenchCurrency.CHAOS,
                    5,
                    Set.of(),
                    "^" + literal(text) + "$",
                    locale.code()),
                new Random(42));
    assertThat(result.actualUses()).isEqualTo(1);
    assertThat(result.termination()).isEqualTo("MATCH");
    assertThat(result.finalState().displayItem()).isEqualTo(next);
    String numeric = DisplayNumbers.number(12345.25, locale);
    assertThat(numeric).isEqualTo(locale == DisplayLocale.ES ? "12.345,3" : "12,345.3");
    assertThat(ChaosRepeatPattern.compile(literal(numeric)).find(numeric)).isTrue();
  }

  @Test
  void independentlyMatchesOptionsInsteadOfConcatenatingDistinctAffixes() {
    var before = rare();
    var next = WORKBENCH.apply(before, WorkbenchCurrency.CHAOS, Set.of(), new Random(42)).state();
    var groups = DISPLAY.format(next, WORKBENCH.abyssCatalog(next), DisplayLocale.EN).groups();
    String pattern =
        "^" + literal(groups.get(0).text()) + "\\n" + literal(groups.get(1).text()) + "$";
    assertThat(
            ChaosRepeatPattern.compile(pattern)
                .find(groups.get(0).text() + "\n" + groups.get(1).text()))
        .isTrue();
    var result =
        new ChaosRepeatService(WORKBENCH, DISPLAY, "test")
            .run(
                input(new Concrete(before), WorkbenchCurrency.CHAOS, 1, Set.of(), pattern, "en"),
                new Random(42));
    assertThat(result.termination()).isEqualTo("LIMIT");
  }

  @Test
  void realCompoundModifierRemainsOneMultilineGroupForAllSixLocales() {
    ItemCatalog catalog =
        WORKBENCH.reviewedCatalogs().stream()
            .filter(
                c ->
                    c.modifiers().values().stream()
                        .anyMatch(
                            d ->
                                d.layer() == ModifierDefinition.Layer.EXPLICIT
                                    && d.stats().size() > 1
                                    && d.requiredItemLevel() <= 82
                                    && d.text().contains("\n")))
            .findFirst()
            .orElseThrow();
    var definition =
        catalog.modifiers().values().stream()
            .filter(
                d ->
                    d.layer() == ModifierDefinition.Layer.EXPLICIT
                        && d.stats().size() > 1
                        && d.requiredItemLevel() <= 82
                        && d.text().contains("\n"))
            .findFirst()
            .orElseThrow();
    var values = new HashMap<String, Long>();
    definition.stats().forEach(s -> values.put(s.id(), s.min()));
    var implicits = new ArrayList<ModifierInstance>();
    if (catalog.base().hasImplicit()) {
      var d = catalog.find(catalog.base().implicitModifierId()).orElseThrow();
      var v = new HashMap<String, Long>();
      d.stats().forEach(s -> v.put(s.id(), s.min()));
      implicits.add(new ModifierInstance(d.id(), v));
    }
    var state =
        new ItemState(
            catalog.metadata().snapshotId(),
            catalog.base().id(),
            82,
            ItemState.Rarity.RARE,
            implicits,
            List.of(new ModifierInstance(definition.id(), values)),
            Set.of());
    assertThat(new ItemStateValidator(catalog).validate(state)).isEmpty();
    for (var locale : DisplayLocale.values()) {
      var groups = DISPLAY.format(state, catalog, locale).groups();
      assertThat(groups).hasSize(1);
      var group = groups.getFirst();
      assertThat(group.lines()).hasSizeGreaterThan(1);
      assertThat(ChaosRepeatPattern.compile("^" + literal(group.text()) + "$").find(group.text()))
          .isTrue();
      assertThat(
              ChaosRepeatPattern.compile(
                      literal(group.lines().getFirst())
                          + "[\\s\\S]*"
                          + literal(group.lines().getLast()))
                  .find(group.text()))
          .isTrue();
    }
  }

  @Test
  void runtimeTemplateIsTheByteIdenticalPinnedFrontendAssetAndCatalogMismatchIsRejected()
      throws Exception {
    byte[] original =
        Files.readAllBytes(Path.of("../frontend/src/shared/i18n/modifierTemplates.json"));
    byte[] bundled;
    try (var in =
        new org.springframework.core.io.ClassPathResource("chaosrepeat/modifierTemplates.json")
            .getInputStream()) {
      bundled = in.readAllBytes();
    }
    assertThat(bundled).isEqualTo(original);
    assertThat(ChaosRepeatController.sha256(bundled))
        .isEqualTo(ChaosDisplayResources.TEMPLATE_SHA256);
    var state = rare();
    var catalog = WORKBENCH.abyssCatalog(state);
    var target = state.explicits().getFirst().modifierId();
    var definitions =
        catalog.modifiers().values().stream()
            .map(
                d ->
                    !d.id().equals(target)
                        ? d
                        : new ModifierDefinition(
                            d.id(),
                            d.name(),
                            d.layer(),
                            d.affixType(),
                            d.familyIds(),
                            d.requiredItemLevel(),
                            d.weight(),
                            d.tier(),
                            d.text() + " changed",
                            d.stats(),
                            d.tags(),
                            d.sourceUrl()))
            .toList();
    var altered = new ItemCatalog(catalog.metadata(), catalog.base(), definitions);
    assertThatThrownBy(() -> DISPLAY.format(state, altered, DisplayLocale.EN))
        .isInstanceOf(RepeatRejected.class);
  }
}
