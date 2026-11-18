package com.poe2craft.community.api;

import com.poe2craft.community.api.dto.Write;
import com.poe2craft.community.application.CommunityComments;
import jakarta.servlet.http.*;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

/**
 * identity로 작성자를 확인하고 comments로 댓글을 관리한다. list는 댓글 목록을 돌려주고 create·update·delete는 작성·수정·삭제를 처리한다.
 */
@RestController
@RequestMapping("/api/community/posts/{post}/comments")
public class CommunityCommentController {
  private final CommunityComments comments;
  private final CommunityIdentity identity;

  public CommunityCommentController(CommunityComments comments, CommunityIdentity identity) {
    this.comments = comments;
    this.identity = identity;
  }

  @GetMapping
  public CommunityComments.Page list(
      @PathVariable UUID post, @RequestParam(defaultValue = "1") int page) {
    return comments.list(post, page);
  }

  @PostMapping
  public CommunityComments.Comment create(
      @PathVariable UUID post,
      @RequestBody Write body,
      HttpServletRequest r,
      HttpServletResponse s) {
    return comments.create(post, identity.actor(r, s, true), body.id(), body.body());
  }

  @PutMapping("/{id}")
  public CommunityComments.Comment update(
      @PathVariable UUID post,
      @PathVariable UUID id,
      @RequestBody Write body,
      HttpServletRequest r,
      HttpServletResponse s) {
    return comments.update(post, id, identity.actor(r, s, true), body.body(), body.revision());
  }

  @PostMapping("/{id}/delete")
  public void delete(
      @PathVariable UUID post,
      @PathVariable UUID id,
      @RequestBody Write body,
      HttpServletRequest r,
      HttpServletResponse s) {
    comments.delete(post, id, identity.actor(r, s, true), body.revision());
  }
}
