package com.poe2craft.community.infrastructure;

import com.fasterxml.jackson.databind.*;
import com.poe2craft.community.application.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Semaphore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Repository
public class PostgresSessionBoard implements SessionBoard {
  private final JdbcTemplate db;
  private final ObjectMapper json;
  private final int maxBytes, maxFrames, pageSize, maxBody, maxComment;
  private final String currentVersion;
  private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder(12);
  private final Semaphore passwordSlots = new Semaphore(2);

  public PostgresSessionBoard(
      JdbcTemplate db,
      ObjectMapper json,
      @Value("${community.max-session-bytes}") int maxBytes,
      @Value("${community.max-frames}") int maxFrames,
      @Value("${community.page-size}") int pageSize,
      @Value("${community.max-body-length}") int maxBody,
      @Value("${community.max-comment-length}") int maxComment,
      @Value("${community.current-version}") String currentVersion) {
    this.db = db;
    this.json = json;
    this.maxBytes = maxBytes;
    this.maxFrames = maxFrames;
    this.pageSize = pageSize;
    this.maxBody = maxBody;
    this.maxComment = maxComment;
    this.currentVersion = currentVersion;
  }

  private final String select =
      "SELECT p.*,a.display_name FROM community.post p LEFT JOIN app_user.account a ON a.id=p.author_id ";

  private JsonNode parse(String raw) {
    try {
      return json.readTree(raw);
    } catch (Exception e) {
      throw new IllegalStateException("Invalid saved session", e);
    }
  }

  private RowMapper<Post> mapper() {
    return (r, n) ->
        new Post(
            r.getObject("id", UUID.class),
            r.getObject("author_id", UUID.class),
            r.getString("display_name"),
            r.getString("source_id"),
            r.getString("game_version"),
            r.getString("base_id"),
            r.getString("title"),
            r.getString("body"),
            parse(r.getString("film")),
            r.getInt("shared_step"),
            r.getLong("revision"),
            r.getTimestamp("created_at").toInstant().toString(),
            r.getTimestamp("updated_at").toInstant().toString());
  }

  private Summary summary(Post p) {
    return new Summary(
        p.id(),
        p.authorId(),
        p.author(),
        p.gameVersion(),
        p.baseId(),
        p.title(),
        p.film().path("frames").size(),
        p.createdAt(),
        p.updatedAt(),
        p.revision(),
        0,
        0);
  }

  @Override
  public Post get(UUID id) {
    var rows = db.query(select + "WHERE p.id=? AND NOT p.deleted", mapper(), id);
    if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    return rows.getFirst();
  }

  private String text(String value, int max) {
    if (value == null) return "";
    value = value.strip();
    if (value.length() > max)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TEXT_TOO_LONG");
    return value;
  }

  private JsonNode draft(Actor actor, Draft d) {
    if (d == null
        || d.id() == null
        || d.sourceId() == null
        || d.sourceId().isBlank()
        || d.sourceId().length() > 160
        || d.gameVersion() == null
        || !d.gameVersion().matches("[0-9][A-Za-z0-9.\\-]{0,31}")) SharedSession.bad();
    if (actor.account() == null
        && (!text(d.title(), 160).isEmpty() || !text(d.body(), maxBody).isEmpty()))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "GUEST_TEXT_DISABLED");
    return SharedSession.validate(d.film(), maxBytes, maxFrames);
  }

  private String hash(String password) {
    if (password == null
        || password.length() < 8
        || password.getBytes(StandardCharsets.UTF_8).length > 72)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_DELETION_PASSWORD");
    if (!passwordSlots.tryAcquire())
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);
    try {
      return passwords.encode(password);
    } finally {
      passwordSlots.release();
    }
  }

  private void authorize(Actor actor, UUID id, String password) {
    var row =
        db.queryForMap(
            "SELECT author_id,deletion_hash FROM community.post WHERE id=? FOR UPDATE", id);
    var owner = (UUID) row.get("author_id");
    if (owner != null) {
      if (!owner.equals(actor.account())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
      return;
    }
    if (password == null || password.getBytes(StandardCharsets.UTF_8).length > 72)
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    if (!passwordSlots.tryAcquire())
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);
    try {
      if (!passwords.matches(password, (String) row.get("deletion_hash")))
        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    } finally {
      passwordSlots.release();
    }
  }

  @Override
  @Transactional
  public Post create(Actor actor, Draft d) {
    var film = draft(actor, d);
    // Serialize duplicate submissions per author/browser without logging passwords or session
    // bodies.
    db.queryForList(
        "SELECT pg_advisory_xact_lock(hashtextextended(?,0))",
        String.valueOf(actor.account() == null ? actor.browser() : actor.account()));
    var existing = mine(actor, d.sourceId());
    if (!existing.isEmpty()) return get(existing.getFirst().id());
    String hash = actor.account() == null ? hash(d.password()) : null;
    var idRows = db.queryForList("SELECT id FROM community.post WHERE id=?", UUID.class, d.id());
    if (!idRows.isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT);
    db.update(
        "INSERT INTO community.post(id,author_id,browser_id,source_id,deletion_hash,game_version,base_id,title,body,film,shared_step) VALUES (?,?,?,?,?,?,?,?,?,?::jsonb,?)",
        d.id(),
        actor.account(),
        actor.browser(),
        d.sourceId(),
        hash,
        d.gameVersion(),
        film.path("frames").get(0).path("state").path("baseItemId").asText(),
        text(d.title(), 160),
        text(d.body(), maxBody),
        film.toString(),
        film.path("frames").size() - 1);
    return get(d.id());
  }

  @Override
  @Transactional
  public Post update(Actor actor, UUID id, Draft d) {
    var old = get(id);
    authorize(actor, id, d.password());
    old = get(id);
    if (d.revision() != old.revision())
      throw new ResponseStatusException(HttpStatus.CONFLICT, "POST_CHANGED");
    if (!old.sourceId().equals(d.sourceId())) SharedSession.bad();
    if (old.authorId() == null
        && (!text(d.title(), 160).isEmpty() || !text(d.body(), maxBody).isEmpty()))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "GUEST_TEXT_DISABLED");
    var film = draft(actor, d);
    var before = old.film().path("frames");
    var after = film.path("frames");
    if (after.size() < before.size()) SharedSession.bad();
    for (int i = 0; i < before.size(); i++)
      if (!before.get(i).equals(after.get(i)))
        throw new ResponseStatusException(HttpStatus.CONFLICT, "SESSION_PATH_CHANGED");
    db.update(
        "UPDATE community.post SET film=?::jsonb,title=?,body=?,game_version=?,revision=revision+1,updated_at=now() WHERE id=?",
        film.toString(),
        text(d.title(), 160),
        text(d.body(), maxBody),
        d.gameVersion(),
        id);
    return get(id);
  }

  @Override
  @Transactional
  public void delete(Actor actor, UUID id, String password, long revision) {
    get(id);
    authorize(actor, id, password);
    if (db.update(
            "UPDATE community.post SET deleted=true,revision=revision+1,updated_at=now() WHERE id=? AND revision=? AND NOT deleted",
            id,
            revision)
        != 1) throw new ResponseStatusException(HttpStatus.CONFLICT);
  }

  @Override
  public Page list(String base, String version, UUID author, int page, String sort) {
    if (!Set.of("newest", "popular").contains(sort))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    if (page < 1 || page > 10000) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    var args = new ArrayList<Object>();
    String where = "WHERE NOT p.deleted";
    if (base != null && !base.isBlank()) {
      where += " AND p.base_id=?";
      args.add(base);
    }
    if (version != null && !version.isBlank()) {
      where += " AND p.game_version=?";
      args.add(version);
    }
    if (author != null) {
      where += " AND p.author_id=?";
      args.add(author);
    }
    long total =
        db.queryForObject(
            "SELECT count(*) FROM community.post p " + where, Long.class, args.toArray());
    args.add(pageSize);
    args.add((page - 1) * pageSize);
    // Read metadata only; full films are fetched by the detail endpoint.
    var posts =
        db.query(
            "SELECT p.id,p.author_id,a.display_name,p.game_version,p.base_id,p.title,jsonb_array_length(p.film->'frames') steps,p.created_at,p.updated_at,p.revision,p.downloads,p.recommendations FROM community.post p LEFT JOIN app_user.account a ON a.id=p.author_id "
                + where
                + " ORDER BY "
                + (sort.equals("popular") ? "p.downloads DESC," : "")
                + "p.created_at DESC,p.id LIMIT ? OFFSET ?",
            (r, n) ->
                new Summary(
                    r.getObject(1, UUID.class),
                    r.getObject(2, UUID.class),
                    r.getString(3),
                    r.getString(4),
                    r.getString(5),
                    r.getString(6),
                    r.getInt(7),
                    r.getTimestamp(8).toInstant().toString(),
                    r.getTimestamp(9).toInstant().toString(),
                    r.getLong(10),
                    r.getLong(11),
                    r.getLong(12)),
            args.toArray());
    return new Page(posts, total, page, pageSize);
  }

  @Override
  public List<Summary> mine(Actor actor, String source) {
    if (source == null || source.length() > 160) SharedSession.bad();
    return db
        .query(
            select
                + "WHERE NOT p.deleted AND p.source_id=? AND "
                + (actor.account() == null
                    ? "p.author_id IS NULL AND p.browser_id=?"
                    : "p.author_id=?"),
            mapper(),
            source,
            actor.account() == null ? actor.browser() : actor.account())
        .stream()
        .map(this::summary)
        .toList();
  }

  @Override
  public Map<String, Object> options() {
    return Map.of(
        "limits",
        Map.of("bodyLength", maxBody, "commentLength", maxComment),
        "currentVersion",
        currentVersion,
        "versions",
        db.queryForList(
            "SELECT DISTINCT game_version FROM community.post WHERE NOT deleted ORDER BY game_version DESC",
            String.class),
        "bases",
        db.queryForList(
            "SELECT DISTINCT base_id FROM community.post WHERE NOT deleted ORDER BY base_id",
            String.class));
  }
}
