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
/**
 * 기본·화폐·검증사례 동작을 고정 입력·기대 결과로 검증한다. 각 테스트가 공유하는 준비 값과 시나리오는 아래와 같다.
 *
 * <p>기존 계약: Independently enumerable common crafting data used by multiple service tests.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code RULESET}: 규칙묶음; 초기값 {@code "synthetic-ruleset-one"} ({@code String}).
 *   <li>{@code implicit}: 암시옵션 ({@code ModifierDefinition}).
 *   <li>{@code a}: a ({@code ModifierDefinition}).
 *   <li>{@code b}: b ({@code ModifierDefinition}).
 *   <li>{@code catalog}: 이 인스턴스가 사용하는 검증된 카탈로그 ({@code ItemCatalog}).
 *   <li>{@code kernel}: 전이커널 ({@code BasicCurrencyTransitions}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code definition(id, type, min, max, weight)}: 정의 처리에서 {@code
 *       ModifierDefinition.StatRange}을(를) 사용한다. 입력: {@code id}(식별자), {@code type}(타입), {@code
 *       min}(최소값), {@code max}(최대값), {@code weight}(가중치). 반환: {@code ModifierDefinition}.
 *   <li>{@code catalog(definitions)}: 카탈로그 처리에서 {@code definitions.stream}, {@code d.affixType}을(를)
 *       사용한다. 입력: {@code definitions}(정의목록). 반환: {@code ItemCatalog}.
 *   <li>{@code item(rarity, mods)}: 아이템 처리에서 {@code catalog.metadata}, {@code catalog.base}을(를)
 *       사용한다. 입력: {@code rarity}(희귀도), {@code mods}(옵션목록). 반환: {@code ItemState}.
 *   <li>{@code mod(id, value)}: mod의 입력에 따른 분기·결과를 처리한다. 입력: {@code id}(식별자), {@code value}(값). 반환:
 *       {@code ModifierInstance}.
 *   <li>{@code state(item)}: 현재 상태 표현을 조회한다. 입력: {@code item}(아이템). 반환: {@code BasicCurrencyState}.
 *   <li>{@code expand(item, action)}: 다음 상태 또는 분포 구간을 확장한다. 입력: {@code item}(아이템), {@code
 *       action}(행동). 반환: {@code Result}.
 *   <li>{@code masses(result)}: masses 처리에서 {@code result.outcomes}, {@code resultMap.put}을(를)
 *       사용한다. 입력: {@code result}(결과). 반환: {@code Map<ItemState, Fraction>}.
 *   <li>{@code normalized(result)}: 입력 JSON 사본에 선택 필드 기본값을 채운다. 입력: {@code result}(결과).
 * </ul>
 */
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
