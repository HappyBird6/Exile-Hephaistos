package com.poe2craft.community.infrastructure;

import com.fasterxml.jackson.databind.*;
import com.poe2craft.community.application.*;
import com.poe2craft.shared.crafting.infrastructure.CraftingRegistryLoader;
import com.poe2craft.shared.item.BaseRegistry;
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

/**
 * 공개 세션 게시물을 PostgreSQL에 저장하고 조회한다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code db}: DB ({@code JdbcTemplate}).
 *   <li>{@code json}: 이 경계의 JSON 읽기·쓰기 변환기 ({@code ObjectMapper}).
 *   <li>{@code maxBytes}: 최대값·바이트수 ({@code int}).
 *   <li>{@code maxFrames}: 최대값·Frames ({@code int}).
 *   <li>{@code pageSize}: 페이지·크기 ({@code int}).
 *   <li>{@code maxBody}: 최대값·본문 ({@code int}).
 *   <li>{@code maxComment}: 최대값·댓글 ({@code int}).
 *   <li>{@code currentVersion}: 현재·버전 ({@code String}).
 *   <li>{@code passwords}: passwords ({@code BCryptPasswordEncoder}).
 *   <li>{@code passwordSlots}: 비밀번호·슬롯목록 ({@code Semaphore}).
 *   <li>{@code select}: 선택 ({@code String}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code PostgresSessionBoard(db, json, maxBytes, maxFrames, pageSize, maxBody, maxComment,
 *       currentVersion)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code db}(DB), {@code json}(JSON), {@code
 *       maxBytes}(최대값·바이트수), {@code maxFrames}(최대값·Frames), {@code pageSize}(페이지·크기), {@code
 *       maxBody}(최대값·본문), {@code maxComment}(최대값·댓글), {@code currentVersion}(현재·버전).
 *   <li>{@code parse(raw)}: 외부 표현을 내부 값으로 해석한다. 입력: {@code raw}(원문). 반환: {@code JsonNode}. 사용하는 예외:
 *       {@code IllegalStateException}.
 *   <li>{@code mapper()}: 변환기 값을 조회·구성한다. 반환: {@code RowMapper<Post>}.
 *   <li>{@code summary(p)}: 요약 처리에서 {@code p.id}, {@code p.authorId}을(를) 사용한다. 입력: {@code p}(p).
 *       반환: {@code Summary}.
 *   <li>{@code get(id)}: 키 또는 위치에 해당하는 값을 조회한다. 입력: {@code id}(식별자). 반환: {@code Post}. 사용하는 예외:
 *       {@code ResponseStatusException}.
 *   <li>{@code text(value, max)}: 텍스트 처리에서 {@code value.strip}, {@code value.length}을(를) 사용한다. 입력:
 *       {@code value}(값), {@code max}(최대값). 반환: {@code String}. 사용하는 예외: {@code
 *       ResponseStatusException}.
 *   <li>{@code draft(actor, d)}: 초안 처리에서 {@code d.id}, {@code d.sourceId}을(를) 사용한다. 입력: {@code
 *       actor}(작업주체), {@code d}(d). 반환: {@code JsonNode}. 사용하는 예외: {@code ResponseStatusException}.
 *   <li>{@code hash(password)}: 해시 처리에서 {@code password.length}, {@code password.getBytes}을(를)
 *       사용한다. 입력: {@code password}(비밀번호). 반환: {@code String}. 사용하는 예외: {@code
 *       ResponseStatusException}.
 *   <li>{@code authorize(actor, id, password)}: authorize 처리에서 {@code db.queryForMap}, {@code
 *       row.get}을(를) 사용한다. 입력: {@code actor}(작업주체), {@code id}(식별자), {@code password}(비밀번호). 사용하는
 *       예외: {@code ResponseStatusException}.
 *   <li>{@code create(actor, d)}: 새 작업 또는 값을 생성한다. 입력: {@code actor}(작업주체), {@code d}(d). 반환:
 *       {@code Post}. 사용하는 예외: {@code ResponseStatusException}.
 *   <li>{@code update(actor, id, d)}: 갱신 처리에서 {@code d.password}, {@code d.revision}을(를) 사용한다. 입력:
 *       {@code actor}(작업주체), {@code id}(식별자), {@code d}(d). 반환: {@code Post}. 사용하는 예외: {@code
 *       ResponseStatusException}.
 *   <li>{@code editText(actor, id, title, body, revision)}: 편집·텍스트 처리에서 {@code actor.account},
 *       {@code old.authorId}을(를) 사용한다. 입력: {@code actor}(작업주체), {@code id}(식별자), {@code title}(제목),
 *       {@code body}(본문), {@code revision}(리비전). 반환: {@code Post}. 사용하는 예외: {@code
 *       ResponseStatusException}.
 *   <li>{@code delete(actor, id, password, revision)}: 삭제 처리에서 {@code db.update}을(를) 사용한다. 입력:
 *       {@code actor}(작업주체), {@code id}(식별자), {@code password}(비밀번호), {@code revision}(리비전). 사용하는
 *       예외: {@code ResponseStatusException}.
 *   <li>{@code list(base, version, author, page, sort, itemClass)}: 조건에 맞는 목록을 조회한다. 입력: {@code
 *       base}(베이스), {@code version}(버전), {@code author}(작성자), {@code page}(페이지), {@code
 *       sort}(sort), {@code itemClass}(아이템·동치류). 반환: {@code Page}. 사용하는 예외: {@code
 *       ResponseStatusException}.
 *   <li>{@code mine(actor, source)}: mine 처리에서 {@code source.length}, {@code SharedSession.bad}을(를)
 *       사용한다. 입력: {@code actor}(작업주체), {@code source}(출처). 반환: {@code List<Summary>}.
 *   <li>{@code availableBases()}: 사용가능·베이스목록 값을 조회·구성한다. 반환: {@code List<String>}.
 *   <li>{@code options()}: options 값을 조회·구성한다. 반환: {@code Map<String, Object>}.
 * </ul>
 */
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
    return SharedSession.validate(
        d.film(), maxBytes, maxFrames, CraftingRegistryLoader::isChaosMaterial);
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
  public Post editText(Actor actor, UUID id, String title, String body, long revision) {
    var old = get(id);
    if (actor.account() == null || !actor.account().equals(old.authorId()))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    authorize(actor, id, null);
    if (db.update(
            "UPDATE community.post SET title=?,body=?,revision=revision+1,updated_at=now() WHERE id=? AND revision=? AND NOT deleted",
            text(title, 160),
            text(body, maxBody),
            id,
            revision)
        != 1) throw new ResponseStatusException(HttpStatus.CONFLICT, "POST_CHANGED");
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
  public Page list(
      String base, String version, UUID author, int page, String sort, String itemClass) {
    if (!Set.of("newest", "popular").contains(sort))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    if (page < 1 || page > 10000) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    Author authorInfo =
        author == null
            ? null
            : db
                .query(
                    "SELECT id,display_name FROM app_user.account WHERE id=?",
                    (r, n) -> new Author(r.getObject(1, UUID.class), r.getString(2)),
                    author)
                .stream()
                .findFirst()
                .orElse(null);
    var args = new ArrayList<Object>();
    String where = "WHERE NOT p.deleted";
    if (base != null && !base.isBlank()) {
      where += " AND p.base_id=?";
      args.add(base);
    }
    if (itemClass != null && !itemClass.isBlank()) {
      var ids =
          availableBases().stream()
              .filter(id -> itemClass.equals(BaseRegistry.itemClass(id)))
              .toList();
      if (ids.isEmpty()) return new Page(List.of(), 0, page, pageSize, authorInfo);
      where += " AND p.base_id IN (" + String.join(",", Collections.nCopies(ids.size(), "?")) + ")";
      args.addAll(ids);
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
    return new Page(posts, total, page, pageSize, authorInfo);
  }

  @Override
  public List<Summary> mine(Actor actor, String source) {
    if (source == null || source.length() > 160) SharedSession.bad();
    return db
        .query(
            select
                + "WHERE NOT p.deleted AND p.source_id=? AND "
                + "((p.author_id IS NULL AND p.browser_id=?) OR p.author_id=?) ORDER BY p.created_at DESC",
            mapper(),
            source,
            actor.browser(),
            actor.account())
        .stream()
        .map(this::summary)
        .toList();
  }

  private List<String> availableBases() {
    return BaseRegistry.supportedBaseIds().stream().sorted().toList();
  }

  @Override
  public Map<String, Object> options() {
    return Map.of(
        "limits",
        Map.of(
            "bodyLength",
            maxBody,
            "commentLength",
            maxComment,
            "sessionBytes",
            maxBytes,
            "frames",
            maxFrames),
        "currentVersion",
        currentVersion,
        "versions",
        db.queryForList(
            "SELECT DISTINCT game_version FROM community.post WHERE NOT deleted ORDER BY game_version DESC",
            String.class),
        "bases",
        availableBases(),
        "baseClasses",
        availableBases().stream()
            .collect(java.util.stream.Collectors.toMap(id -> id, BaseRegistry::itemClass)),
        "classes",
        availableBases().stream()
            .map(BaseRegistry::itemClass)
            .filter(Objects::nonNull)
            .distinct()
            .sorted()
            .toList());
  }
}
