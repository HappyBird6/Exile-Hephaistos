package com.poe2craft.craftingsupport.search.infrastructure;

import com.poe2craft.shared.crafting.domain.AutomaticCurrencyPolicy;
import com.poe2craft.shared.json.BundledJson;
import com.poe2craft.shared.ruleset.SealedResources;
import java.io.IOException;

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
