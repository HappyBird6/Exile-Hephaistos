package com.poe2craft.community.api;

import com.poe2craft.community.application.SessionBoard;
import jakarta.servlet.http.*;
import java.util.*;
import org.springframework.web.bind.annotation.*;

/**
 * 공개 제작 세션의 목록·게시·수정·삭제 요청을 처리한다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code board}: 게시판 ({@code SessionBoard}).
 *   <li>{@code identity}: 정체성 ({@code CommunityIdentity}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code CommunityController(board, identity)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code board}(게시판),
 *       {@code identity}(정체성).
 *   <li>{@code context(r, s)}: 문맥 처리에서 {@code identity.actor}, {@code s.setHeader}을(를) 사용한다. 입력:
 *       {@code r}(r), {@code s}(s). 반환: {@code Map<String, Object>}.
 *   <li>{@code options()}: options 값을 조회·구성한다. 반환: {@code Map<String, Object>}.
 *   <li>{@code list(base, version, author, page, sort, itemClass)}: 조건에 맞는 목록을 조회한다. 입력: {@code
 *       base}(베이스), {@code version}(버전), {@code author}(작성자), {@code page}(페이지), {@code
 *       sort}(sort), {@code itemClass}(아이템·동치류). 반환: {@code SessionBoard.Page}.
 *   <li>{@code get(id)}: 키 또는 위치에 해당하는 값을 조회한다. 입력: {@code id}(식별자). 반환: {@code SessionBoard.Post}.
 *   <li>{@code mine(source, r, s)}: mine 처리에서 {@code s.setHeader}, {@code board.mine}을(를) 사용한다. 입력:
 *       {@code source}(출처), {@code r}(r), {@code s}(s). 반환: {@code List<SessionBoard.Summary>}.
 *   <li>{@code create(d, r, s)}: 새 작업 또는 값을 생성한다. 입력: {@code d}(d), {@code r}(r), {@code s}(s). 반환:
 *       {@code SessionBoard.Post}.
 *   <li>{@code update(id, d, r, s)}: 갱신 처리에서 {@code board.update}, {@code identity.actor}을(를) 사용한다.
 *       입력: {@code id}(식별자), {@code d}(d), {@code r}(r), {@code s}(s). 반환: {@code
 *       SessionBoard.Post}.
 *   <li>{@code editText(id, edit, r, s)}: 편집·텍스트 처리에서 {@code board.editText}, {@code
 *       identity.actor}을(를) 사용한다. 입력: {@code id}(식별자), {@code edit}(편집), {@code r}(r), {@code
 *       s}(s). 반환: {@code SessionBoard.Post}.
 *   <li>{@code delete(id, d, r, s)}: 삭제 처리에서 {@code board.delete}, {@code identity.actor}을(를) 사용한다.
 *       입력: {@code id}(식별자), {@code d}(d), {@code r}(r), {@code s}(s).
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code TextEdit}: {@code CommunityController}에서 사용하는 텍스트·편집의 불변 값이다.
 *   <li>{@code Deletion}: {@code CommunityController}에서 사용하는 Deletion의 불변 값이다.
 * </ul>
 */
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
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "newest") String sort,
      @RequestParam(required = false) String itemClass) {
    return board.list(base, version, author, page, sort, itemClass);
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

  /**
   * {@code CommunityController}에서 사용하는 텍스트·편집의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code title}: 제목 ({@code String}).
   *   <li>{@code body}: 본문 ({@code String}).
   *   <li>{@code revision}: 결과를 갱신할 때 비교하는 리비전 ({@code long}).
   * </ul>
   */
  public record TextEdit(String title, String body, long revision) {}

  @PutMapping("/posts/{id}/text")
  public SessionBoard.Post editText(
      @PathVariable UUID id,
      @RequestBody TextEdit edit,
      HttpServletRequest r,
      HttpServletResponse s) {
    return board.editText(
        identity.actor(r, s, true), id, edit.title(), edit.body(), edit.revision());
  }

  /**
   * {@code CommunityController}에서 사용하는 Deletion의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code password}: 비밀번호 ({@code String}).
   *   <li>{@code revision}: 결과를 갱신할 때 비교하는 리비전 ({@code long}).
   * </ul>
   */
  public record Deletion(String password, long revision) {}

  @PostMapping("/posts/{id}/delete")
  public void delete(
      @PathVariable UUID id, @RequestBody Deletion d, HttpServletRequest r, HttpServletResponse s) {
    board.delete(identity.actor(r, s, true), id, d.password(), d.revision());
  }
}
