package com.poe2craft.bootstrap;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.shared.crafting.application.CatalogCraftingService;
import com.poe2craft.shared.crafting.domain.CraftingEngine;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchOmen;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ItemStateValidator;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.item.ModifierInstance;
import com.poe2craft.shared.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * 상위·베이스·작업대 동작을 고정 입력·기대 결과로 검증한다. 각 테스트가 공유하는 준비 값과 시나리오는 아래와 같다.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code service()}: 서비스 값을 조회·구성한다. 반환: {@code CatalogCraftingService}.
 *   <li>{@code normal(c)}: 일반 처리에서 {@code c.metadata}, {@code c.base}을(를) 사용한다. 입력: {@code c}(c).
 *       반환: {@code ItemState}.
 *   <li>{@code ordinaryCraftingDispatchQualityAndCrossBaseSafety(key)}: 이름에 명시한 시나리오의 실제 결과·보존 상태를
 *       기대값과 비교한다. 입력: {@code key}(키).
 *   <li>{@code exactPerfectPathsOmenAndClassRestrictions(key)}: 이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과
 *       비교한다. 입력: {@code key}(키).
 * </ul>
 */
class TopBaseWorkbenchTest {
  private CatalogCraftingService service() {
    var solar = ItemCatalogLoader.loadDefault();
    return new CatalogConfiguration()
        .workbenchService(solar, new WorkbenchSimulator(solar, new CraftingEngine(solar)));
  }

  private ItemState normal(ItemCatalog c) {
    return new ItemState(
        c.metadata().snapshotId(),
        c.base().id(),
        82,
        ItemState.Rarity.NORMAL,
        List.of(),
        List.of(),
        Set.of());
  }

  @ParameterizedTest
  @ValueSource(strings = {"soldier", "imperial"})
  void ordinaryCraftingDispatchQualityAndCrossBaseSafety(String key) {
    var c = ItemCatalogLoader.loadTopBase(key);
    assertThat(c.metadata().snapshotId().length()).isLessThanOrEqualTo(120);
    var service = service();
    var initial = service.initial(key, 82);
    assertThat(initial.state().baseItemId()).isEqualTo(c.base().id());
    assertThat(initial.qualityLimit().maximumQuality()).isEqualTo(20);
    assertThat(c.compatibleSnapshotIds()).isEmpty();
    var state = normal(c);
    for (var action :
        List.of(
            WorkbenchCurrency.TRANSMUTATION,
            WorkbenchCurrency.AUGMENTATION,
            WorkbenchCurrency.REGAL,
            WorkbenchCurrency.EXALTED,
            WorkbenchCurrency.ANNULMENT,
            WorkbenchCurrency.CHAOS)) {
      var result = service.apply(state, action, Set.of(), new Random(831));
      assertThat(result.applied()).as(action.name()).isTrue();
      assertThat(result.state().baseItemId()).isEqualTo(c.base().id());
      assertThat(new ItemStateValidator(c).validate(result.state())).isEmpty();
      state = result.state();
    }
    assertThat(service.qualityDisplay(state).qualityLimit().maximumQuality()).isEqualTo(20);
    var old = key.equals("soldier") ? ItemCatalogLoader.loadBody() : ItemCatalogLoader.loadHelmet();
    assertThat(new ItemStateValidator(old).validate(state)).isNotEmpty();
    assertThat(service.initial(key.equals("soldier") ? "body" : "helmet", 82).state().baseItemId())
        .isEqualTo(old.base().id());
    assertThat(
            service
                .apply(normal(old), WorkbenchCurrency.TRANSMUTATION, Set.of(), new Random(831))
                .applied())
        .isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {"soldier", "imperial"})
  void exactPerfectPathsOmenAndClassRestrictions(String key) {
    var c = ItemCatalogLoader.loadTopBase(key);
    var service = service();
    var alchemy = service.apply(normal(c), WorkbenchCurrency.ALCHEMY, Set.of(), new Random(19));
    assertThat(alchemy.applied()).isTrue();
    var selected =
        c.modifiers().values().stream()
            .filter(d -> d.weight() > 0)
            .collect(java.util.stream.Collectors.groupingBy(ModifierDefinition::affixType));
    var explicit =
        List.of(
                selected.get(ModifierDefinition.AffixType.PREFIX).getFirst(),
                selected.get(ModifierDefinition.AffixType.SUFFIX).getFirst())
            .stream()
            .map(
                d -> {
                  var values = new HashMap<String, Long>();
                  d.stats().forEach(s -> values.put(s.id(), s.min()));
                  return new ModifierInstance(d.id(), values);
                })
            .toList();
    var rare =
        new ItemState(
            c.metadata().snapshotId(),
            c.base().id(),
            82,
            ItemState.Rarity.RARE,
            List.of(),
            explicit,
            Set.of());
    var actions =
        key.equals("soldier")
            ? List.of(
                WorkbenchCurrency.PERFECT_ESSENCE_BODY,
                WorkbenchCurrency.PERFECT_ESSENCE_RUIN,
                WorkbenchCurrency.PERFECT_ESSENCE_SEEKING)
            : List.of(WorkbenchCurrency.PERFECT_ESSENCE_THAWING);
    for (var action : actions) {
      var result =
          service.apply(
              rare, action, Set.of(WorkbenchOmen.SINISTRAL_CRYSTALLISATION.id()), new Random(19));
      assertThat(result.applied()).as(action.name()).isTrue();
      assertThat(result.events().getLast().modifierId()).isIn(action.replacementModifiers());
      assertThat(result.consumedOmens())
          .containsExactly(WorkbenchOmen.SINISTRAL_CRYSTALLISATION.id());
      assertThat(new ItemStateValidator(c).validate(result.state())).isEmpty();
    }
    for (var action :
        List.of(
            WorkbenchCurrency.PERFECT_ESSENCE_ICE,
            WorkbenchCurrency.ARTIFICER,
            WorkbenchCurrency.RUNIC_ALLOY)) {
      var result = service.apply(rare, action, Set.of(), new Random(19));
      assertThat(result.applied()).as(action.name()).isFalse();
      assertThat(result.state()).isEqualTo(rare);
    }
    var fractured =
        service.apply(alchemy.state(), WorkbenchCurrency.FRACTURING, Set.of(), new Random(19));
    assertThat(fractured.applied()).isTrue();
    var locked =
        fractured.state().explicits().stream()
            .filter(ModifierInstance::fractured)
            .findFirst()
            .orElseThrow();
    var chaos = service.apply(fractured.state(), WorkbenchCurrency.CHAOS, Set.of(), new Random(19));
    assertThat(chaos.applied()).isTrue();
    assertThat(chaos.state().explicits()).contains(locked);
  }
}
