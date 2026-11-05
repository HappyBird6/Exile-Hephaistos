package com.poe2craft.bootstrap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.configuration.BackendSettings;
import com.poe2craft.workbench.history.application.SessionSnapshotRepository;
import com.poe2craft.workbench.history.application.SessionSnapshotService;
import com.poe2craft.workbench.history.infrastructure.FileSessionSnapshotRepository;
import java.nio.file.Path;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Workbench history storage is separate from automatic search jobs and their lifetime. */
@Configuration
public class SessionHistoryConfiguration {
  @Bean
  SessionSnapshotRepository sessionSnapshotRepository(BackendSettings settings) {
    return new FileSessionSnapshotRepository(
        Path.of(settings.storage().sessionSnapshotDirectory()), settings.sessionHistory());
  }

  @Bean
  SessionSnapshotService sessionSnapshotService(
      ObjectMapper json, SessionSnapshotRepository repository) {
    return new SessionSnapshotService(json, repository);
  }
}
