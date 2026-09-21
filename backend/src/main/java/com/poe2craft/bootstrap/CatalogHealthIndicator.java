package com.poe2craft.bootstrap;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("catalog")
public final class CatalogHealthIndicator implements HealthIndicator {
  @Override
  public Health health() {
    // TODO(domain): Published snapshot query and verified catalog are required before readiness.
    return Health.down().withDetail("reason", "NO_PUBLISHED_SNAPSHOT").build();
  }
}
