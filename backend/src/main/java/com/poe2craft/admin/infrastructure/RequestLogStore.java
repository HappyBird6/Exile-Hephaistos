package com.poe2craft.admin.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.admin.application.RequestEvent;
import com.poe2craft.configuration.BackendSettings;
import java.io.*;
import java.nio.file.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.zip.GZIPOutputStream;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
public class RequestLogStore implements com.poe2craft.admin.application.RequestLogRepository {
  public static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
  private final JdbcTemplate jdbc;
  private final TransactionTemplate transaction;
  private final ObjectMapper json;
  private final Path directory;

  public RequestLogStore(
      JdbcTemplate jdbc,
      PlatformTransactionManager manager,
      ObjectMapper json,
      BackendSettings settings) {
    this.jdbc = jdbc;
    this.transaction = new TransactionTemplate(manager);
    this.json = json;
    directory = Path.of(settings.adminLogs().archiveDirectory()).toAbsolutePath().normalize();
  }

  public void insert(List<RequestEvent> events) {
    jdbc.batchUpdate(
        "INSERT INTO operations.request_log VALUES (?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT(id) DO NOTHING",
        events,
        events.size(),
        (s, e) -> {
          s.setObject(1, e.id());
          s.setTimestamp(2, Timestamp.from(e.occurredAt()));
          s.setString(3, e.method());
          s.setString(4, e.path());
          s.setString(5, e.route());
          s.setInt(6, e.status());
          s.setLong(7, e.durationMs());
          s.setString(8, e.clientIp());
          s.setString(9, e.visitorId());
          s.setInt(10, e.pageViews());
          s.setString(11, e.errorType());
          s.setString(12, e.userAgent());
        });
  }

  public Map<String, Object> query(Filters f) {
    var args = new ArrayList<Object>();
    String where = " WHERE occurred_at >= ? AND occurred_at < ?";
    args.add(Timestamp.from(f.date().atStartOfDay(SEOUL).toInstant()));
    args.add(Timestamp.from(f.date().plusDays(1).atStartOfDay(SEOUL).toInstant()));
    if (f.kind().equals("errors")) where += " AND status >= 500";
    if (f.kind().equals("limited")) where += " AND status = 429";
    if (!f.ip().isBlank()) {
      where += " AND client_ip = ?";
      args.add(f.ip());
    }
    if (!f.visitor().isBlank()) {
      where += " AND visitor_id = ?";
      args.add(f.visitor());
    }
    if (!f.route().isBlank()) {
      where += " AND route = ?";
      args.add(f.route());
    }
    if (f.status() != null) {
      where += " AND status = ?";
      args.add(f.status());
    }
    Object[] parameters = args.toArray();
    String table = " FROM operations.request_log";
    var summary =
        jdbc.queryForMap(
            "SELECT count(*) AS requests, count(*) FILTER(WHERE status >= 500) AS errors, count(*) FILTER(WHERE status=429) AS limited, count(DISTINCT visitor_id) FILTER(WHERE page_views>0) AS visitors, count(DISTINCT client_ip) AS ips, coalesce(sum(page_views),0) AS views, coalesce(round(avg(duration_ms)),0) AS average_ms, coalesce(percentile_cont(0.95) WITHIN GROUP(ORDER BY duration_ms),0) AS p95_ms, count(DISTINCT visitor_id) FILTER(WHERE occurred_at >= now()-interval '5 minutes') AS active_visitors"
                + table
                + where,
            parameters);
    var minutes =
        jdbc.queryForList(
            "SELECT to_char(occurred_at AT TIME ZONE 'Asia/Seoul','HH24:MI') AS minute, count(*) AS requests, count(*) FILTER(WHERE status >=500) AS errors, count(*) FILTER(WHERE status=429) AS limited, round(avg(duration_ms)) AS average_ms"
                + table
                + where
                + " GROUP BY 1 ORDER BY 1",
            parameters);
    var routes =
        jdbc.queryForList(
            "SELECT route AS value, count(*) AS requests, count(*) FILTER(WHERE status>=500) AS errors, round(avg(duration_ms)) AS average_ms"
                + table
                + where
                + " GROUP BY route ORDER BY requests DESC,route LIMIT 10",
            parameters);
    var ips =
        jdbc.queryForList(
            "SELECT client_ip AS value, count(*) AS requests, count(*) FILTER(WHERE status=429) AS limited"
                + table
                + where
                + " GROUP BY client_ip ORDER BY requests DESC,client_ip LIMIT 10",
            parameters);
    var visitors =
        jdbc.queryForList(
            "SELECT visitor_id AS value,count(*) AS requests"
                + table
                + where
                + " AND visitor_id IS NOT NULL GROUP BY visitor_id ORDER BY requests DESC,visitor_id LIMIT 10",
            parameters);
    args.add(40);
    args.add((f.page() - 1) * 40);
    var rows =
        jdbc.queryForList(
            "SELECT id, occurred_at, method,path,route,status,duration_ms,client_ip,visitor_id,page_views,error_type,user_agent"
                + table
                + where
                + " ORDER BY occurred_at DESC,id LIMIT ? OFFSET ?",
            args.toArray());
    rows.forEach(
        row ->
            row.computeIfPresent("occurred_at", (k, v) -> ((Timestamp) v).toInstant().toString()));
    return Map.of(
        "summary",
        summary,
        "minutes",
        minutes,
        "routes",
        routes,
        "ips",
        ips,
        "visitors",
        visitors,
        "rows",
        rows,
        "page",
        f.page(),
        "pageSize",
        40,
        "date",
        f.date().toString());
  }

  public boolean archiveBefore(Instant cutoff, int limit) {
    Boolean done =
        transaction.execute(
            status -> {
              if (!Boolean.TRUE.equals(
                  jdbc.queryForObject(
                      "SELECT pg_try_advisory_xact_lock(197311101)", Boolean.class))) return false;
              var oldest =
                  jdbc.queryForList(
                      "SELECT occurred_at FROM operations.request_log WHERE occurred_at < ? ORDER BY occurred_at LIMIT 1",
                      Timestamp.from(cutoff));
              if (oldest.isEmpty()) return false;
              var dayEnd =
                  ((Timestamp) oldest.getFirst().get("occurred_at"))
                      .toInstant()
                      .atZone(SEOUL)
                      .toLocalDate()
                      .plusDays(1)
                      .atStartOfDay(SEOUL)
                      .toInstant();
              var rows =
                  jdbc.queryForList(
                      "SELECT * FROM operations.request_log WHERE occurred_at < ? ORDER BY occurred_at,id LIMIT ? FOR UPDATE",
                      Timestamp.from(dayEnd.isBefore(cutoff) ? dayEnd : cutoff),
                      limit);
              if (rows.isEmpty()) return false;
              var first = ((Timestamp) rows.getFirst().get("occurred_at")).toInstant();
              var last = ((Timestamp) rows.getLast().get("occurred_at")).toInstant();
              String filename =
                  "requests-"
                      + first.atZone(SEOUL).toLocalDate()
                      + "-"
                      + rows.getFirst().get("id")
                      + ".jsonl.gz";
              Path target = directory.resolve(filename), temporary = null;
              try {
                Files.createDirectories(directory);
                temporary = Files.createTempFile(directory, "archive-", ".tmp");
                try (var stream = new GZIPOutputStream(Files.newOutputStream(temporary));
                    var writer =
                        new BufferedWriter(
                            new OutputStreamWriter(
                                stream, java.nio.charset.StandardCharsets.UTF_8))) {
                  for (var row : rows) {
                    row.computeIfPresent(
                        "occurred_at", (k, v) -> ((Timestamp) v).toInstant().toString());
                    writer.write(json.writeValueAsString(row));
                    writer.newLine();
                  }
                }
                try (var channel =
                    java.nio.channels.FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                  channel.force(true);
                }
                Files.move(
                    temporary,
                    target,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
                // Only delete DB rows after a complete compressed file and archive index are
                // available.
                jdbc.update(
                    "INSERT INTO operations.log_archive VALUES (?,?,?,?,?,?,?) ON CONFLICT(filename) DO UPDATE SET row_count=excluded.row_count,bytes=excluded.bytes",
                    UUID.randomUUID(),
                    filename,
                    Timestamp.from(Instant.now()),
                    Timestamp.from(first),
                    Timestamp.from(last),
                    rows.size(),
                    Files.size(target));
                jdbc.update(
                    "DELETE FROM operations.request_log WHERE id IN ("
                        + String.join(",", Collections.nCopies(rows.size(), "?"))
                        + ")",
                    rows.stream().map(row -> row.get("id")).toArray());
                return true;
              } catch (IOException failure) {
                throw new java.io.UncheckedIOException(failure);
              } finally {
                if (temporary != null)
                  try {
                    Files.deleteIfExists(temporary);
                  } catch (IOException ignored) {
                  }
              }
            });
    return Boolean.TRUE.equals(done);
  }

  public Map<String, Object> archives(int page) {
    var rows =
        jdbc.queryForList(
            "SELECT id,filename,created_at,first_event_at,last_event_at,row_count,bytes FROM operations.log_archive ORDER BY created_at DESC,id LIMIT 40 OFFSET ?",
            (page - 1) * 40);
    for (var row : rows)
      for (var key : List.of("created_at", "first_event_at", "last_event_at"))
        row.computeIfPresent(key, (k, v) -> ((Timestamp) v).toInstant().toString());
    var totals =
        jdbc.queryForMap(
            "SELECT count(*) AS files,coalesce(sum(bytes),0) AS bytes,coalesce(sum(row_count),0) AS rows FROM operations.log_archive");
    long free = -1;
    try {
      Files.createDirectories(directory);
      free = Files.getFileStore(directory).getUsableSpace();
    } catch (IOException ignored) {
    }
    return Map.of("rows", rows, "totals", totals, "page", page, "freeBytes", free);
  }

  public Path download(UUID id) {
    var rows =
        jdbc.queryForList(
            "SELECT filename FROM operations.log_archive WHERE id=?", String.class, id);
    if (rows.isEmpty()) return null;
    Path path = directory.resolve(rows.getFirst()).normalize();
    return path.getParent().equals(directory)
            && Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
        ? path
        : null;
  }
}
