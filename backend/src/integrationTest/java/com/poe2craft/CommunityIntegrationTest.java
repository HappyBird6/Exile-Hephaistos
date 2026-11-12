package com.poe2craft;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.poe2craft.account.application.AccountLibrary;
import com.poe2craft.community.application.SessionBoard;
import com.poe2craft.community.application.SessionBoard.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class CommunityIntegrationTest {
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
    p.add("backend.prices.initial-delay", () -> "24h");
  }

  @Autowired SessionBoard board;
  @Autowired AccountLibrary accounts;
  @Autowired ObjectMapper json;
  @Autowired MockMvc mvc;
  @Autowired com.poe2craft.community.application.CommunityReactions reactions;

  Actor member() {
    return new Actor(
        accounts.signIn(UUID.randomUUID().toString(), "Member").id(), UUID.randomUUID());
  }

  JsonNode film() throws Exception {
    return json.readTree(
        "{\"id\":\"source\",\"createdAt\":\"2026-11-12T00:00:00Z\",\"sourceDocument\":\"private clipboard\",\"frames\":[{\"action\":null,\"state\":{\"snapshotId\":\"test\",\"baseItemId\":\"Metadata/Items/Amulets/FourAmulet9\",\"rarity\":\"NORMAL\",\"itemLevel\":82,\"implicits\":[],\"explicits\":[],\"conditions\":[]}}]}");
  }

  Draft draft(JsonNode film, String body, String password, long revision) {
    return new Draft(UUID.randomUUID(), "source", "0.5.5", "", body, film, password, revision);
  }

  @Test
  void guestPermissionsPasswordAndPublicSnapshot() throws Exception {
    var actor = new Actor(null, UUID.randomUUID());
    var film = film();
    assertThatThrownBy(() -> board.create(actor, draft(film, "not allowed", "delete-pass", 0)))
        .isInstanceOf(ResponseStatusException.class);
    var p = board.create(actor, draft(film, "", "delete-pass", 0));
    assertThat(p.film().has("sourceDocument")).isFalse();
    assertThat(film.has("sourceDocument")).isTrue();
    assertThat(board.create(actor, draft(film, "", "delete-pass", 0)).id()).isEqualTo(p.id());
    assertThatThrownBy(() -> board.delete(actor, p.id(), "wrong", p.revision()))
        .isInstanceOf(ResponseStatusException.class);
    board.delete(new Actor(null, UUID.randomUUID()), p.id(), "delete-pass", p.revision());
    assertThatThrownBy(() -> board.get(p.id())).isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void ownerAppendOnlyUpdateVersionAndFilter() throws Exception {
    var actor = member();
    var p = board.create(actor, draft(film(), "Member description", null, 0));
    assertThatThrownBy(() -> board.update(member(), p.id(), draft(p.film(), "stolen", null, 1)))
        .isInstanceOf(ResponseStatusException.class);
    var appended = p.film().deepCopy();
    ((ArrayNode) appended.path("frames")).add(p.film().path("frames").get(0).deepCopy());
    var next = board.update(actor, p.id(), draft(appended, "updated", null, 1));
    assertThat(next.sharedStep()).isZero();
    assertThat(next.film().path("frames").size()).isEqualTo(2);
    assertThatThrownBy(() -> board.update(actor, p.id(), draft(appended, "stale", null, 1)))
        .isInstanceOf(ResponseStatusException.class);
    assertThatThrownBy(() -> board.update(actor, p.id(), draft(p.film(), "shrink", null, 2)))
        .isInstanceOf(ResponseStatusException.class);
    assertThat(board.list(p.baseId(), "0.5.5", actor.account(), 1).total()).isEqualTo(1);
    assertThat(board.list(p.baseId(), "0.0", actor.account(), 1).total()).isZero();
  }

  @Test
  void csrfIsRequiredForCommunityMutations() throws Exception {
    mvc.perform(post("/api/community/posts").contentType("application/json").content("{}"))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/community/context"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.csrf").isString());
  }

  @Test
  void unifiedDownloadsAcrossActionsBrowsersAndLoginAndRecommendations() throws Exception {
    var author = member();
    var p = board.create(author, draft(film(), "", null, 0));
    var guest = new Actor(null, UUID.randomUUID());
    assertThat(reactions.download(p.id(), guest).downloads()).isEqualTo(1);
    assertThat(reactions.download(p.id(), guest).downloads()).isEqualTo(1);
    var reader = member();
    var signed = new Actor(reader.account(), guest.browser());
    assertThat(reactions.download(p.id(), signed).downloads()).isEqualTo(1);
    var second = new Actor(reader.account(), UUID.randomUUID());
    assertThat(reactions.download(p.id(), second).downloads()).isEqualTo(1);
    assertThat(reactions.download(p.id(), new Actor(null, second.browser())).downloads())
        .isEqualTo(1);
    assertThatThrownBy(() -> reactions.recommend(p.id(), guest, true))
        .isInstanceOf(ResponseStatusException.class);
    assertThat(reactions.recommend(p.id(), signed, true).recommendations()).isEqualTo(1);
    assertThat(reactions.recommend(p.id(), signed, true).recommendations()).isEqualTo(1);
    assertThat(reactions.recommend(p.id(), signed, false).recommendations()).isZero();
    try (var pool = java.util.concurrent.Executors.newFixedThreadPool(4)) {
      var jobs = new java.util.ArrayList<java.util.concurrent.Callable<Long>>();
      for (int i = 0; i < 8; i++) jobs.add(() -> reactions.download(p.id(), second).downloads());
      for (var result : pool.invokeAll(jobs)) assertThat(result.get()).isEqualTo(1);
    }
    assertThat(
            board.list(null, null, author.account(), 1, "popular").posts().getFirst().downloads())
        .isEqualTo(1);
  }
}
