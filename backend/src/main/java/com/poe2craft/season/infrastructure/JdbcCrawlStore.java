package com.poe2craft.season.infrastructure;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.season.api.CrawlModels.*;
import com.poe2craft.season.application.port.CrawlStore;
import com.poe2craft.season.application.service.CrawlException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
public class JdbcCrawlStore implements CrawlStore {
  private final JdbcTemplate jdbc;
  private final ObjectMapper json;
  private final TransactionTemplate tx;

  public JdbcCrawlStore(JdbcTemplate jdbc, ObjectMapper json, TransactionTemplate tx) {
    this.jdbc = jdbc;
    this.json = json;
    this.tx = tx;
  }

  public Settings settings(boolean enabled) {
    return tx.execute(
        s -> {
          long version =
              jdbc.queryForObject(
                  "select version from season.crawl_settings where id=1 for share", Long.class);
          return new Settings(version, targets(), enabled, new Limits(20));
        });
  }

  private List<Target> targets() {
    return jdbc.query(
        "select * from season.crawl_target order by position",
        (r, n) ->
            new Target(
                r.getObject("id", UUID.class),
                r.getString("name"),
                r.getString("url"),
                r.getBoolean("enabled")));
  }

  public Settings update(Update input, boolean enabled) {
    return tx.execute(
        s -> {
          if (jdbc.update(
                  "update season.crawl_settings set version=version+1 where id=1 and version=?",
                  input.version())
              != 1) throw new CrawlException(409, "STALE_SETTINGS");
          jdbc.update("delete from season.crawl_target");
          int position = 0;
          for (Target t : input.targets())
            jdbc.update(
                "insert into season.crawl_target values (?,?,?,?,?)",
                t.id(),
                t.name(),
                t.url(),
                t.enabled(),
                position++);
          return new Settings(
              input.version() + 1, List.copyOf(input.targets()), enabled, new Limits(20));
        });
  }

  public Run enqueue() {
    return tx.execute(
        s -> {
          jdbc.queryForObject(
              "select version from season.crawl_settings where id=1 for update", Long.class);
          expire();
          if (jdbc.queryForObject(
                  "select count(*) from season.crawl_run where status in ('QUEUED','RUNNING')",
                  Integer.class)
              > 0) throw new CrawlException(409, "RUN_ACTIVE");
          var selected = targets().stream().filter(Target::enabled).toList();
          if (selected.isEmpty()) throw new CrawlException(422, "NO_ENABLED_TARGETS");
          UUID id = UUID.randomUUID();
          jdbc.update(
              "insert into season.crawl_run(id,status,targets) values (?,'QUEUED',?::jsonb)",
              id,
              encode(selected));
          return get(id);
        });
  }

  public List<Run> recent() {
    expire();
    return jdbc.query(
        "select * from season.crawl_run order by created_at desc limit 50", this::read);
  }

  public Run get(UUID id) {
    var rows = jdbc.query("select * from season.crawl_run where id=?", this::read, id);
    if (rows.isEmpty()) throw new CrawlException(404, "RUN_NOT_FOUND");
    return rows.getFirst();
  }

  public boolean start(UUID id) {
    return jdbc.update(
            "update season.crawl_run set status='RUNNING',started_at=now() where id=? and status='QUEUED'",
            id)
        == 1;
  }

  public void finish(UUID id, List<Source> sources) {
    jdbc.update(
        "update season.crawl_run set status='RAW_CAPTURED',sources=?::jsonb,source_count=?,finished_at=now() where id=? and status='RUNNING'",
        encode(sources),
        sources.size(),
        id);
  }

  public void fail(UUID id, String code) {
    jdbc.update(
        "update season.crawl_run set status='FAILED',error_code=?,finished_at=now() where id=? and status in ('QUEUED','RUNNING')",
        code,
        id);
  }

  public void expire() {
    jdbc.update(
        "update season.crawl_run set status='FAILED',error_code='RUN_EXPIRED',finished_at=now() where status in ('QUEUED','RUNNING') and coalesce(started_at,created_at) < now()-interval '10 minutes'");
  }

  private Run read(ResultSet r, int row) throws SQLException {
    return new Run(
        r.getObject("id", UUID.class),
        r.getString("status"),
        instant(r, "created_at"),
        instant(r, "started_at"),
        instant(r, "finished_at"),
        decode(r.getString("targets"), new TypeReference<List<Target>>() {}),
        r.getString("error_code"),
        (Integer) r.getObject("source_count"),
        r.getString("sources") == null
            ? List.of()
            : decode(r.getString("sources"), new TypeReference<List<Source>>() {}));
  }

  private Instant instant(ResultSet r, String key) throws SQLException {
    var t = r.getTimestamp(key);
    return t == null ? null : t.toInstant();
  }

  private String encode(Object value) {
    try {
      return json.writeValueAsString(value);
    } catch (Exception e) {
      throw new IllegalStateException("Stored crawl payload is invalid");
    }
  }

  private <T> T decode(String value, TypeReference<T> type) {
    try {
      return json.readValue(value, type);
    } catch (Exception e) {
      throw new IllegalStateException("Stored crawl payload is invalid");
    }
  }
}
