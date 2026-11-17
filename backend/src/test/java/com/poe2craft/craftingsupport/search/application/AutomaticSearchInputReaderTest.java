package com.poe2craft.craftingsupport.search.application;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter;
import com.poe2craft.shared.crafting.domain.BasicCurrencyFixture;
import com.poe2craft.shared.item.ItemState;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * 자동·탐색·입력·읽기변환기 동작을 고정 입력·기대 결과로 검증한다. 각 테스트가 공유하는 준비 값과 시나리오는 아래와 같다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code json}: 이 경계의 JSON 읽기·쓰기 변환기 ({@code ObjectMapper}).
 *   <li>{@code fixture}: 검증사례 ({@code BasicCurrencyFixture}).
 *   <li>{@code reader}: 읽기변환기 ({@code AutomaticSearchInputReader}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code input()}: 입력 값을 조회·구성한다. 반환: {@code AutomaticSearchService.Input}.
 *   <li>{@code canonicalInputRetainsStateAndOptionalGoalDefaults()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를
 *       기대값과 비교한다. 사용하는 예외: {@code Exception}.
 *   <li>{@code acceptsOmittedOptionalBooleanFlagsFromTheBrowser()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과
 *       비교한다. 사용하는 예외: {@code Exception}.
 *   <li>{@code explicitInvalidBooleanFlagsRemainRejected()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 *       사용하는 예외: {@code Exception}.
 *   <li>{@code unsupportedStateRemainsRejected()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과 비교한다. 사용하는 예외:
 *       {@code Exception}.
 * </ul>
 */
class AutomaticSearchInputReaderTest {
  final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
  final BasicCurrencyFixture fixture = new BasicCurrencyFixture();
  final AutomaticSearchInputReader reader = new AutomaticSearchInputReader(json);

  AutomaticSearchService.Input input() {
    var root = fixture.item(ItemState.Rarity.RARE, List.of());
    var goal =
        new GoalFilter(
            1,
            "test-goal",
            new GoalFilter.General(
                root.baseItemId(),
                new GoalFilter.Range(null, null),
                List.of(ItemState.Rarity.RARE)),
            List.of());
    return new AutomaticSearchService.Input(
        new MethodSearchProtocol.Create(
            2, UUID.randomUUID().toString(), fixture.state(root), goal, List.of()),
        false);
  }

  @Test
  void canonicalInputRetainsStateAndOptionalGoalDefaults() throws Exception {
    var original = input();
    var body = (ObjectNode) json.valueToTree(original);
    ((ObjectNode) body.path("request").path("goal").path("general")).remove("quality");
    assertThat(reader.read(json.writeValueAsString(body), AutomaticSearchService.Input.class))
        .isEqualTo(original);
  }

  @Test
  void acceptsOmittedOptionalBooleanFlagsFromTheBrowser() throws Exception {
    var original = input();
    var body = (ObjectNode) json.valueToTree(original);
    var item = (ObjectNode) body.path("request").path("start").path("item");
    item.remove("qualityInfused");
    item.path("implicits").forEach(m -> ((ObjectNode) m).remove(List.of("crafted", "fractured")));
    assertThat(reader.read(json.writeValueAsString(body), AutomaticSearchService.Input.class))
        .isEqualTo(original);
  }

  @Test
  void explicitInvalidBooleanFlagsRemainRejected() throws Exception {
    for (var invalid : List.of("null", "\"false\"", "0")) {
      var body = (ObjectNode) json.valueToTree(input());
      ((ObjectNode) body.path("request").path("start").path("item"))
          .set("qualityInfused", json.readTree(invalid));
      assertThatThrownBy(
              () -> reader.read(json.writeValueAsString(body), AutomaticSearchService.Input.class))
          .isInstanceOf(PathSearchProtocol.Rejected.class);
    }
  }

  @Test
  void unsupportedStateRemainsRejected() throws Exception {
    var body = (ObjectNode) json.valueToTree(input());
    ((ObjectNode) body.path("request").path("start").path("item")).put("equipmentQuality", 20);
    assertThatThrownBy(
            () -> reader.read(json.writeValueAsString(body), AutomaticSearchService.Input.class))
        .isInstanceOf(PathSearchProtocol.Rejected.class);
  }
}
