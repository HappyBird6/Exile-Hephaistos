package com.poe2craft.craftingsupport.search.infrastructure;

import com.poe2craft.shared.crafting.domain.AutomaticCurrencyPolicy;
import com.poe2craft.shared.json.BundledJson;
import com.poe2craft.shared.ruleset.SealedResources;
import java.io.IOException;

/**
 * 탐색 허용 화폐 정책을 번들 JSON에서 엄격하게 읽는다.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code AutomaticCurrencyPolicyLoader()}: 외부 인스턴스 생성이 필요 없는 유틸리티 생성자다.
 *   <li>{@code load()}: 검증된 자료를 로딩한다. 반환: {@code AutomaticCurrencyPolicy}. 사용하는 예외: {@code
 *       IllegalStateException}.
 * </ul>
 */
public final class AutomaticCurrencyPolicyLoader {
  private AutomaticCurrencyPolicyLoader() {}

  public static AutomaticCurrencyPolicy load() {
    try (var stream = SealedResources.open("/crafting/automatic-currency-policy-v1.json")) {
      return new AutomaticCurrencyPolicy(
          BundledJson.mapper().readValue(stream, AutomaticCurrencyPolicy.Document.class));
    } catch (IOException e) {
      throw new IllegalStateException("Cannot load approved automatic currency policy", e);
    }
  }
}
