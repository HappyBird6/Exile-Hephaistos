package com.poe2craft.community.application;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;

/**
 * 공개 제작 세션 게시판의 저장·조회 계약을 정의한다.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code create(actor, draft)}: 새 작업 또는 값을 생성한다. 입력: {@code actor}(작업주체), {@code draft}(초안).
 *       반환: {@code Post}.
 *   <li>{@code update(actor, id, draft)}: 갱신의 실행 계약을 정의한다. 입력: {@code actor}(작업주체), {@code
 *       id}(식별자), {@code draft}(초안). 반환: {@code Post}.
 *   <li>{@code editText(actor, id, title, body, revision)}: 편집·텍스트의 실행 계약을 정의한다. 입력: {@code
 *       actor}(작업주체), {@code id}(식별자), {@code title}(제목), {@code body}(본문), {@code revision}(리비전).
 *       반환: {@code Post}.
 *   <li>{@code delete(actor, id, password, revision)}: 삭제의 실행 계약을 정의한다. 입력: {@code actor}(작업주체),
 *       {@code id}(식별자), {@code password}(비밀번호), {@code revision}(리비전).
 *   <li>{@code get(id)}: 키 또는 위치에 해당하는 값을 조회한다. 입력: {@code id}(식별자). 반환: {@code Post}.
 *   <li>{@code list(base, version, author, page)}: 조건에 맞는 목록을 조회한다. 입력: {@code base}(베이스), {@code
 *       version}(버전), {@code author}(작성자), {@code page}(페이지). 반환: {@code Page}.
 *   <li>{@code list(base, version, author, page, sort)}: 조건에 맞는 목록을 조회한다. 입력: {@code base}(베이스),
 *       {@code version}(버전), {@code author}(작성자), {@code page}(페이지), {@code sort}(sort). 반환: {@code
 *       Page}.
 *   <li>{@code list(base, version, author, page, sort, itemClass)}: 조건에 맞는 목록을 조회한다. 입력: {@code
 *       base}(베이스), {@code version}(버전), {@code author}(작성자), {@code page}(페이지), {@code
 *       sort}(sort), {@code itemClass}(아이템·동치류). 반환: {@code Page}.
 *   <li>{@code mine(actor, source)}: mine의 실행 계약을 정의한다. 입력: {@code actor}(작업주체), {@code
 *       source}(출처). 반환: {@code List<Summary>}.
 *   <li>{@code options()}: options 값을 조회·구성한다. 반환: {@code Map<String, Object>}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Actor}: {@code SessionBoard}에서 사용하는 작업주체의 불변 값이다.
 *   <li>{@code Draft}: {@code SessionBoard}에서 사용하는 초안의 불변 값이다.
 *   <li>{@code Post}: {@code SessionBoard}에서 사용하는 게시물의 불변 값이다.
 *   <li>{@code Summary}: {@code SessionBoard}에서 사용하는 요약의 불변 값이다.
 *   <li>{@code Author}: {@code SessionBoard}에서 사용하는 작성자의 불변 값이다.
 *   <li>{@code Page}: {@code SessionBoard}에서 사용하는 페이지의 불변 값이다.
 * </ul>
 */
public interface SessionBoard {
  /**
   * {@code SessionBoard}에서 사용하는 작업주체의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code account}: 계정 ({@code UUID}).
   *   <li>{@code browser}: browser ({@code UUID}).
   * </ul>
   */
  record Actor(UUID account, UUID browser) {}

  /**
   * {@code SessionBoard}에서 사용하는 초안의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code id}: 이 값의 식별자 ({@code UUID}).
   *   <li>{@code sourceId}: 출처·식별자 ({@code String}).
   *   <li>{@code gameVersion}: 게임·버전 ({@code String}).
   *   <li>{@code title}: 제목 ({@code String}).
   *   <li>{@code body}: 본문 ({@code String}).
   *   <li>{@code film}: film ({@code JsonNode}).
   *   <li>{@code password}: 비밀번호 ({@code String}).
   *   <li>{@code revision}: 결과를 갱신할 때 비교하는 리비전 ({@code long}).
   * </ul>
   */
  record Draft(
      UUID id,
      String sourceId,
      String gameVersion,
      String title,
      String body,
      JsonNode film,
      String password,
      long revision) {}

  /**
   * {@code SessionBoard}에서 사용하는 게시물의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code id}: 이 값의 식별자 ({@code UUID}).
   *   <li>{@code authorId}: 작성자·식별자 ({@code UUID}).
   *   <li>{@code author}: 작성자 ({@code String}).
   *   <li>{@code sourceId}: 출처·식별자 ({@code String}).
   *   <li>{@code gameVersion}: 게임·버전 ({@code String}).
   *   <li>{@code baseId}: 베이스·식별자 ({@code String}).
   *   <li>{@code title}: 제목 ({@code String}).
   *   <li>{@code body}: 본문 ({@code String}).
   *   <li>{@code film}: film ({@code JsonNode}).
   *   <li>{@code sharedStep}: 공유·단계 ({@code int}).
   *   <li>{@code revision}: 결과를 갱신할 때 비교하는 리비전 ({@code long}).
   *   <li>{@code createdAt}: 생성시각·기준 ({@code String}).
   *   <li>{@code updatedAt}: 갱신시각·기준 ({@code String}).
   * </ul>
   */
  record Post(
      UUID id,
      UUID authorId,
      String author,
      String sourceId,
      String gameVersion,
      String baseId,
      String title,
      String body,
      JsonNode film,
      int sharedStep,
      long revision,
      String createdAt,
      String updatedAt) {}

  /**
   * {@code SessionBoard}에서 사용하는 요약의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code id}: 이 값의 식별자 ({@code UUID}).
   *   <li>{@code authorId}: 작성자·식별자 ({@code UUID}).
   *   <li>{@code author}: 작성자 ({@code String}).
   *   <li>{@code gameVersion}: 게임·버전 ({@code String}).
   *   <li>{@code baseId}: 베이스·식별자 ({@code String}).
   *   <li>{@code title}: 제목 ({@code String}).
   *   <li>{@code steps}: 단계목록 ({@code int}).
   *   <li>{@code createdAt}: 생성시각·기준 ({@code String}).
   *   <li>{@code updatedAt}: 갱신시각·기준 ({@code String}).
   *   <li>{@code revision}: 결과를 갱신할 때 비교하는 리비전 ({@code long}).
   *   <li>{@code downloads}: downloads ({@code long}).
   *   <li>{@code recommendations}: 추천목록 ({@code long}).
   * </ul>
   */
  record Summary(
      UUID id,
      UUID authorId,
      String author,
      String gameVersion,
      String baseId,
      String title,
      int steps,
      String createdAt,
      String updatedAt,
      long revision,
      long downloads,
      long recommendations) {}

  /**
   * {@code SessionBoard}에서 사용하는 작성자의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code id}: 이 값의 식별자 ({@code UUID}).
   *   <li>{@code name}: 표시 또는 조회에 사용하는 이름 ({@code String}).
   * </ul>
   */
  record Author(UUID id, String name) {}

  /**
   * {@code SessionBoard}에서 사용하는 페이지의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code posts}: posts ({@code List<Summary>}).
   *   <li>{@code total}: 합계 ({@code long}).
   *   <li>{@code page}: 페이지 ({@code int}).
   *   <li>{@code pageSize}: 페이지·크기 ({@code int}).
   *   <li>{@code author}: 작성자 ({@code Author}).
   * </ul>
   */
  record Page(List<Summary> posts, long total, int page, int pageSize, Author author) {}

  Post create(Actor actor, Draft draft);

  Post update(Actor actor, UUID id, Draft draft);

  Post editText(Actor actor, UUID id, String title, String body, long revision);

  void delete(Actor actor, UUID id, String password, long revision);

  Post get(UUID id);

  default Page list(String base, String version, UUID author, int page) {
    return list(base, version, author, page, "newest");
  }

  default Page list(String base, String version, UUID author, int page, String sort) {
    return list(base, version, author, page, sort, null);
  }

  Page list(String base, String version, UUID author, int page, String sort, String itemClass);

  List<Summary> mine(Actor actor, String source);

  Map<String, Object> options();
}
