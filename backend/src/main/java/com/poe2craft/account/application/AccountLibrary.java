package com.poe2craft.account.application;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.UUID;

/**
 * 인증된 사용자의 개인 제작 세션 보관함 계약을 정의한다.
 *
 * <p>기존 계약: Owner comes from server authentication, never from an uploaded document.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code readBatchSize()}: 일괄실행·크기을(를) 읽는다. 반환: {@code int}.
 *   <li>{@code readBatch(owner, generation, references)}: 일괄실행을(를) 읽는다. 입력: {@code owner}(소유자),
 *       {@code generation}(세대번호), {@code references}(참조목록). 반환: {@code List<Document>}.
 *   <li>{@code snapshot(owner)}: 현재 내용을 불변 관측값으로 구성한다. 입력: {@code owner}(소유자). 반환: {@code
 *       Snapshot}.
 *   <li>{@code clearSessions(owner)}: clear·세션목록의 실행 계약을 정의한다. 입력: {@code owner}(소유자). 반환: {@code
 *       long}.
 *   <li>{@code saveSession(owner, id, expectedVersion, generation, content)}: 세션을(를) 저장한다. 입력:
 *       {@code owner}(소유자), {@code id}(식별자), {@code expectedVersion}(기대값·버전), {@code
 *       generation}(세대번호), {@code content}(내용). 반환: {@code Document}.
 *   <li>{@code index(owner)}: 색인의 실행 계약을 정의한다. 입력: {@code owner}(소유자). 반환: {@code List<Reference>}.
 *   <li>{@code read(owner, kind, id)}: 지정한 입력 또는 저장된 값을 읽는다. 입력: {@code owner}(소유자), {@code
 *       kind}(종류), {@code id}(식별자). 반환: {@code Document}.
 *   <li>{@code signIn(subject, name)}: 사용자 인증을 수행한다. 입력: {@code subject}(subject), {@code
 *       name}(이름). 반환: {@code Account}.
 *   <li>{@code list(owner)}: 조건에 맞는 목록을 조회한다. 입력: {@code owner}(소유자). 반환: {@code List<Document>}.
 *   <li>{@code save(owner, kind, id, expectedVersion, content)}: 현재 값을 저장 경계에 기록한다. 입력: {@code
 *       owner}(소유자), {@code kind}(종류), {@code id}(식별자), {@code expectedVersion}(기대값·버전), {@code
 *       content}(내용). 반환: {@code Document}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Account}: {@code AccountLibrary}에서 사용하는 계정의 불변 값이다.
 *   <li>{@code Document}: {@code AccountLibrary}에서 사용하는 문서의 불변 값이다.
 *   <li>{@code Reference}: {@code AccountLibrary}에서 사용하는 참조의 불변 값이다.
 *   <li>{@code Snapshot}: {@code AccountLibrary}에서 사용하는 스냅샷의 불변 값이다.
 *   <li>{@code ReadKey}: {@code AccountLibrary}에서 사용하는 읽기·키의 불변 값이다.
 * </ul>
 */
public interface AccountLibrary {
  /**
   * {@code AccountLibrary}에서 사용하는 계정의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code id}: 이 값의 식별자 ({@code UUID}).
   *   <li>{@code displayName}: 표시·이름 ({@code String}).
   * </ul>
   */
  record Account(UUID id, String displayName) {}

  /**
   * {@code AccountLibrary}에서 사용하는 문서의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code kind}: 종류 ({@code String}).
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code version}: 저장·실행 계약의 버전 ({@code long}).
   *   <li>{@code content}: 내용 ({@code JsonNode}).
   * </ul>
   */
  record Document(String kind, String id, long version, JsonNode content) {}

  /**
   * {@code AccountLibrary}에서 사용하는 참조의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code kind}: 종류 ({@code String}).
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code version}: 저장·실행 계약의 버전 ({@code long}).
   *   <li>{@code deleted}: deleted ({@code boolean}).
   * </ul>
   */
  record Reference(String kind, String id, long version, boolean deleted) {}

  /**
   * {@code AccountLibrary}에서 사용하는 스냅샷의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code generation}: 늦은 작업 결과를 구분하는 세대 번호 ({@code long}).
   *   <li>{@code documents}: 문서목록 ({@code List<Reference>}).
   * </ul>
   */
  record Snapshot(long generation, List<Reference> documents) {}

  /**
   * {@code AccountLibrary}에서 사용하는 읽기·키의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code kind}: 종류 ({@code String}).
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code version}: 저장·실행 계약의 버전 ({@code long}).
   * </ul>
   */
  record ReadKey(String kind, String id, long version) {}

  int readBatchSize();

  List<Document> readBatch(UUID owner, long generation, List<ReadKey> references);

  Snapshot snapshot(UUID owner);

  long clearSessions(UUID owner);

  Document saveSession(
      UUID owner, String id, long expectedVersion, long generation, JsonNode content);

  List<Reference> index(UUID owner);

  Document read(UUID owner, String kind, String id);

  Account signIn(String subject, String name);

  List<Document> list(UUID owner);

  Document save(UUID owner, String kind, String id, long expectedVersion, JsonNode content);
}
