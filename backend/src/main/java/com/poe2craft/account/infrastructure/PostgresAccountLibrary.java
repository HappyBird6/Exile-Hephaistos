package com.poe2craft.account.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.account.application.AccountLibrary;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 소유자별 제작 세션을 PostgreSQL에 저장하고 조회한다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code db}: DB ({@code JdbcTemplate}).
 *   <li>{@code json}: 이 경계의 JSON 읽기·쓰기 변환기 ({@code ObjectMapper}).
 *   <li>{@code maxBytes}: 최대값·바이트수 ({@code int}).
 *   <li>{@code maxDocuments}: 최대값·문서목록 ({@code int}).
 *   <li>{@code readBatchSize}: 읽기·일괄실행·크기 ({@code int}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code PostgresAccountLibrary(db, json, maxBytes, maxDocuments, readBatchSize)}: 전달받은 값으로
 *       구성원을 초기화한다. 입력: {@code db}(DB), {@code json}(JSON), {@code maxBytes}(최대값·바이트수), {@code
 *       maxDocuments}(최대값·문서목록), {@code readBatchSize}(읽기·일괄실행·크기). 사용하는 예외: {@code
 *       IllegalArgumentException}.
 *   <li>{@code lockedGeneration(owner)}: 잠긴·세대번호 처리에서 {@code db.queryForList}, {@code
 *       rows.isEmpty}을(를) 사용한다. 입력: {@code owner}(소유자). 반환: {@code long}. 사용하는 예외: {@code
 *       ResponseStatusException}.
 *   <li>{@code snapshot(owner)}: 현재 내용을 불변 관측값으로 구성한다. 입력: {@code owner}(소유자). 반환: {@code
 *       Snapshot}.
 *   <li>{@code clearSessions(owner)}: clear·세션목록 처리에서 {@code db.update}을(를) 사용한다. 입력: {@code
 *       owner}(소유자). 반환: {@code long}.
 *   <li>{@code saveSession(owner, id, version, generation, content)}: 세션을(를) 저장한다. 입력: {@code
 *       owner}(소유자), {@code id}(식별자), {@code version}(버전), {@code generation}(세대번호), {@code
 *       content}(내용). 반환: {@code Document}. 사용하는 예외: {@code ResponseStatusException}.
 *   <li>{@code readBatchSize()}: {@code readBatchSize} 값을 조회한다. 반환: {@code int}.
 *   <li>{@code readBatch(owner, generation, references)}: 일괄실행을(를) 읽는다. 입력: {@code owner}(소유자),
 *       {@code generation}(세대번호), {@code references}(참조목록). 반환: {@code List<Document>}. 사용하는 예외:
 *       {@code ResponseStatusException}, {@code IllegalStateException}.
 *   <li>{@code signIn(subject, name)}: 사용자 인증을 수행한다. 입력: {@code subject}(subject), {@code
 *       name}(이름). 반환: {@code Account}. 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code index(owner)}: 색인 처리에서 {@code db.query}, {@code r.getString}을(를) 사용한다. 입력: {@code
 *       owner}(소유자). 반환: {@code List<Reference>}.
 *   <li>{@code read(owner, kind, id)}: 지정한 입력 또는 저장된 값을 읽는다. 입력: {@code owner}(소유자), {@code
 *       kind}(종류), {@code id}(식별자). 반환: {@code Document}. 사용하는 예외: {@code IllegalStateException},
 *       {@code ResponseStatusException}.
 *   <li>{@code list(owner)}: 조건에 맞는 목록을 조회한다. 입력: {@code owner}(소유자). 반환: {@code List<Document>}.
 *       사용하는 예외: {@code IllegalStateException}.
 *   <li>{@code save(owner, kind, id, expectedVersion, content)}: 현재 값을 저장 경계에 기록한다. 입력: {@code
 *       owner}(소유자), {@code kind}(종류), {@code id}(식별자), {@code expectedVersion}(기대값·버전), {@code
 *       content}(내용). 반환: {@code Document}. 사용하는 예외: {@code ResponseStatusException}.
 * </ul>
 */
@Repository
public class PostgresAccountLibrary implements AccountLibrary {
  private final JdbcTemplate db;
  private final ObjectMapper json;
  private final int maxBytes;
  private final int maxDocuments;
  private final int readBatchSize;

  public PostgresAccountLibrary(
      JdbcTemplate db,
      ObjectMapper json,
      @Value("${account.library.max-document-bytes}") int maxBytes,
      @Value("${account.library.max-documents}") int maxDocuments,
      @Value("${account.library.read-batch-size}") int readBatchSize) {
    this.db = db;
    this.json = json;
    this.maxBytes = maxBytes;
    this.maxDocuments = maxDocuments;
    if (readBatchSize < 1 || readBatchSize > 20)
      throw new IllegalArgumentException("Library read batch size must be between 1 and 20");
    this.readBatchSize = readBatchSize;
  }

  private long lockedGeneration(UUID owner) {
    var rows =
        db.queryForList(
            "SELECT session_generation FROM app_user.account WHERE id=? FOR UPDATE",
            Long.class,
            owner);
    if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    return rows.getFirst();
  }

  @Override
  @Transactional
  public Snapshot snapshot(UUID owner) {
    return new Snapshot(lockedGeneration(owner), index(owner));
  }

  @Override
  @Transactional
  public long clearSessions(UUID owner) {
    long generation = lockedGeneration(owner) + 1;
    db.update("UPDATE app_user.account SET session_generation=? WHERE id=?", generation, owner);
    // The generation invalidates every pre-clear upload, so individual tombstones are no
    // longer needed. Release their quota as well as their payloads when clearing all sessions.
    db.update("DELETE FROM app_user.library_document WHERE account_id=? AND kind='session'", owner);
    return generation;
  }

  @Override
  @Transactional
  public Document saveSession(
      UUID owner, String id, long version, long generation, JsonNode content) {
    if (lockedGeneration(owner) != generation)
      throw new ResponseStatusException(HttpStatus.CONFLICT, "SESSIONS_CLEARED");
    return save(owner, "session", id, version, content);
  }

  @Override
  public int readBatchSize() {
    return readBatchSize;
  }

  @Override
  @Transactional
  public List<Document> readBatch(UUID owner, long generation, List<ReadKey> references) {
    if (generation < 0
        || references == null
        || references.isEmpty()
        || references.size() > readBatchSize)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_LIBRARY_BATCH");
    var keys = new HashSet<String>();
    for (var ref : references) {
      if (ref == null
          || ref.kind() == null
          || !Set.of("session", "base-favorite", "material-favorites").contains(ref.kind())
          || ref.id() == null
          || !ref.id().matches("[A-Za-z0-9_.:-]{1,160}")
          || ref.version() < 1
          || !keys.add(ref.kind() + "/" + ref.id()))
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_LIBRARY_BATCH");
    }
    // All saves/clears take this same owner lock, keeping versions and generation consistent.
    if (lockedGeneration(owner) != generation)
      throw new ResponseStatusException(HttpStatus.CONFLICT, "SESSIONS_CLEARED");
    var args = new ArrayList<Object>();
    args.add(owner);
    for (var ref : references) {
      args.add(ref.kind());
      args.add(ref.id());
    }
    var rows =
        db.query(
            "SELECT kind,document_id,version,content::text FROM app_user.library_document WHERE account_id=? AND (kind,document_id) IN ("
                + String.join(",", Collections.nCopies(references.size(), "(?,?)"))
                + ")",
            (r, n) -> {
              try {
                return new Document(
                    r.getString(1),
                    r.getString(2),
                    r.getLong(3),
                    r.getString(4) == null ? null : json.readTree(r.getString(4)));
              } catch (Exception e) {
                throw new IllegalStateException("Stored document unreadable", e);
              }
            },
            args.toArray());
    var found = new HashMap<String, Document>();
    for (var row : rows) found.put(row.kind() + "/" + row.id(), row);
    var ordered = new ArrayList<Document>();
    for (var ref : references) {
      var row = found.get(ref.kind() + "/" + ref.id());
      if (row == null || row.content() == null || row.version() != ref.version())
        throw new ResponseStatusException(HttpStatus.CONFLICT, "LIBRARY_VERSION_CONFLICT");
      ordered.add(row);
    }
    return List.copyOf(ordered);
  }

  @Override
  public Account signIn(String subject, String name) {
    if (subject == null || subject.isBlank() || subject.length() > 255)
      throw new IllegalArgumentException("Invalid identity");
    String label =
        name == null || name.isBlank() ? "Player" : name.substring(0, Math.min(160, name.length()));
    return db.queryForObject(
        "INSERT INTO app_user.account(id,google_subject,display_name) VALUES (?,?,?) ON CONFLICT(google_subject) DO UPDATE SET display_name=EXCLUDED.display_name RETURNING id,display_name",
        (r, n) -> new Account(r.getObject(1, UUID.class), r.getString(2)),
        UUID.randomUUID(),
        subject,
        label);
  }

  @Override
  public List<Reference> index(UUID owner) {
    return db.query(
        "SELECT kind,document_id,version,content IS NULL FROM app_user.library_document WHERE account_id=? ORDER BY kind,document_id",
        (r, n) -> new Reference(r.getString(1), r.getString(2), r.getLong(3), r.getBoolean(4)),
        owner);
  }

  @Override
  public Document read(UUID owner, String kind, String id) {
    var rows =
        db.query(
            "SELECT version,content::text FROM app_user.library_document WHERE account_id=? AND kind=? AND document_id=?",
            (r, n) -> {
              try {
                return new Document(
                    kind,
                    id,
                    r.getLong(1),
                    r.getString(2) == null ? null : json.readTree(r.getString(2)));
              } catch (Exception e) {
                throw new IllegalStateException("Stored document unreadable", e);
              }
            },
            owner,
            kind,
            id);
    if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    return rows.getFirst();
  }

  @Override
  public List<Document> list(UUID owner) {
    return db.query(
        "SELECT kind,document_id,version,content::text FROM app_user.library_document WHERE account_id=? ORDER BY kind,document_id",
        (r, n) -> {
          try {
            return new Document(
                r.getString(1),
                r.getString(2),
                r.getLong(3),
                r.getString(4) == null ? null : json.readTree(r.getString(4)));
          } catch (Exception e) {
            throw new IllegalStateException("Stored document unreadable", e);
          }
        },
        owner);
  }

  @Override
  @Transactional
  public Document save(UUID owner, String kind, String id, long expectedVersion, JsonNode content) {
    if (!Set.of("session", "base-favorite", "material-favorites").contains(kind)
        || id == null
        || !id.matches("[A-Za-z0-9_.:-]{1,160}")
        || expectedVersion < 0)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_LIBRARY_DOCUMENT");
    if (content != null && content.isNull()) content = null;
    String body = content == null ? null : content.toString();
    if (body != null && body.getBytes(StandardCharsets.UTF_8).length > maxBytes)
      throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "LIBRARY_DOCUMENT_TOO_LARGE");
    if (content != null
        && (!content.isObject()
            || (!kind.equals("material-favorites")
                && (!content.path("id").asText().equals(id)
                    || !content.path("frames").isArray()
                    || content.path("frames").isEmpty()))))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_LIBRARY_DOCUMENT");
    // Serialize the per-account quota check, including simultaneous first inserts.
    var owners =
        db.queryForList("SELECT id FROM app_user.account WHERE id=? FOR UPDATE", UUID.class, owner);
    if (owners.isEmpty()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    if (expectedVersion == 0) {
      if (db.queryForObject(
              "SELECT count(*) FROM app_user.library_document WHERE account_id=?",
              Long.class,
              owner)
          >= maxDocuments) throw new ResponseStatusException(HttpStatus.CONFLICT, "LIBRARY_FULL");
      int inserted =
          db.update(
              "INSERT INTO app_user.library_document(account_id,kind,document_id,version,content) VALUES (?,?,?,1,?::jsonb) ON CONFLICT DO NOTHING",
              owner,
              kind,
              id,
              body);
      if (inserted != 1)
        throw new ResponseStatusException(HttpStatus.CONFLICT, "LIBRARY_VERSION_CONFLICT");
    } else {
      int updated =
          db.update(
              "UPDATE app_user.library_document SET content=?::jsonb,version=version+1,updated_at=now() WHERE account_id=? AND kind=? AND document_id=? AND version=?",
              body,
              owner,
              kind,
              id,
              expectedVersion);
      if (updated != 1)
        throw new ResponseStatusException(HttpStatus.CONFLICT, "LIBRARY_VERSION_CONFLICT");
    }
    return new Document(kind, id, expectedVersion + 1, content);
  }
}
