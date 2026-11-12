package com.poe2craft.community.api;

import com.poe2craft.community.application.CommunityReactions;
import jakarta.servlet.http.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/community/posts/{id}")
public class CommunityReactionController {
  private final CommunityReactions reactions;
  private final CommunityIdentity identity;

  public CommunityReactionController(CommunityReactions reactions, CommunityIdentity identity) {
    this.reactions = reactions;
    this.identity = identity;
  }

  @GetMapping("/stats")
  public CommunityReactions.Stats stats(
      @PathVariable UUID id, HttpServletRequest r, HttpServletResponse s) {
    s.setHeader("Cache-Control", "no-store");
    return reactions.stats(id, identity.actor(r, s, false));
  }

  public record Download(String action) {}

  public record Recommendation(Boolean enabled) {}

  @PostMapping("/download")
  public CommunityReactions.Stats download(
      @PathVariable UUID id,
      @RequestBody Download body,
      HttpServletRequest r,
      HttpServletResponse s) {
    if (body.action() == null || !Set.of("import", "craft", "favorite").contains(body.action()))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    return reactions.download(id, identity.actor(r, s, true));
  }

  @PutMapping("/recommendation")
  public CommunityReactions.Stats recommend(
      @PathVariable UUID id,
      @RequestBody Recommendation body,
      HttpServletRequest r,
      HttpServletResponse s) {
    if (body.enabled() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    return reactions.recommend(id, identity.actor(r, s, true), body.enabled());
  }
}
