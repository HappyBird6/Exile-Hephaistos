package com.poe2craft.workbench.repeat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.bootstrap.CatalogConfiguration;
import com.poe2craft.shared.crafting.application.CatalogCraftingService;
import com.poe2craft.shared.crafting.domain.AbyssCraftingState;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.SolarAmulet;
import java.util.*;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * 반복·검증·작업대 동작을 고정 입력·기대 결과로 검증한다. 각 테스트가 공유하는 준비 값과 시나리오는 아래와 같다.
 *
 * <p>기존 계약: Real reviewed bootstrap without Spring Boot, web listeners, DB, Redis or Docker.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code CONTEXT}: 문맥 ({@code AnnotationConfigApplicationContext}).
 *   <li>{@code WORKBENCH}: 작업대 ({@code CatalogCraftingService}).
 *   <li>{@code JSON}: 공통 JSON 변환기 ({@code ObjectMapper}).
 *   <li>{@code DISPLAY}: 표시 ({@code ChaosDisplayResources}).
 *   <li>{@code ID}: 식별자; 초기값 {@code "123e4567-e89b-42d3-a456-426614174000"} ({@code String}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code RepeatTestWorkbench()}: 외부 인스턴스 생성이 필요 없는 유틸리티 생성자다.
 *   <li>{@code rare()}: 희귀 값을 조회·구성한다. 반환: {@code ItemState}.
 *   <li>{@code abyss(hidden)}: 훼손 처리에서 {@code WORKBENCH.abyssCatalog}, {@code
 *       AbyssCraftingState.from}을(를) 사용한다. 입력: {@code hidden}(미공개). 반환: {@code AbyssCraftingState}.
 *   <li>{@code input(state, action, max, omens, pattern, locale)}: 입력 처리에서 {@code
 *       ChaosRepeatProtocol.Input}을(를) 사용한다. 입력: {@code state}(상태), {@code action}(행동), {@code
 *       max}(최대값), {@code omens}(징조목록), {@code pattern}(패턴), {@code locale}(언어코드). 반환: {@code
 *       ChaosRepeatProtocol.Input}.
 * </ul>
 */
public final class RepeatTestWorkbench {
  private RepeatTestWorkbench() {}

  private static final AnnotationConfigApplicationContext CONTEXT =
      new AnnotationConfigApplicationContext(CatalogConfiguration.class);
  public static final CatalogCraftingService WORKBENCH =
      CONTEXT.getBean(CatalogCraftingService.class);
  public static final ObjectMapper JSON = new ObjectMapper();
  public static final ChaosDisplayResources DISPLAY = new ChaosDisplayResources(JSON);
  public static final String ID = "123e4567-e89b-42d3-a456-426614174000";

  public static ItemState rare() {
    var catalog =
        WORKBENCH.reviewedCatalogs().stream()
            .filter(c -> c.base().id().equals(SolarAmulet.BASE_ID))
            .findFirst()
            .orElseThrow();
    return WORKBENCH
        .apply(SolarAmulet.initial(catalog), WorkbenchCurrency.ALCHEMY, Set.of(), new Random(4))
        .state();
  }

  public static AbyssCraftingState abyss(boolean hidden) {
    var item = rare();
    var catalog = WORKBENCH.abyssCatalog(item);
    var state = AbyssCraftingState.from(item, catalog);
    if (!hidden) return state;
    var slots = new ArrayList<>(state.slots());
    var last = slots.getLast();
    slots.set(slots.size() - 1, new AbyssCraftingState.Slot(last.id(), last.side(), null, true));
    var result = new AbyssCraftingState(state.item(), slots, null);
    result.validate(catalog);
    return result;
  }

  public static ChaosRepeatProtocol.Input input(
      ChaosRepeatProtocol.State state,
      WorkbenchCurrency action,
      int max,
      Set<String> omens,
      String pattern,
      String locale) {
    return new ChaosRepeatProtocol.Input(
        ID, "0".repeat(64), state, action, omens, pattern, locale, max);
  }
}
