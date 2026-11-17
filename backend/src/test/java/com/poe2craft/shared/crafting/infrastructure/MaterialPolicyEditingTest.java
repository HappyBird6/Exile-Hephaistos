package com.poe2craft.shared.crafting.infrastructure;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.poe2craft.shared.crafting.domain.*;
import com.poe2craft.shared.item.*;
import com.poe2craft.shared.item.infrastructure.ItemCatalogLoader;
import java.io.ByteArrayInputStream;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * JSON 수정이 에센스 후보와 적용 정책에 반영되는지 검증한다. json은 독립 테스트 문서의 파서다. source는 현재 실행 원본을 복사하고 read는 엄격한 실제 로더를
 * 사용한다. changingJsonBaseTargetsChangesActualCraftingWithoutTheOldGlobalCandidateCountRestriction는
 * 후보를 두 개로 바꿔 실제 난수 제작에서 두 결과가 나오는지 검사한다. rarityPolicyComesFromJsonAndMalformedReferencesStillFail은
 * 희귀도 정책 변경을 읽으면서도 없는 옵션 참조를 거절하는지 검사한다. 입력 스트림 오류는 테스트 호출자에게 전달한다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code json}: 이 경계의 JSON 읽기·쓰기 변환기 ({@code ObjectMapper}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code source()}: 출처 값을 조회·구성한다. 반환: {@code ObjectNode}. 사용하는 예외: {@code Exception}.
 *   <li>{@code read(source)}: 지정한 입력 또는 저장된 값을 읽는다. 입력: {@code source}(출처). 반환: {@code
 *       WorkbenchDefinitions}. 사용하는 예외: {@code Exception}.
 *   <li>{@code
 *       changingJsonBaseTargetsChangesActualCraftingWithoutTheOldGlobalCandidateCountRestriction()}:
 *       이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과 비교한다. 사용하는 예외: {@code Exception}.
 *   <li>{@code rarityPolicyComesFromJsonAndMalformedReferencesStillFail()}: 시나리오의 잘못된 입력·상태가 {@code
 *       IllegalArgumentException}로 거절되는지 검증한다. 사용하는 예외: {@code Exception}.
 * </ul>
 */
class MaterialPolicyEditingTest {
  private final ObjectMapper json = new ObjectMapper();

  private ObjectNode source() throws Exception {
    try (var stream = getClass().getResourceAsStream("/crafting/workbench-definitions-v1.json")) {
      return (ObjectNode) json.readTree(stream);
    }
  }

  private WorkbenchDefinitions read(ObjectNode source) throws Exception {
    return WorkbenchDefinitionsLoader.read(
        new ByteArrayInputStream(json.writeValueAsBytes(source)));
  }

  @Test
  void changingJsonBaseTargetsChangesActualCraftingWithoutTheOldGlobalCandidateCountRestriction()
      throws Exception {
    WorkbenchDefinitionsLoader.initialize();
    var catalog = ItemCatalogLoader.loadStocky();
    var source = source();
    var ids =
        source
            .withObject(
                "/baseTargets/"
                    + catalog.base().id().replace("~", "~0").replace("/", "~1")
                    + "/fixed")
            .putArray("LESSER_ESSENCE_BODY");
    ids.add("stocky-mitts:prefix:sanguine").add("stocky-mitts:prefix:robust");
    var edited = read(source);
    edited.validateTargets(WorkbenchDefinitionsLoader.readTargets());
    var targets = edited.targets(catalog.base().id());
    var simulator =
        new WorkbenchSimulator(
            catalog,
            new CraftingEngine(catalog),
            Set.of(),
            targets.fixed(),
            targets.replacements());
    var item =
        new ItemState(
            catalog.metadata().snapshotId(),
            catalog.base().id(),
            82,
            ItemState.Rarity.MAGIC,
            List.of(),
            List.of(),
            Set.of());
    var observed = new HashSet<String>();
    var random = new Random(72);
    for (int attempt = 0; attempt < 32; attempt++) {
      var result = simulator.apply(item, WorkbenchCurrency.LESSER_ESSENCE_BODY, Set.of(), random);
      assertTrue(result.applied(), result.reason());
      assertEquals(ItemState.Rarity.RARE, result.state().rarity());
      assertEquals(1, result.state().explicits().size());
      observed.add(result.state().explicits().getFirst().modifierId());
    }
    assertEquals(Set.of("stocky-mitts:prefix:sanguine", "stocky-mitts:prefix:robust"), observed);
  }

  @Test
  void rarityPolicyComesFromJsonAndMalformedReferencesStillFail() throws Exception {
    var source = source();
    for (var node : source.path("currencies")) {
      if (node.path("id").asText().equals("LESSER_ESSENCE_BODY")) {
        ((ObjectNode) node.path("materialPolicy")).putArray("inputRarities").add("NORMAL");
        ((ObjectNode) node.path("materialPolicy")).put("resultRarity", "MAGIC");
      }
    }
    var edited = read(source);
    var policy = edited.currency(WorkbenchCurrency.LESSER_ESSENCE_BODY).materialPolicy();
    assertEquals(List.of(ItemState.Rarity.NORMAL), policy.inputRarities());
    assertEquals(ItemState.Rarity.MAGIC, policy.resultRarity());
    var catalog = ItemCatalogLoader.loadStocky();
    var normal =
        new ItemState(
            catalog.metadata().snapshotId(),
            catalog.base().id(),
            82,
            ItemState.Rarity.NORMAL,
            List.of(),
            List.of(),
            Set.of());
    assertTrue(policy.allows(normal));
    assertEquals(ItemState.Rarity.MAGIC, policy.result(normal));
    var inconsistent = source.deepCopy();
    for (var currency : inconsistent.path("currencies")) {
      if (currency.path("id").asText().equals("LESSER_ESSENCE_BODY"))
        ((ObjectNode) currency.path("materialPolicy")).put("mode", "NONE");
    }
    assertThrows(java.io.IOException.class, () -> read(inconsistent));
    var first = (ObjectNode) source.path("baseTargets").elements().next();
    first.withObject("/fixed").putArray("LESSER_ESSENCE_BODY").add("missing-source-option");
    var invalid = read(source);
    assertThrows(
        IllegalArgumentException.class,
        () -> invalid.validateTargets(WorkbenchDefinitionsLoader.readTargets()));
  }
}
