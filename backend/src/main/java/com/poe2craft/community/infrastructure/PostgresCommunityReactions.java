package com.poe2craft.community.infrastructure;

import com.poe2craft.community.application.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Repository
public class PostgresCommunityReactions implements CommunityReactions {
  private final JdbcTemplate db;

  public PostgresCommunityReactions(JdbcTemplate db) {
    this.db = db;
  }

  private void lock(UUID post) {
    if (db.queryForList(
            "SELECT id FROM community.post WHERE id=? AND NOT deleted FOR UPDATE", UUID.class, post)
        .isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
  }

  @Override
  public Stats stats(UUID post, SessionBoard.Actor actor) {
    var result =
        db.query(
            "SELECT downloads,recommendations,EXISTS(SELECT 1 FROM community.recommendation WHERE post_id=p.id AND account_id=?) FROM community.post p WHERE p.id=? AND NOT p.deleted",
            (r, n) -> new Stats(r.getLong(1), r.getLong(2), r.getBoolean(3)),
            actor.account(),
            post);
    if (result.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    return result.getFirst();
  }

  @Override
  @Transactional
  public Stats download(UUID post, SessionBoard.Actor actor) {
    lock(post);
    var browser =
        db.queryForList(
            "SELECT e.id,e.account_id FROM community.download_browser b JOIN community.download_entry e ON e.id=b.entry_id WHERE b.post_id=? AND b.browser_id=?",
            post,
            actor.browser());
    UUID browserEntry = browser.isEmpty() ? null : (UUID) browser.getFirst().get("id");
    UUID browserAccount = browser.isEmpty() ? null : (UUID) browser.getFirst().get("account_id");
    var member =
        actor.account() == null
            ? List.<UUID>of()
            : db.queryForList(
                "SELECT id FROM community.download_entry WHERE post_id=? AND account_id=?",
                UUID.class,
                post,
                actor.account());
    UUID entry = member.isEmpty() ? null : member.getFirst();
    if (actor.account() == null && browserEntry != null) return stats(post, actor);
    if (actor.account() != null
        && entry == null
        && browserEntry != null
        && browserAccount == null) {
      db.update(
          "UPDATE community.download_entry SET account_id=? WHERE id=?",
          actor.account(),
          browserEntry);
      return stats(post, actor);
    }
    if (entry == null) {
      entry = UUID.randomUUID();
      db.update(
          "INSERT INTO community.download_entry(id,post_id,account_id) VALUES (?,?,?)",
          entry,
          post,
          actor.account());
      db.update("UPDATE community.post SET downloads=downloads+1 WHERE id=?", post);
    }
    if (browserEntry == null)
      db.update(
          "INSERT INTO community.download_browser(post_id,browser_id,entry_id) VALUES (?,?,?)",
          post,
          actor.browser(),
          entry);
    else if (browserAccount == null && !browserEntry.equals(entry)) {
      db.update(
          "UPDATE community.download_browser SET entry_id=? WHERE entry_id=?", entry, browserEntry);
      db.update("DELETE FROM community.download_entry WHERE id=?", browserEntry);
      db.update("UPDATE community.post SET downloads=downloads-1 WHERE id=?", post);
    }
    return stats(post, actor);
  }

  @Override
  @Transactional
  public Stats recommend(UUID post, SessionBoard.Actor actor, boolean enabled) {
    if (actor.account() == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    lock(post);
    int changed =
        enabled
            ? db.update(
                "INSERT INTO community.recommendation(post_id,account_id) VALUES (?,?) ON CONFLICT DO NOTHING",
                post,
                actor.account())
            : db.update(
                "DELETE FROM community.recommendation WHERE post_id=? AND account_id=?",
                post,
                actor.account());
    if (changed > 0)
      db.update(
          "UPDATE community.post SET recommendations=recommendations+? WHERE id=?",
          enabled ? 1 : -1,
          post);
    return stats(post, actor);
  }
}
