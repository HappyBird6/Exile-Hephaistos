package com.poe2craft.season.application.service;

import com.poe2craft.season.api.CrawlModels.*;
import com.poe2craft.season.application.port.CrawlRunner;
import com.poe2craft.season.application.port.CrawlStore;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CrawlService {
  private final CrawlStore store;
  private final CrawlRunner runner;
  private final boolean enabled;
  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  public CrawlService(
      CrawlStore store, CrawlRunner runner, @Value("${app.crawl.enabled:false}") boolean enabled) {
    this.store = store;
    this.runner = runner;
    this.enabled = enabled;
  }

  public Settings settings() {
    return store.settings(enabled);
  }

  public Settings update(Update update) {
    if (update.version() < 0) throw new CrawlException(422, "INVALID_VERSION");
    TargetValidation.validate(update.targets());
    return store.update(update, enabled);
  }

  public List<Run> runs() {
    return store.recent();
  }

  public Run get(UUID id) {
    store.expire();
    return store.get(id);
  }

  public Run launch() {
    if (!enabled) throw new CrawlException(503, "RUNNER_DISABLED");
    Run run = store.enqueue();
    try {
      executor.submit(() -> execute(run));
    } catch (RuntimeException e) {
      store.fail(run.id(), "RUNNER_UNAVAILABLE");
      throw new CrawlException(503, "RUNNER_UNAVAILABLE");
    }
    return run;
  }

  private void execute(Run run) {
    try {
      if (!store.start(run.id())) return;
      store.finish(run.id(), runner.capture(run));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      store.fail(run.id(), "RUN_INTERRUPTED");
    } catch (Exception e) {
      store.fail(run.id(), "CAPTURE_FAILED");
    }
  }

  @PreDestroy
  public void close() {
    executor.shutdownNow();
  }
}
