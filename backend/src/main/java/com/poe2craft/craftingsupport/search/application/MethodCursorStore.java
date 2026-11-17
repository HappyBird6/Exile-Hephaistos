package com.poe2craft.craftingsupport.search.application;

import java.util.*;
import java.util.function.Consumer;

/**
 * 원자적으로 공개된 그래프 체크포인트에 연결되는 실행 커서를 저장한다.
 *
 * <p>기존 계약: Staged execution rows become visible only through an atomic graph checkpoint
 * publication.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code CHECKPOINT_PREFIX}: 접두; 초기값 {@code "execution/"} ({@code String}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code beginExecution(scope, header)}: 시작·실행의 실행 계약을 정의한다. 입력: {@code scope}(범위), {@code
 *       header}(헤더). 반환: {@code ExecutionCursor}.
 *   <li>{@code appendExecution(id, expectedRows, rows)}: append·실행의 실행 계약을 정의한다. 입력: {@code
 *       id}(식별자), {@code expectedRows}(기대값·행목록), {@code rows}(행목록). 반환: {@code long}.
 *   <li>{@code sealExecution(id, expectedRows)}: 봉인·실행의 실행 계약을 정의한다. 입력: {@code id}(식별자), {@code
 *       expectedRows}(기대값·행목록). 반환: {@code ExecutionCursor}.
 *   <li>{@code captureExecution(scope, header, producer)}: capture·실행 처리에서 {@code producer.accept},
 *       {@code batch.add}을(를) 사용한다. 입력: {@code scope}(범위), {@code header}(헤더), {@code
 *       producer}(producer). 반환: {@code ExecutionCursor}.
 *   <li>{@code execution(scope)}: 실행의 실행 계약을 정의한다. 입력: {@code scope}(범위). 반환: {@code
 *       Optional<ExecutionCursor>}.
 *   <li>{@code executionRows(id, offset, limit)}: 실행·행목록의 실행 계약을 정의한다. 입력: {@code id}(식별자), {@code
 *       offset}(시작위치), {@code limit}(한도). 반환: {@code ExecutionPage}.
 *   <li>{@code discardExecution(id)}: discard·실행의 실행 계약을 정의한다. 입력: {@code id}(식별자).
 *   <li>{@code reference(cursor)}: 참조 처리에서 {@code cursor.sealed}, {@code
 *       MethodGraphStore.Checkpoint}을(를) 사용한다. 입력: {@code cursor}(진행커서). 반환: {@code
 *       MethodGraphStore.Checkpoint}. 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code finished(scope)}: 종료 처리에서 {@code scope.isBlank}, {@code
 *       MethodGraphStore.Checkpoint}을(를) 사용한다. 입력: {@code scope}(범위). 반환: {@code
 *       MethodGraphStore.Checkpoint}. 사용하는 예외: {@code IllegalArgumentException}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code ExecutionCursor}: {@code MethodCursorStore}에서 사용하는 실행·진행커서의 불변 값이다.
 *   <li>{@code ExecutionRow}: {@code MethodCursorStore}에서 사용하는 실행·행의 불변 값이다.
 *   <li>{@code ExecutionPage}: {@code MethodCursorStore}에서 사용하는 실행·페이지의 불변 값이다.
 * </ul>
 */
public interface MethodCursorStore {
  String CHECKPOINT_PREFIX = "execution/";

  /**
   * {@code MethodCursorStore}에서 사용하는 실행·진행커서의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code scope}: 범위 ({@code String}).
   *   <li>{@code header}: 헤더 ({@code String}).
   *   <li>{@code rows}: 행목록 ({@code long}).
   *   <li>{@code sealed}: sealed ({@code boolean}).
   * </ul>
   */
  record ExecutionCursor(String id, String scope, String header, long rows, boolean sealed) {}

  /**
   * {@code MethodCursorStore}에서 사용하는 실행·행의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code ordinal}: ordinal ({@code long}).
   *   <li>{@code content}: 내용 ({@code String}).
   * </ul>
   */
  record ExecutionRow(long ordinal, String content) {}

  /**
   * {@code MethodCursorStore}에서 사용하는 실행·페이지의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code rows}: 행목록 ({@code List<ExecutionRow>}).
   *   <li>{@code complete}: 완료 ({@code boolean}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code ExecutionPage(rows, complete)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code rows}(행목록), {@code
   *       complete}(완료).
   * </ul>
   */
  record ExecutionPage(List<ExecutionRow> rows, boolean complete) {
    public ExecutionPage {
      rows = List.copyOf(rows);
    }
  }

  ExecutionCursor beginExecution(String scope, String header);

  long appendExecution(String id, long expectedRows, List<String> rows);

  ExecutionCursor sealExecution(String id, long expectedRows);

  /** Synchronous bounded-memory capture. The returned sealed cursor is not yet published. */
  default ExecutionCursor captureExecution(
      String scope, String header, Consumer<Consumer<String>> producer) {
    var draft = beginExecution(scope, header);
    long[] offset = {0};
    var batch = new ArrayList<String>();
    try {
      producer.accept(
          row -> {
            batch.add(row);
            if (batch.size() == 128) {
              offset[0] = appendExecution(draft.id(), offset[0], batch);
              batch.clear();
            }
          });
      if (!batch.isEmpty()) offset[0] = appendExecution(draft.id(), offset[0], batch);
      return sealExecution(draft.id(), offset[0]);
    } catch (RuntimeException | Error failure) {
      try {
        discardExecution(draft.id());
      } catch (RuntimeException cleanup) {
        failure.addSuppressed(cleanup);
      }
      throw failure;
    }
  }

  Optional<ExecutionCursor> execution(String scope);

  ExecutionPage executionRows(String id, long offset, int limit);

  /** Rejects deletion while any committed execution checkpoint still references this cursor. */
  void discardExecution(String id);

  static MethodGraphStore.Checkpoint reference(ExecutionCursor cursor) {
    if (!cursor.sealed()) throw new IllegalArgumentException("Execution must be sealed");
    return new MethodGraphStore.Checkpoint(CHECKPOINT_PREFIX + cursor.scope(), cursor.id());
  }

  /** Publish with the terminal observation; reclaim the now-unreferenced execution afterwards. */
  static MethodGraphStore.Checkpoint finished(String scope) {
    if (scope == null || scope.isBlank())
      throw new IllegalArgumentException("Execution scope required");
    return new MethodGraphStore.Checkpoint(CHECKPOINT_PREFIX + scope, "");
  }
}
