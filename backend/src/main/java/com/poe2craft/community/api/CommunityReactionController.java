package com.poe2craft.community.api;

import com.poe2craft.community.application.CommunityReactions;
import jakarta.servlet.http.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * 공개 제작 세션에 대한 반응 조회와 변경 요청을 처리한다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code reactions}: 반응목록 ({@code CommunityReactions}).
 *   <li>{@code identity}: 정체성 ({@code CommunityIdentity}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code CommunityReactionController(reactions, identity)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
 *       reactions}(반응목록), {@code identity}(정체성).
 *   <li>{@code stats(id, r, s)}: 수치목록 처리에서 {@code s.setHeader}, {@code reactions.stats}을(를) 사용한다.
 *       입력: {@code id}(식별자), {@code r}(r), {@code s}(s). 반환: {@code CommunityReactions.Stats}.
 *   <li>{@code download(id, body, r, s)}: 다운로드 처리에서 {@code body.action}, {@code
 *       reactions.download}을(를) 사용한다. 입력: {@code id}(식별자), {@code body}(본문), {@code r}(r), {@code
 *       s}(s). 반환: {@code CommunityReactions.Stats}. 사용하는 예외: {@code ResponseStatusException}.
 *   <li>{@code recommend(id, body, r, s)}: 현재 계산에서 제공할 추천 결과를 구성한다. 입력: {@code id}(식별자), {@code
 *       body}(본문), {@code r}(r), {@code s}(s). 반환: {@code CommunityReactions.Stats}. 사용하는 예외:
 *       {@code ResponseStatusException}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Download}: {@code CommunityReactionController}에서 사용하는 다운로드의 불변 값이다.
 *   <li>{@code Recommendation}: {@code CommunityReactionController}에서 사용하는 추천의 불변 값이다.
 * </ul>
 */
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

  /**
   * {@code CommunityReactionController}에서 사용하는 다운로드의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code action}: 실행할 화폐·제작 행동 ({@code String}).
   * </ul>
   */
  public record Download(String action) {}

  /**
   * {@code CommunityReactionController}에서 사용하는 추천의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code enabled}: 사용여부 ({@code Boolean}).
   * </ul>
   */
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
