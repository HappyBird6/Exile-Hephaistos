package com.poe2craft.season.application.service;

import com.poe2craft.season.api.CrawlModels.Target;
import java.util.HashSet;
import java.util.List;

public final class TargetValidation {
  private TargetValidation() {}

  public static void validate(List<Target> targets) {
    if (targets == null || targets.size() > 20) throw new CrawlException(422, "INVALID_TARGETS");
    var ids = new HashSet<>();
    var urls = new HashSet<>();
    for (Target target : targets) {
      if (target == null
          || target.id() == null
          || !ids.add(target.id())
          || target.name() == null
          || target.name().isBlank()
          || target.name().length() > 80
          || target.name().chars().anyMatch(Character::isISOControl)
          || target.url() == null
          || target.url().length() > 1024
          || !target.url().matches("https://poe2db\\.tw/(us|kr)/[A-Za-z0-9_-]+")
          || !urls.add(target.url())) throw new CrawlException(422, "INVALID_TARGETS");
    }
  }
}
