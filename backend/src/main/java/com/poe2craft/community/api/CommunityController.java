package com.poe2craft.community.api;

import com.poe2craft.community.application.SessionBoard;
import jakarta.servlet.http.*;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/community")
public class CommunityController {
  private final SessionBoard board;
  private final CommunityIdentity identity;

  public CommunityController(SessionBoard board, CommunityIdentity identity) {
    this.board = board;
    this.identity = identity;
  }

  @GetMapping("/context")
  public Map<String, Object> context(HttpServletRequest r, HttpServletResponse s) {
    identity.actor(r, s, false);
    s.setHeader("Cache-Control", "no-store");
    var session = r.getSession();
    String token = (String) session.getAttribute("account.csrf");
    if (token == null) {
      token = UUID.randomUUID().toString();
      session.setAttribute("account.csrf", token);
    }
    return Map.of("csrf", token);
  }

  @GetMapping("/options")
  public Map<String, Object> options() {
    return board.options();
  }

  @GetMapping("/posts")
  public SessionBoard.Page list(
      @RequestParam(required = false) String base,
      @RequestParam(required = false) String version,
      @RequestParam(required = false) UUID author,
      @RequestParam(defaultValue = "1") int page) {
    return board.list(base, version, author, page);
  }

  @GetMapping("/posts/{id}")
  public SessionBoard.Post get(@PathVariable UUID id) {
    return board.get(id);
  }

  @GetMapping("/mine")
  public List<SessionBoard.Summary> mine(
      @RequestParam String source, HttpServletRequest r, HttpServletResponse s) {
    s.setHeader("Cache-Control", "no-store");
    return board.mine(identity.actor(r, s, true), source);
  }

  @PostMapping("/posts")
  public SessionBoard.Post create(
      @RequestBody SessionBoard.Draft d, HttpServletRequest r, HttpServletResponse s) {
    return board.create(identity.actor(r, s, true), d);
  }

  @PutMapping("/posts/{id}")
  public SessionBoard.Post update(
      @PathVariable UUID id,
      @RequestBody SessionBoard.Draft d,
      HttpServletRequest r,
      HttpServletResponse s) {
    return board.update(identity.actor(r, s, true), id, d);
  }

  public record Deletion(String password, long revision) {}

  @PostMapping("/posts/{id}/delete")
  public void delete(
      @PathVariable UUID id, @RequestBody Deletion d, HttpServletRequest r, HttpServletResponse s) {
    board.delete(identity.actor(r, s, true), id, d.password(), d.revision());
  }
}
