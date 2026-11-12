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

@Repository
public class PostgresAccountLibrary implements AccountLibrary {
  private final JdbcTemplate db;
  private final ObjectMapper json;
  private final int maxBytes;
  private final int maxDocuments;

  public PostgresAccountLibrary(
      JdbcTemplate db,
      ObjectMapper json,
      @Value("${account.library.max-document-bytes}") int maxBytes,
      @Value("${account.library.max-documents}") int maxDocuments) {
    this.db = db;
    this.json = json;
    this.maxBytes = maxBytes;
    this.maxDocuments = maxDocuments;
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
    db.update(
        "UPDATE app_user.library_document SET content=NULL,version=version+1,updated_at=now() WHERE account_id=? AND kind='session'",
        owner);
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
