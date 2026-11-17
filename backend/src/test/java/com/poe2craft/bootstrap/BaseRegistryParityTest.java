package com.poe2craft.bootstrap;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.item.BasicJewel;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.item.ModifierInstance;
import java.security.*;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * 베이스·등록목록·동일성 동작을 고정 입력·기대 결과로 검증한다. 각 테스트가 공유하는 준비 값과 시나리오는 아래와 같다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code M}: 공통 JSON 변환기 ({@code ObjectMapper}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code canonical(n)}: 정규형 처리에서 {@code n.isObject}, {@code M.createObjectNode}을(를) 사용한다. 입력:
 *       {@code n}(n). 반환: {@code JsonNode}.
 *   <li>{@code digest(value)}: 다이제스트 처리에서 {@code HexFormat.of}, {@code
 *       MessageDigest.getInstance}을(를) 사용한다. 입력: {@code value}(값). 반환: {@code String}. 사용하는 예외:
 *       {@code Exception}.
 *   <li>{@code allExistingInitialsActionsAndSeededTracesRemainExact()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를
 *       기대값과 비교한다. 사용하는 예외: {@code Exception}.
 * </ul>
 */
class BaseRegistryParityTest {
  static final ObjectMapper M = new ObjectMapper();

  static JsonNode canonical(JsonNode n) {
    if (n.isObject()) {
      var result = M.createObjectNode();
      var keys = new TreeSet<String>();
      n.fieldNames().forEachRemaining(keys::add);
      for (var k : keys) result.set(k, canonical(n.get(k)));
      return result;
    }
    if (n.isArray()) {
      var values = new ArrayList<JsonNode>();
      n.forEach(v -> values.add(canonical(v)));
      if (values.stream().allMatch(JsonNode::isTextual))
        values.sort(Comparator.comparing(JsonNode::asText));
      var result = M.createArrayNode();
      values.forEach(result::add);
      return result;
    }
    return n;
  }

  static String digest(Object value) throws Exception {
    return HexFormat.of()
        .formatHex(
            MessageDigest.getInstance("SHA-256")
                .digest(M.writeValueAsBytes(canonical(M.valueToTree(value)))));
  }

  @Test
  void allExistingInitialsActionsAndSeededTracesRemainExact() throws Exception {
    var cfg = new CatalogConfiguration();
    var catalog = cfg.itemCatalog(cfg.workbenchDefinitions());
    var engine = cfg.craftingEngine(catalog);
    var service = cfg.workbenchService(catalog, cfg.workbenchSimulator(catalog, engine));
    var result = new TreeMap<String, Object>();
    var expected =
        M.readTree(getClass().getResourceAsStream("/base-registry-runtime-baseline.json"));
    org.junit.jupiter.api.Assertions.assertEquals(125, expected.size());
    for (var it = expected.fieldNames(); it.hasNext(); ) {
      var key = it.next();
      var record = new TreeMap<String, Object>();
      for (int level : new int[] {1, 20, 82}) {
        var initial = service.initial(key, level);
        // The additive UI rules field is checked separately; retain the exact legacy payload hash.
        var initialJson = (ObjectNode) M.valueToTree(initial);
        initialJson.remove("baseRules");
        record.put("initial-" + level, digest(initialJson));
        var rules = initial.baseRules();
        org.junit.jupiter.api.Assertions.assertEquals(initial.state().baseItemId(), rules.id());
        org.junit.jupiter.api.Assertions.assertEquals(1, rules.magicPrefixes());
        org.junit.jupiter.api.Assertions.assertEquals(1, rules.magicSuffixes());
        int rareLimit = BasicJewel.supportedCrafting(rules.id()) ? 2 : 3;
        org.junit.jupiter.api.Assertions.assertEquals(rareLimit, rules.rarePrefixes());
        org.junit.jupiter.api.Assertions.assertEquals(rareLimit, rules.rareSuffixes());
        var s = initial.state();
        var state =
            new ItemState(
                s.snapshotId(),
                s.baseItemId(),
                s.itemLevel(),
                s.rarity(),
                s.implicits(),
                s.modifierIds().stream()
                    .map(
                        id ->
                            new ModifierInstance(
                                id,
                                initial.modifiers().get(id).stats().stream()
                                    .collect(
                                        java.util.stream.Collectors.toMap(
                                            ModifierDefinition.StatRange::id, stat -> stat.max()))))
                    .toList(),
                s.conditions(),
                initial.augmentSockets());
        record.put("actions-" + level, digest(service.actions(state, Set.of())));
        var trace = new ArrayList<Object>();
        var random = new Random(760105L);
        for (var action :
            List.of(
                WorkbenchCurrency.TRANSMUTATION,
                WorkbenchCurrency.AUGMENTATION,
                WorkbenchCurrency.REGAL,
                WorkbenchCurrency.EXALTED,
                WorkbenchCurrency.EXALTED,
                WorkbenchCurrency.DIVINE,
                WorkbenchCurrency.CHAOS,
                WorkbenchCurrency.ANNULMENT)) {
          var applied = service.apply(state, action, Set.of(), random);
          trace.add(applied);
          state = applied.state();
        }
        record.put("seeded-" + level, digest(trace));
      }
      result.put(key, record);
      org.junit.jupiter.api.Assertions.assertEquals(
          expected.get(key), M.valueToTree(record), key + ": pre-refactor behavior changed");
    }
    org.junit.jupiter.api.Assertions.assertEquals(expected, M.valueToTree(result));
  }
}
