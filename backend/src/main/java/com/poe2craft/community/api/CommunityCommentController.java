package com.poe2craft.community.api;

import com.poe2craft.community.application.CommunityComments;
import jakarta.servlet.http.*;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

/**
 * 공개 제작 세션의 댓글 작성·조회·삭제 요청을 처리한다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code comments}: 댓글목록 ({@code CommunityComments}).
 *   <li>{@code identity}: 정체성 ({@code CommunityIdentity}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code CommunityCommentController(comments, identity)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
 *       comments}(댓글목록), {@code identity}(정체성).
 *   <li>{@code list(post, page)}: 조건에 맞는 목록을 조회한다. 입력: {@code post}(게시물), {@code page}(페이지). 반환:
 *       {@code CommunityComments.Page}.
 *   <li>{@code create(post, body, r, s)}: 새 작업 또는 값을 생성한다. 입력: {@code post}(게시물), {@code body}(본문),
 *       {@code r}(r), {@code s}(s). 반환: {@code CommunityComments.Comment}.
 *   <li>{@code update(post, id, body, r, s)}: 갱신 처리에서 {@code comments.update}, {@code
 *       identity.actor}을(를) 사용한다. 입력: {@code post}(게시물), {@code id}(식별자), {@code body}(본문), {@code
 *       r}(r), {@code s}(s). 반환: {@code CommunityComments.Comment}.
 *   <li>{@code delete(post, id, body, r, s)}: 삭제 처리에서 {@code comments.delete}, {@code
 *       identity.actor}을(를) 사용한다. 입력: {@code post}(게시물), {@code id}(식별자), {@code body}(본문), {@code
 *       r}(r), {@code s}(s).
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Write}: {@code CommunityCommentController}에서 사용하는 쓰기의 불변 값이다.
 * </ul>
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

  /**
   * {@code CommunityCommentController}에서 사용하는 쓰기의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code id}: 이 값의 식별자 ({@code UUID}).
   *   <li>{@code body}: 본문 ({@code String}).
   *   <li>{@code revision}: 결과를 갱신할 때 비교하는 리비전 ({@code long}).
   * </ul>
   */
  public record Write(UUID id, String body, long revision) {}

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
