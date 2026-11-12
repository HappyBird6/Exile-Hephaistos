package com.poe2craft;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.account.application.AccountLibrary;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
class AccountLibraryIntegrationTest {
  @Container
  static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:17.6-alpine");

  @Container
  static final GenericContainer<?> REDIS =
      new GenericContainer<>("redis:7.4.5-alpine").withExposedPorts(6379);

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry p) {
    p.add("spring.flyway.locations", () -> "classpath:db/migration,classpath:db/testmigration");
    p.add("spring.datasource.url", PG::getJdbcUrl);
    p.add("spring.datasource.username", PG::getUsername);
    p.add("spring.datasource.password", PG::getPassword);
    p.add("spring.data.redis.host", REDIS::getHost);
    p.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
  }

  @Autowired AccountLibrary library;
  @Autowired ObjectMapper json;

  @Test
  void identitiesOwnershipVersionsAndTombstones() throws Exception {
    String subject = UUID.randomUUID().toString();
    var a = library.signIn(subject, "One");
    var b = library.signIn(UUID.randomUUID().toString(), "Two");
    assertThat(library.signIn(subject, "Renamed").id()).isEqualTo(a.id());
    var body = json.readTree("{\"id\":\"session-1\",\"frames\":[{\"state\":{}}]}");
    var saved = library.save(a.id(), "session", "session-1", 0, body);
    assertThat(saved.version()).isEqualTo(1);
    assertThat(library.list(b.id())).isEmpty();
    assertThatThrownBy(() -> library.save(b.id(), "session", "session-1", 1, body))
        .isInstanceOf(ResponseStatusException.class);
    assertThatThrownBy(() -> library.save(a.id(), "session", "session-1", 0, body))
        .isInstanceOf(ResponseStatusException.class);
    var removed = library.save(a.id(), "session", "session-1", 1, null);
    assertThat(removed.version()).isEqualTo(2);
    assertThat(library.list(a.id()).getFirst().content()).isNull();
    assertThatThrownBy(() -> library.save(a.id(), "session", "session-1", 1, body))
        .isInstanceOf(ResponseStatusException.class);
    assertThat(library.list(a.id()).getFirst().version()).isEqualTo(2);
  }

  @Test
  void rejectsInvalidDocumentsWithoutWriting() throws Exception {
    var a = library.signIn(UUID.randomUUID().toString(), "User");
    var invalid = json.readTree("{\"id\":\"wrong\",\"frames\":[]}");
    assertThatThrownBy(() -> library.save(a.id(), "session", "test", 0, invalid))
        .isInstanceOf(ResponseStatusException.class);
    assertThatThrownBy(() -> library.save(a.id(), "unknown", "test", 0, invalid))
        .isInstanceOf(ResponseStatusException.class);
    assertThat(library.list(a.id())).isEmpty();
  }

  @Test
  void bulkClearRejectsStaleUploadsAndKeepsFavorites() throws Exception {
    var owner = library.signIn(UUID.randomUUID().toString(), "Clear test").id();
    var body = json.readTree("{\"id\":\"a\",\"frames\":[{}]}");
    library.saveSession(owner, "a", 0, 0, body);
    library.save(owner, "base-favorite", "a", 0, body);
    assertThat(library.clearSessions(owner)).isEqualTo(1);
    assertThat(library.read(owner, "session", "a").content()).isNull();
    assertThat(library.read(owner, "base-favorite", "a").content()).isNotNull();
    assertThatThrownBy(() -> library.saveSession(owner, "new-offline-id", 0, 0, body))
        .isInstanceOf(ResponseStatusException.class);
  }
}
