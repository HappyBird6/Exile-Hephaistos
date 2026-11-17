package com.poe2craft.community.application;

import java.util.UUID;

/**
 * 게시물에 대한 사용자 반응과 집계 계약을 정의한다.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code stats(post, actor)}: 수치목록의 실행 계약을 정의한다. 입력: {@code post}(게시물), {@code actor}(작업주체).
 *       반환: {@code Stats}.
 *   <li>{@code download(post, actor)}: 다운로드의 실행 계약을 정의한다. 입력: {@code post}(게시물), {@code
 *       actor}(작업주체). 반환: {@code Stats}.
 *   <li>{@code recommend(post, actor, enabled)}: 현재 계산에서 제공할 추천 결과를 구성한다. 입력: {@code post}(게시물),
 *       {@code actor}(작업주체), {@code enabled}(사용여부). 반환: {@code Stats}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Stats}: {@code CommunityReactions}에서 사용하는 수치목록의 불변 값이다.
 * </ul>
 */
public interface CommunityReactions {
  /**
   * {@code CommunityReactions}에서 사용하는 수치목록의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code downloads}: downloads ({@code long}).
   *   <li>{@code recommendations}: 추천목록 ({@code long}).
   *   <li>{@code recommended}: recommended ({@code boolean}).
   * </ul>
   */
  record Stats(long downloads, long recommendations, boolean recommended) {}

  Stats stats(UUID post, SessionBoard.Actor actor);

  Stats download(UUID post, SessionBoard.Actor actor);

  Stats recommend(UUID post, SessionBoard.Actor actor, boolean enabled);
}
