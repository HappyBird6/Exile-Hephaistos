package com.poe2craft.community.application;

import java.util.*;

/**
 * 게시물 댓글의 조회·작성·삭제 계약을 정의한다.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code list(post, page)}: 조건에 맞는 목록을 조회한다. 입력: {@code post}(게시물), {@code page}(페이지). 반환:
 *       {@code Page}.
 *   <li>{@code create(post, actor, id, body)}: 새 작업 또는 값을 생성한다. 입력: {@code post}(게시물), {@code
 *       actor}(작업주체), {@code id}(식별자), {@code body}(본문). 반환: {@code Comment}.
 *   <li>{@code update(post, id, actor, body, revision)}: 갱신의 실행 계약을 정의한다. 입력: {@code post}(게시물),
 *       {@code id}(식별자), {@code actor}(작업주체), {@code body}(본문), {@code revision}(리비전). 반환: {@code
 *       Comment}.
 *   <li>{@code delete(post, id, actor, revision)}: 삭제의 실행 계약을 정의한다. 입력: {@code post}(게시물), {@code
 *       id}(식별자), {@code actor}(작업주체), {@code revision}(리비전).
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Comment}: {@code CommunityComments}에서 사용하는 댓글의 불변 값이다.
 *   <li>{@code Page}: {@code CommunityComments}에서 사용하는 페이지의 불변 값이다.
 * </ul>
 */
public interface CommunityComments {
  /**
   * {@code CommunityComments}에서 사용하는 댓글의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code id}: 이 값의 식별자 ({@code UUID}).
   *   <li>{@code authorId}: 작성자·식별자 ({@code UUID}).
   *   <li>{@code author}: 작성자 ({@code String}).
   *   <li>{@code body}: 본문 ({@code String}).
   *   <li>{@code revision}: 결과를 갱신할 때 비교하는 리비전 ({@code long}).
   *   <li>{@code createdAt}: 생성시각·기준 ({@code String}).
   *   <li>{@code updatedAt}: 갱신시각·기준 ({@code String}).
   * </ul>
   */
  record Comment(
      UUID id,
      UUID authorId,
      String author,
      String body,
      long revision,
      String createdAt,
      String updatedAt) {}

  /**
   * {@code CommunityComments}에서 사용하는 페이지의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code comments}: 댓글목록 ({@code List<Comment>}).
   *   <li>{@code total}: 합계 ({@code long}).
   *   <li>{@code page}: 페이지 ({@code int}).
   *   <li>{@code pageSize}: 페이지·크기 ({@code int}).
   * </ul>
   */
  record Page(List<Comment> comments, long total, int page, int pageSize) {}

  Page list(UUID post, int page);

  Comment create(UUID post, SessionBoard.Actor actor, UUID id, String body);

  Comment update(UUID post, UUID id, SessionBoard.Actor actor, String body, long revision);

  void delete(UUID post, UUID id, SessionBoard.Actor actor, long revision);
}
