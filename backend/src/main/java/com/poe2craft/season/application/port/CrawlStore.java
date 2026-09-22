package com.poe2craft.season.application.port;

import com.poe2craft.season.api.CrawlModels.*;
import java.util.List;
import java.util.UUID;

public interface CrawlStore {
  Settings settings(boolean runnerEnabled);

  Settings update(Update input, boolean runnerEnabled);

  Run enqueue();

  List<Run> recent();

  Run get(UUID id);

  boolean start(UUID id);

  void finish(UUID id, List<Source> sources);

  void fail(UUID id, String code);

  void expire();
}
