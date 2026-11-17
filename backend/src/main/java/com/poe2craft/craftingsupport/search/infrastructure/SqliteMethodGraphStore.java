package com.poe2craft.craftingsupport.search.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.craftingsupport.search.application.MethodGraphStore;
import java.io.IOException;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/**
 * 작업별 SQLite 파일에 계산 그래프를 저장하고 페이지 캐시를 제한한다.
 *
 * <p>기존 계약: Job-owned local file. SQLite's page cache is bounded; graph rows are never mirrored in
 * a map.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code CURRENT_DEPTH_PAGE_SQL}: SQL ({@code String}).
 *   <li>{@code LATER_DEPTH_PAGE_SQL}: SQL ({@code String}).
 *   <li>{@code path}: 경로 ({@code Path}).
 *   <li>{@code connection}: DB연결 ({@code Connection}).
 *   <li>{@code cacheKiB}: 캐시·Ki·B ({@code int}).
 *   <li>{@code reads}: 읽기횟수 ({@code long}).
 *   <li>{@code writes}: 쓰기횟수 ({@code long}).
 *   <li>{@code closed}: 닫힘 ({@code boolean}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code SqliteMethodGraphStore(path, identity, cacheKiB)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
 *       path}(경로), {@code identity}(정체성), {@code cacheKiB}(캐시·Ki·B). 사용하는 예외: {@code
 *       IllegalArgumentException}, {@code IllegalStateException}.
 *   <li>{@code transaction(work)}: 트랜잭션 처리에서 {@code connection.setAutoCommit}, {@code work.run}을(를)
 *       사용한다. 입력: {@code work}(계산작업). 반환: {@code T}. 사용하는 예외: {@code SQLException}.
 *   <li>{@code access(work)}: access 처리에서 {@code work.run}을(를) 사용한다. 입력: {@code work}(계산작업). 반환:
 *       {@code T}. 사용하는 예외: {@code IllegalStateException}.
 *   <li>{@code execute(sql)}: execute 처리에서 {@code connection.createStatement}, {@code
 *       statement.execute}을(를) 사용한다. 입력: {@code sql}(SQL). 사용하는 예외: {@code SQLException}.
 *   <li>{@code update(sql, values)}: 갱신 처리에서 {@code connection.prepareStatement}, {@code
 *       statement.setObject}을(를) 사용한다. 입력: {@code sql}(SQL), {@code values}(값목록). 반환: {@code int}.
 *       사용하는 예외: {@code SQLException}.
 *   <li>{@code number(sql)}: 숫자 처리에서 {@code connection.createStatement}, {@code
 *       statement.executeQuery}을(를) 사용한다. 입력: {@code sql}(SQL). 반환: {@code long}. 사용하는 예외: {@code
 *       IllegalStateException}, {@code SQLException}.
 *   <li>{@code revision()}: 리비전 값을 조회·구성한다. 반환: {@code long}.
 *   <li>{@code commit(expectedRevision, batch)}: 완전히 기록된 상태를 공개 가능한 체크포인트로 확정한다. 입력: {@code
 *       expectedRevision}(기대값·리비전), {@code batch}(일괄실행). 반환: {@code long}. 사용하는 예외: {@code
 *       IllegalStateException}, {@code IllegalArgumentException}.
 *   <li>{@code source(id)}: 출처 처리에서 {@code connection.prepareStatement}, {@code
 *       statement.setLong}을(를) 사용한다. 입력: {@code id}(식별자). 반환: {@code Optional<Source>}.
 *   <li>{@code beginExecution(scope, header)}: 시작·실행 처리에서 {@code scope.isBlank}, {@code
 *       header.isBlank}을(를) 사용한다. 입력: {@code scope}(범위), {@code header}(헤더). 반환: {@code
 *       ExecutionCursor}. 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code executionById(id)}: 실행·기준·식별자 처리에서 {@code connection.prepareStatement}, {@code
 *       statement.setString}을(를) 사용한다. 입력: {@code id}(식별자). 반환: {@code ExecutionCursor}. 사용하는 예외:
 *       {@code IllegalArgumentException}, {@code SQLException}.
 *   <li>{@code appendExecution(id, expectedRows, rows)}: append·실행 처리에서 {@code values.isEmpty},
 *       {@code values.size}을(를) 사용한다. 입력: {@code id}(식별자), {@code expectedRows}(기대값·행목록), {@code
 *       rows}(행목록). 반환: {@code long}. 사용하는 예외: {@code IllegalArgumentException}, {@code
 *       IllegalStateException}.
 *   <li>{@code captureExecution(scope, header, producer)}: capture·실행 처리에서 {@code scope.isBlank},
 *       {@code header.isBlank}을(를) 사용한다. 입력: {@code scope}(범위), {@code header}(헤더), {@code
 *       producer}(producer). 반환: {@code ExecutionCursor}. 사용하는 예외: {@code
 *       IllegalArgumentException}, {@code IllegalStateException}.
 *   <li>{@code sealExecution(id, expectedRows)}: 봉인·실행의 입력에 따른 분기·결과를 처리한다. 입력: {@code id}(식별자),
 *       {@code expectedRows}(기대값·행목록). 반환: {@code ExecutionCursor}. 사용하는 예외: {@code
 *       IllegalStateException}.
 *   <li>{@code execution(scope)}: 실행 처리에서 {@code reference.isEmpty}, {@code Optional.empty}을(를)
 *       사용한다. 입력: {@code scope}(범위). 반환: {@code Optional<ExecutionCursor>}. 사용하는 예외: {@code
 *       IllegalStateException}.
 *   <li>{@code executionRows(id, offset, limit)}: 실행·행목록 처리에서 {@code cursor.sealed}, {@code
 *       cursor.rows}을(를) 사용한다. 입력: {@code id}(식별자), {@code offset}(시작위치), {@code limit}(한도). 반환:
 *       {@code ExecutionPage}. 사용하는 예외: {@code IllegalArgumentException}, {@code
 *       IllegalStateException}.
 *   <li>{@code discardExecution(id)}: discard·실행 처리에서 {@code connection.prepareStatement}, {@code
 *       statement.setString}을(를) 사용한다. 입력: {@code id}(식별자). 사용하는 예외: {@code IllegalStateException}.
 *   <li>{@code method(id)}: 제작방법 처리에서 {@code connection.prepareStatement}, {@code
 *       statement.setString}을(를) 사용한다. 입력: {@code id}(식별자). 반환: {@code Optional<Method>}.
 *   <li>{@code methods(expectedRevision, after, limit)}: 제작방법목록 처리에서 {@code after.depth}, {@code
 *       after.ordinal}을(를) 사용한다. 입력: {@code expectedRevision}(기대값·리비전), {@code after}(변경후), {@code
 *       limit}(한도). 반환: {@code Page}. 사용하는 예외: {@code IllegalArgumentException}, {@code
 *       IllegalStateException}.
 *   <li>{@code readMethodRows(rows, sql, parameters)}: 제작방법·행목록을(를) 읽는다. 입력: {@code rows}(행목록),
 *       {@code sql}(SQL), {@code parameters}(parameters). 사용하는 예외: {@code SQLException}.
 *   <li>{@code observation(method, cycles)}: 관측값 처리에서 {@code connection.prepareStatement}, {@code
 *       statement.setString}을(를) 사용한다. 입력: {@code method}(제작방법), {@code cycles}(주기횟수). 반환: {@code
 *       Optional<Observation>}.
 *   <li>{@code checkpoint(name)}: 현재 진행 상태의 복구 지점을 기록한다. 입력: {@code name}(이름). 반환: {@code
 *       Optional<Checkpoint>}.
 *   <li>{@code internOperator(digest, canonicalContent)}: 공유색인·연산자 처리에서 {@code digest.isBlank},
 *       {@code canonicalContent.isBlank}을(를) 사용한다. 입력: {@code digest}(다이제스트), {@code
 *       canonicalContent}(정규형·내용). 반환: {@code long}. 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code operator(id)}: 연산자 처리에서 {@code connection.prepareStatement}, {@code
 *       statement.setLong}을(를) 사용한다. 입력: {@code id}(식별자). 반환: {@code String}. 사용하는 예외: {@code
 *       IllegalArgumentException}.
 *   <li>{@code metrics()}: 측정값 값을 조회·구성한다. 반환: {@code Metrics}. 사용하는 예외: {@code
 *       IllegalStateException}.
 *   <li>{@code close()}: 보유한 자원과 작업 상태를 종료·해제한다.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code SqlWork}: {@code SqliteMethodGraphStore} 내부의 SQL·계산작업 처리를 맡는다.
 * </ul>
 */
public final class SqliteMethodGraphStore implements MethodGraphStore {
  static final String CURRENT_DEPTH_PAGE_SQL =
      "SELECT ordinal,depth,id,source,program FROM methods WHERE depth=? AND ordinal>? ORDER BY ordinal LIMIT ?";
  private static final String LATER_DEPTH_PAGE_SQL =
      "SELECT ordinal,depth,id,source,program FROM methods WHERE depth>? ORDER BY depth,ordinal LIMIT ?";
  private final Path path;
  private final Connection connection;
  private final int cacheKiB;
  private long reads, writes;
  private boolean closed;

  public SqliteMethodGraphStore(Path path, Identity identity, int cacheKiB) {
    if (cacheKiB < 64 || cacheKiB > 65536)
      throw new IllegalArgumentException("Invalid cache budget");
    this.path = path.toAbsolutePath().normalize();
    this.cacheKiB = cacheKiB;
    Connection opened = null;
    try {
      Files.createDirectories(this.path.getParent());
      opened = DriverManager.getConnection("jdbc:sqlite:" + this.path);
      connection = opened;
      execute("PRAGMA foreign_keys=ON");
      execute("PRAGMA cache_size=-" + cacheKiB);
      execute("PRAGMA mmap_size=0");
      execute("PRAGMA temp_store=FILE");
      execute("PRAGMA busy_timeout=5000");
      execute("PRAGMA journal_mode=WAL");
      execute("PRAGMA synchronous=FULL");
      execute("PRAGMA wal_autocheckpoint=256");
      String context = new ObjectMapper().writeValueAsString(identity);
      transaction(
          () -> {
            execute(
                "CREATE TABLE IF NOT EXISTS graph_identity(id INTEGER PRIMARY KEY CHECK(id=1), version TEXT NOT NULL, context TEXT NOT NULL, revision INTEGER NOT NULL)");
            try (var statement =
                    connection.prepareStatement(
                        "SELECT version,context FROM graph_identity WHERE id=1");
                var result = statement.executeQuery()) {
              if (result.next()) {
                if (!VERSION.equals(result.getString(1)) || !context.equals(result.getString(2)))
                  throw new IllegalStateException("INCOMPATIBLE_GRAPH_STORE");
              } else update("INSERT INTO graph_identity VALUES(1,?,?,0)", VERSION, context);
            }
            execute(
                "CREATE TABLE IF NOT EXISTS sources(id INTEGER PRIMARY KEY, depth INTEGER NOT NULL, discovered INTEGER NOT NULL, content TEXT NOT NULL)");
            execute("CREATE INDEX IF NOT EXISTS source_depth ON sources(depth,id)");
            execute(
                "CREATE TABLE IF NOT EXISTS methods(ordinal INTEGER PRIMARY KEY AUTOINCREMENT, id TEXT NOT NULL UNIQUE, source INTEGER NOT NULL REFERENCES sources(id), program TEXT NOT NULL, depth INTEGER NOT NULL)");
            execute("CREATE INDEX IF NOT EXISTS method_source ON methods(source,ordinal)");
            execute("CREATE INDEX IF NOT EXISTS method_depth ON methods(depth,ordinal)");
            execute(
                "CREATE TABLE IF NOT EXISTS operators(id INTEGER PRIMARY KEY, digest TEXT NOT NULL, content TEXT NOT NULL)");
            execute("CREATE INDEX IF NOT EXISTS operator_digest ON operators(digest)");
            execute(
                "CREATE TABLE IF NOT EXISTS observations(method TEXT NOT NULL REFERENCES methods(id), cycles INTEGER NOT NULL CHECK(cycles IN(100,300,500)), status TEXT NOT NULL, operator INTEGER REFERENCES operators(id), summary TEXT, PRIMARY KEY(method,cycles))");
            execute(
                "CREATE TABLE IF NOT EXISTS checkpoints(name TEXT PRIMARY KEY, content TEXT NOT NULL)");
            execute("CREATE INDEX IF NOT EXISTS checkpoint_content ON checkpoints(content,name)");
            execute(
                "CREATE TABLE IF NOT EXISTS execution_cursors(id TEXT PRIMARY KEY, scope TEXT NOT NULL, header TEXT NOT NULL, row_count INTEGER NOT NULL, sealed INTEGER NOT NULL)");
            execute("CREATE INDEX IF NOT EXISTS execution_scope ON execution_cursors(scope,id)");
            execute(
                "CREATE TABLE IF NOT EXISTS execution_rows(cursor TEXT NOT NULL REFERENCES execution_cursors(id) ON DELETE CASCADE, ordinal INTEGER NOT NULL, content TEXT NOT NULL, PRIMARY KEY(cursor,ordinal))");
            return null;
          });
    } catch (SQLException | IOException | RuntimeException | Error error) {
      if (opened != null)
        try {
          opened.close();
        } catch (SQLException cleanup) {
          error.addSuppressed(cleanup);
        }
      if (error instanceof Error fatal) throw fatal;
      throw new IllegalStateException("Cannot open local method graph", error);
    }
  }

  /**
   * {@code SqliteMethodGraphStore} 내부의 SQL·계산작업 처리를 맡는다.
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code run()}: 작업의 실행 루프를 수행한다. 반환: {@code T}. 사용하는 예외: {@code SQLException}.
   * </ul>
   */
  private interface SqlWork<T> {
    T run() throws SQLException;
  }

  private <T> T transaction(SqlWork<T> work) throws SQLException {
    connection.setAutoCommit(false);
    try {
      T result = work.run();
      connection.commit();
      return result;
    } catch (SQLException | RuntimeException | Error error) {
      try {
        connection.rollback();
      } catch (SQLException rollback) {
        error.addSuppressed(rollback);
        closed = true;
        try {
          connection.close();
        } catch (SQLException cleanup) {
          error.addSuppressed(cleanup);
        }
      }
      throw error;
    } finally {
      if (!closed) connection.setAutoCommit(true);
    }
  }

  private <T> T access(SqlWork<T> work) {
    if (closed) throw new IllegalStateException("Graph store closed");
    try {
      return work.run();
    } catch (SQLException error) {
      throw new IllegalStateException("Local method graph operation failed", error);
    }
  }

  private void execute(String sql) throws SQLException {
    try (var statement = connection.createStatement()) {
      statement.execute(sql);
    }
  }

  private int update(String sql, Object... values) throws SQLException {
    try (var statement = connection.prepareStatement(sql)) {
      for (int i = 0; i < values.length; i++) statement.setObject(i + 1, values[i]);
      int result = statement.executeUpdate();
      writes++;
      return result;
    }
  }

  private long number(String sql) throws SQLException {
    try (var statement = connection.createStatement();
        var result = statement.executeQuery(sql)) {
      reads++;
      if (!result.next()) throw new IllegalStateException("Missing graph counter");
      return result.getLong(1);
    }
  }

  @Override
  public synchronized long revision() {
    return access(() -> number("SELECT revision FROM graph_identity WHERE id=1"));
  }

  @Override
  public synchronized long commit(long expectedRevision, Batch batch) {
    Objects.requireNonNull(batch);
    return access(
        () ->
            transaction(
                () -> {
                  if (revision() != expectedRevision)
                    throw new IllegalStateException("STALE_GRAPH_REVISION");
                  for (var source : batch.sources()) {
                    var old = source(source.id());
                    if (old.isPresent() && !old.get().content().equals(source.content()))
                      throw new IllegalStateException("SOURCE_IDENTITY_CONFLICT");
                    update(
                        "INSERT INTO sources VALUES(?,?,?,?) ON CONFLICT(id) DO UPDATE SET depth=MIN(depth,excluded.depth),discovered=MAX(discovered,excluded.discovered)",
                        source.id(),
                        source.depth(),
                        source.discovered() ? 1 : 0,
                        source.content());
                    if (old.isPresent() && source.depth() < old.get().depth())
                      update(
                          "UPDATE methods SET depth=? WHERE source=?", source.depth(), source.id());
                  }
                  for (var method : batch.methods()) {
                    try (var statement =
                        connection.prepareStatement(
                            "SELECT source,program FROM methods WHERE id=?")) {
                      statement.setString(1, method.id());
                      try (var old = statement.executeQuery()) {
                        reads++;
                        if (old.next()) {
                          if (old.getLong(1) != method.source()
                              || !old.getString(2).equals(method.program()))
                            throw new IllegalStateException("METHOD_IDENTITY_CONFLICT");
                          continue;
                        }
                      }
                    }
                    if (update(
                            "INSERT INTO methods(id,source,program,depth) SELECT ?,?,?,depth FROM sources WHERE id=?",
                            method.id(),
                            method.source(),
                            method.program(),
                            method.source())
                        != 1) throw new IllegalStateException("MISSING_METHOD_SOURCE");
                  }
                  for (var observation : batch.observations()) {
                    var old = observation(observation.method(), observation.cycles());
                    if (old.isPresent()) {
                      if (observation.status() == Status.REQUESTED) continue;
                      if (old.get().status() == Status.READY
                          || old.get().status() == Status.UNSUPPORTED) {
                        if (!old.get().equals(observation))
                          throw new IllegalStateException("TERMINAL_OBSERVATION_CONFLICT");
                        continue;
                      }
                    }
                    update(
                        "INSERT INTO observations VALUES(?,?,?,?,?) ON CONFLICT(method,cycles) DO UPDATE SET status=excluded.status,operator=excluded.operator,summary=excluded.summary",
                        observation.method(),
                        observation.cycles(),
                        observation.status().name(),
                        observation.operator(),
                        observation.summary());
                  }
                  for (var checkpoint : batch.checkpoints()) {
                    if (checkpoint.name().startsWith(CHECKPOINT_PREFIX)) {
                      if (checkpoint.content().isEmpty()) {
                        update("DELETE FROM checkpoints WHERE name=?", checkpoint.name());
                        continue;
                      }
                      var cursor = executionById(checkpoint.content());
                      if (!cursor.sealed()
                          || !checkpoint.name().equals(CHECKPOINT_PREFIX + cursor.scope()))
                        throw new IllegalArgumentException(
                            "Invalid execution checkpoint reference");
                    }
                    update(
                        "INSERT INTO checkpoints VALUES(?,?) ON CONFLICT(name) DO UPDATE SET content=excluded.content",
                        checkpoint.name(),
                        checkpoint.content());
                  }
                  update("UPDATE graph_identity SET revision=revision+1 WHERE id=1");
                  return revision();
                }));
  }

  @Override
  public synchronized Optional<Source> source(long id) {
    return access(
        () -> {
          try (var statement =
              connection.prepareStatement(
                  "SELECT depth,discovered,content FROM sources WHERE id=?")) {
            statement.setLong(1, id);
            try (var result = statement.executeQuery()) {
              reads++;
              return result.next()
                  ? Optional.of(
                      new Source(id, result.getInt(1), result.getBoolean(2), result.getString(3)))
                  : Optional.empty();
            }
          }
        });
  }

  @Override
  public synchronized ExecutionCursor beginExecution(String scope, String header) {
    if (scope == null || scope.isBlank() || header == null || header.isBlank())
      throw new IllegalArgumentException("Execution scope and header required");
    return access(
        () ->
            transaction(
                () -> {
                  var cursor =
                      new ExecutionCursor(UUID.randomUUID().toString(), scope, header, 0, false);
                  update(
                      "INSERT INTO execution_cursors VALUES(?,?,?,0,0)",
                      cursor.id(),
                      scope,
                      header);
                  return cursor;
                }));
  }

  private ExecutionCursor executionById(String id) throws SQLException {
    try (var statement =
        connection.prepareStatement(
            "SELECT scope,header,row_count,sealed FROM execution_cursors WHERE id=?")) {
      statement.setString(1, id);
      try (var result = statement.executeQuery()) {
        reads++;
        if (!result.next()) throw new IllegalArgumentException("Unknown execution cursor");
        return new ExecutionCursor(
            id, result.getString(1), result.getString(2), result.getLong(3), result.getBoolean(4));
      }
    }
  }

  @Override
  public synchronized long appendExecution(String id, long expectedRows, List<String> rows) {
    var values = List.copyOf(rows);
    if (expectedRows < 0
        || values.isEmpty()
        || values.size() > 256
        || values.stream().anyMatch(String::isBlank))
      throw new IllegalArgumentException("Execution append requires 1..256 rows");
    long next = Math.addExact(expectedRows, values.size());
    return access(
        () ->
            transaction(
                () -> {
                  if (update(
                          "UPDATE execution_cursors SET row_count=? WHERE id=? AND row_count=? AND sealed=0",
                          next,
                          id,
                          expectedRows)
                      != 1) throw new IllegalStateException("STALE_EXECUTION_CURSOR");
                  for (int i = 0; i < values.size(); i++)
                    update(
                        "INSERT INTO execution_rows VALUES(?,?,?)",
                        id,
                        expectedRows + i,
                        values.get(i));
                  return next;
                }));
  }

  @Override
  public synchronized ExecutionCursor captureExecution(
      String scope,
      String header,
      java.util.function.Consumer<java.util.function.Consumer<String>> producer) {
    if (scope == null || scope.isBlank() || header == null || header.isBlank())
      throw new IllegalArgumentException("Execution scope and header required");
    Objects.requireNonNull(producer);
    return access(
        () ->
            transaction(
                () -> {
                  String id = UUID.randomUUID().toString();
                  update("INSERT INTO execution_cursors VALUES(?,?,?,0,0)", id, scope, header);
                  long[] count = {0};
                  try (var insert =
                      connection.prepareStatement("INSERT INTO execution_rows VALUES(?,?,?)")) {
                    producer.accept(
                        row -> {
                          if (row == null || row.isBlank())
                            throw new IllegalArgumentException("Nonblank cursor row required");
                          try {
                            insert.setString(1, id);
                            insert.setLong(2, count[0]);
                            insert.setString(3, row);
                            insert.executeUpdate();
                            writes++;
                            count[0] = Math.incrementExact(count[0]);
                          } catch (SQLException failure) {
                            throw new IllegalStateException(
                                "Cannot capture execution row", failure);
                          }
                        });
                  }
                  update(
                      "UPDATE execution_cursors SET row_count=?,sealed=1 WHERE id=?", count[0], id);
                  return new ExecutionCursor(id, scope, header, count[0], true);
                }));
  }

  @Override
  public synchronized ExecutionCursor sealExecution(String id, long expectedRows) {
    return access(
        () ->
            transaction(
                () -> {
                  if (update(
                          "UPDATE execution_cursors SET sealed=1 WHERE id=? AND row_count=? AND sealed=0",
                          id,
                          expectedRows)
                      != 1) throw new IllegalStateException("STALE_EXECUTION_CURSOR");
                  return executionById(id);
                }));
  }

  @Override
  public synchronized Optional<ExecutionCursor> execution(String scope) {
    return access(
        () ->
            transaction(
                () -> {
                  var reference = checkpoint(CHECKPOINT_PREFIX + scope);
                  if (reference.isEmpty()) return Optional.empty();
                  var cursor = executionById(reference.get().content());
                  if (!cursor.sealed() || !cursor.scope().equals(scope))
                    throw new IllegalStateException("Invalid committed execution");
                  return Optional.of(cursor);
                }));
  }

  @Override
  public synchronized ExecutionPage executionRows(String id, long offset, int limit) {
    if (offset < 0 || limit < 1 || limit > 256)
      throw new IllegalArgumentException("Invalid execution page");
    return access(
        () ->
            transaction(
                () -> {
                  var cursor = executionById(id);
                  if (!cursor.sealed())
                    throw new IllegalStateException("Execution cursor is not sealed");
                  if (offset > cursor.rows())
                    throw new IllegalArgumentException("Execution offset outside cursor");
                  var rows = new ArrayList<ExecutionRow>();
                  try (var statement =
                      connection.prepareStatement(
                          "SELECT ordinal,content FROM execution_rows WHERE cursor=? AND ordinal>=? ORDER BY ordinal LIMIT ?")) {
                    statement.setString(1, id);
                    statement.setLong(2, offset);
                    statement.setInt(3, limit);
                    try (var result = statement.executeQuery()) {
                      reads++;
                      while (result.next()) {
                        if (result.getLong(1) != offset + rows.size())
                          throw new IllegalStateException("Noncontiguous execution rows");
                        rows.add(new ExecutionRow(result.getLong(1), result.getString(2)));
                      }
                    }
                  }
                  if (rows.size() != Math.min(limit, cursor.rows() - offset))
                    throw new IllegalStateException("Truncated execution cursor");
                  return new ExecutionPage(rows, offset + rows.size() == cursor.rows());
                }));
  }

  @Override
  public synchronized void discardExecution(String id) {
    access(
        () ->
            transaction(
                () -> {
                  try (var statement =
                      connection.prepareStatement(
                          "SELECT 1 FROM checkpoints WHERE name LIKE 'execution/%' AND content=? LIMIT 1")) {
                    statement.setString(1, id);
                    try (var result = statement.executeQuery()) {
                      reads++;
                      if (result.next())
                        throw new IllegalStateException("EXECUTION_CURSOR_REFERENCED");
                    }
                  }
                  update("DELETE FROM execution_cursors WHERE id=?", id);
                  return null;
                }));
  }

  @Override
  public synchronized Optional<Method> method(String id) {
    return access(
        () -> {
          try (var statement =
              connection.prepareStatement("SELECT source,program FROM methods WHERE id=?")) {
            statement.setString(1, id);
            try (var result = statement.executeQuery()) {
              reads++;
              return result.next()
                  ? Optional.of(new Method(id, result.getLong(1), result.getString(2)))
                  : Optional.empty();
            }
          }
        });
  }

  @Override
  public synchronized Page methods(long expectedRevision, Cursor after, int limit) {
    if (limit < 1 || limit > 256) throw new IllegalArgumentException("Page size must be 1..256");
    return access(
        () ->
            transaction(
                () -> {
                  if (revision() != expectedRevision)
                    throw new IllegalStateException("STALE_GRAPH_REVISION");
                  var rows = new ArrayList<MethodRow>();
                  // A row-value range can seek only depth on SQLite's INTEGER PRIMARY KEY
                  // alias, repeatedly scanning earlier ordinals. Split the ranges explicitly.
                  if (after != null)
                    readMethodRows(
                        rows, CURRENT_DEPTH_PAGE_SQL, after.depth(), after.ordinal(), limit + 1);
                  if (rows.size() < limit + 1)
                    readMethodRows(
                        rows,
                        LATER_DEPTH_PAGE_SQL,
                        after == null ? -1 : after.depth(),
                        limit + 1 - rows.size());
                  boolean more = rows.size() > limit;
                  if (more) rows.removeLast();
                  var last = rows.isEmpty() ? null : rows.getLast();
                  return new Page(
                      expectedRevision,
                      List.copyOf(rows),
                      more ? new Cursor(last.depth(), last.ordinal()) : null);
                }));
  }

  private void readMethodRows(List<MethodRow> rows, String sql, Object... parameters)
      throws SQLException {
    try (var statement = connection.prepareStatement(sql)) {
      for (int i = 0; i < parameters.length; i++) statement.setObject(i + 1, parameters[i]);
      try (var result = statement.executeQuery()) {
        reads++;
        while (result.next())
          rows.add(
              new MethodRow(
                  result.getLong(1),
                  result.getInt(2),
                  new Method(result.getString(3), result.getLong(4), result.getString(5))));
      }
    }
  }

  @Override
  public synchronized Optional<Observation> observation(String method, int cycles) {
    return access(
        () -> {
          try (var statement =
              connection.prepareStatement(
                  "SELECT status,operator,summary FROM observations WHERE method=? AND cycles=?")) {
            statement.setString(1, method);
            statement.setInt(2, cycles);
            try (var result = statement.executeQuery()) {
              reads++;
              if (!result.next()) return Optional.empty();
              long operator = result.getLong(2);
              Long reference = result.wasNull() ? null : operator;
              return Optional.of(
                  new Observation(
                      method,
                      cycles,
                      Status.valueOf(result.getString(1)),
                      reference,
                      result.getString(3)));
            }
          }
        });
  }

  @Override
  public synchronized Optional<Checkpoint> checkpoint(String name) {
    return access(
        () -> {
          try (var statement =
              connection.prepareStatement("SELECT content FROM checkpoints WHERE name=?")) {
            statement.setString(1, name);
            try (var result = statement.executeQuery()) {
              reads++;
              return result.next()
                  ? Optional.of(new Checkpoint(name, result.getString(1)))
                  : Optional.empty();
            }
          }
        });
  }

  @Override
  public synchronized long internOperator(String digest, String canonicalContent) {
    if (digest == null
        || digest.isBlank()
        || canonicalContent == null
        || canonicalContent.isBlank())
      throw new IllegalArgumentException("Operator identity required");
    return access(
        () ->
            transaction(
                () -> {
                  try (var statement =
                      connection.prepareStatement(
                          "SELECT id,content FROM operators WHERE digest=?")) {
                    statement.setString(1, digest);
                    try (var result = statement.executeQuery()) {
                      reads++;
                      while (result.next())
                        if (canonicalContent.equals(result.getString(2))) return result.getLong(1);
                    }
                  }
                  update(
                      "INSERT INTO operators(digest,content) VALUES(?,?)",
                      digest,
                      canonicalContent);
                  return number("SELECT last_insert_rowid()");
                }));
  }

  @Override
  public synchronized String operator(long id) {
    return access(
        () -> {
          try (var statement =
              connection.prepareStatement("SELECT content FROM operators WHERE id=?")) {
            statement.setLong(1, id);
            try (var result = statement.executeQuery()) {
              reads++;
              if (!result.next()) throw new IllegalArgumentException("Unknown operator");
              return result.getString(1);
            }
          }
        });
  }

  @Override
  public synchronized Metrics metrics() {
    return access(
        () -> {
          long bytes = 0;
          try {
            for (var file : List.of(path, Path.of(path + "-wal"), Path.of(path + "-shm")))
              if (Files.exists(file)) bytes += Files.size(file);
          } catch (IOException error) {
            throw new IllegalStateException("Cannot inspect graph storage", error);
          }
          long cursorCount = number("SELECT COUNT(*) FROM execution_cursors");
          long cursorRows = number("SELECT COUNT(*) FROM execution_rows");
          return new Metrics(
              number("SELECT COUNT(*) FROM sources"),
              number("SELECT COUNT(*) FROM methods"),
              number("SELECT COUNT(*) FROM operators"),
              number("SELECT COUNT(*) FROM observations"),
              number("SELECT COUNT(*) FROM observations WHERE status='READY'"),
              bytes,
              reads,
              writes,
              cacheKiB,
              cursorCount,
              cursorRows);
        });
  }

  @Override
  public synchronized void close() {
    if (closed) return;
    access(
        () -> {
          connection.close();
          return null;
        });
    closed = true;
  }
}
