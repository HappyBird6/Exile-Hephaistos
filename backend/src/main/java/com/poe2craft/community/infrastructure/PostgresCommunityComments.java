package com.poe2craft.community.infrastructure;

import com.poe2craft.community.application.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Repository
public class PostgresCommunityComments implements CommunityComments {
  private final JdbcTemplate db;
  private final int maxLength, pageSize;

  public PostgresCommunityComments(
      JdbcTemplate db,
      @Value("${community.max-comment-length}") int maxLength,
      @Value("${community.page-size}") int pageSize) {
    this.db = db;
    this.maxLength = maxLength;
    this.pageSize = pageSize;
  }

  private void post(UUID post, boolean lock) {
    if (db.queryForList(
            "SELECT id FROM community.post WHERE id=? AND NOT deleted"
                + (lock ? " FOR UPDATE" : ""),
            UUID.class,
            post)
        .isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
  }

  private void member(SessionBoard.Actor actor) {
    if (actor.account() == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
  }

  private String body(String body) {
    if (body == null || body.isBlank() || body.length() > maxLength)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_COMMENT");
    return body.strip();
  }

  private RowMapper<Comment> mapper() {
    return (r, n) ->
        new Comment(
            r.getObject("id", UUID.class),
            r.getObject("author_id", UUID.class),
            r.getString("display_name"),
            r.getString("body"),
            r.getLong("revision"),
            r.getTimestamp("created_at").toInstant().toString(),
            r.getTimestamp("updated_at").toInstant().toString());
  }

  private final String select =
      "SELECT c.*,a.display_name FROM community.comment c JOIN app_user.account a ON a.id=c.author_id ";

  private Comment read(UUID post, UUID id) {
    var rows = db.query(select + "WHERE c.post_id=? AND c.id=?", mapper(), post, id);
    if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    return rows.getFirst();
  }

  @Override
  public Page list(UUID post, int page) {
    post(post, false);
    if (page < 1 || page > 10000) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    long total =
        db.queryForObject(
            "SELECT count(*) FROM community.comment WHERE post_id=?", Long.class, post);
    return new Page(
        db.query(
            select + "WHERE c.post_id=? ORDER BY c.created_at DESC,c.id LIMIT ? OFFSET ?",
            mapper(),
            post,
            pageSize,
            (page - 1) * pageSize),
        total,
        page,
        pageSize);
  }

  @Override
  @Transactional
  public Comment create(UUID post, SessionBoard.Actor actor, UUID id, String body) {
    member(actor);
    post(post, true);
    body = body(body);
    if (id == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    int inserted =
        db.update(
            "INSERT INTO community.comment(id,post_id,author_id,body) VALUES (?,?,?,?) ON CONFLICT DO NOTHING",
            id,
            post,
            actor.account(),
            body);
    var comment = read(post, id);
    if (!comment.authorId().equals(actor.account())
        || (inserted == 0 && !comment.body().equals(body)))
      throw new ResponseStatusException(HttpStatus.CONFLICT);
    return comment;
  }

  @Override
  @Transactional
  public Comment update(UUID post, UUID id, SessionBoard.Actor actor, String body, long revision) {
    member(actor);
    post(post, true);
    if (!read(post, id).authorId().equals(actor.account()))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    if (db.update(
            "UPDATE community.comment SET body=?,revision=revision+1,updated_at=now() WHERE id=? AND post_id=? AND revision=?",
            body(body),
            id,
            post,
            revision)
        != 1) throw new ResponseStatusException(HttpStatus.CONFLICT);
    return read(post, id);
  }

  @Override
  @Transactional
  public void delete(UUID post, UUID id, SessionBoard.Actor actor, long revision) {
    member(actor);
    post(post, true);
    if (!read(post, id).authorId().equals(actor.account()))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    if (db.update(
            "DELETE FROM community.comment WHERE id=? AND post_id=? AND revision=?",
            id,
            post,
            revision)
        != 1) throw new ResponseStatusException(HttpStatus.CONFLICT);
  }
}
