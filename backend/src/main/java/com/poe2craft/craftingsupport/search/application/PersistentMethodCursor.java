package com.poe2craft.craftingsupport.search.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.poe2craft.craftingsupport.search.domain.CertifiedInterval;
import com.poe2craft.craftingsupport.search.domain.CertifiedMethodRunner;
import com.poe2craft.craftingsupport.search.domain.ExactMethodRunner.*;
import java.util.*;
import java.util.function.*;

/**
 * 완전히 기록된 불변 커서만 그래프 리비전에 연결한다.
 *
 * <p>기존 계약: Incremental cursor I/O; a graph revision publishes only a fully written immutable
 * cursor.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code store}: 저장소 ({@code MethodGraphStore}).
 *   <li>{@code json}: 이 경계의 JSON 읽기·쓰기 변환기 ({@code ObjectMapper}).
 *   <li>{@code headerType}: 헤더·타입 ({@code JavaType}).
 *   <li>{@code rowType}: 행·타입 ({@code JavaType}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code PersistentMethodCursor(store, json, state, action)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
 *       store}(저장소), {@code json}(JSON), {@code state}(상태), {@code action}(행동).
 *   <li>{@code encode(value)}: 값을 저장·전송 표현으로 부호화한다. 입력: {@code value}(값). 반환: {@code String}. 사용하는
 *       예외: {@code IllegalArgumentException}.
 *   <li>{@code decode(content, type)}: 저장·전송 표현을 내부 값으로 복원한다. 입력: {@code content}(내용), {@code
 *       type}(타입). 반환: {@code T}. 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code write(scope, runner, revision, changes)}: 쓰기 처리에서 {@code runner.leaseCursor}을(를)
 *       사용한다. 입력: {@code scope}(범위), {@code runner}(실행기), {@code revision}(리비전), {@code
 *       changes}(changes). 반환: {@code Writer}.
 *   <li>{@code write(scope, lease, revision, changes)}: 쓰기의 입력에 따른 분기·결과를 처리한다. 입력: {@code
 *       scope}(범위), {@code lease}(사용권), {@code revision}(리비전), {@code changes}(changes). 반환: {@code
 *       Writer}.
 *   <li>{@code read(scope, template, transition, classifier, precision, absorptionMaps)}: 지정한 입력 또는
 *       저장된 값을 읽는다. 입력: {@code scope}(범위), {@code template}(템플릿), {@code transition}(전이), {@code
 *       classifier}(classifier), {@code precision}(정밀도), {@code absorptionMaps}(흡수·맵목록). 반환: {@code
 *       Reader<CertifiedMethodRunner<S, A>>}.
 *   <li>{@code read(scope, factory)}: 지정한 입력 또는 저장된 값을 읽는다. 입력: {@code scope}(범위), {@code
 *       factory}(생성기). 반환: {@code Reader<R>}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Writer}: {@code PersistentMethodCursor} 내부의 쓰기변환기 처리를 맡는다.
 *   <li>{@code RestoreTarget}: {@code PersistentMethodCursor}에서 사용하는 복원·대상의 불변 값이다.
 *   <li>{@code Reader}: {@code PersistentMethodCursor} 내부의 읽기변환기 처리를 맡는다.
 * </ul>
 */
public final class PersistentMethodCursor<S, A> {
  private final MethodGraphStore store;
  private final ObjectMapper json;
  private final JavaType headerType, rowType;

  public PersistentMethodCursor(
      MethodGraphStore store, ObjectMapper json, Class<S> state, Class<A> action) {
    this.store = Objects.requireNonNull(store);
    this.json =
        json.copy()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
    headerType =
        json.getTypeFactory()
            .constructParametricType(CertifiedMethodRunner.CursorHeader.class, state);
    rowType =
        json.getTypeFactory()
            .constructParametricType(CertifiedMethodRunner.CursorRow.class, state, action);
  }

  private String encode(Object value) {
    try {
      return json.writeValueAsString(value);
    } catch (JsonProcessingException error) {
      throw new IllegalArgumentException("Cannot encode method cursor", error);
    }
  }

  private <T> T decode(String content, JavaType type) {
    try {
      return json.readValue(content, type);
    } catch (JsonProcessingException error) {
      throw new IllegalArgumentException("Invalid stored method cursor", error);
    }
  }

  public Writer write(
      String scope,
      CertifiedMethodRunner<S, A> runner,
      long revision,
      MethodGraphStore.Batch changes) {
    return write(scope, runner.leaseCursor(), revision, changes);
  }

  public Writer write(
      String scope,
      CertifiedMethodRunner<S, A>.CursorLease lease,
      long revision,
      MethodGraphStore.Batch changes) {
    return new Writer(scope, lease, revision, changes);
  }

  /**
   * {@code PersistentMethodCursor} 내부의 쓰기변환기 처리를 맡는다.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code lease}: 사용권 ({@code CertifiedMethodRunner<S, A>.CursorLease}).
   *   <li>{@code rows}: 행목록 ({@code Iterator<CertifiedMethodRunner.CursorRow<S, A>>}).
   *   <li>{@code draft}: 초안 ({@code MethodCursorStore.ExecutionCursor}).
   *   <li>{@code changes}: changes ({@code MethodGraphStore.Batch}).
   *   <li>{@code expectedRevision}: 기대값·리비전 ({@code long}).
   *   <li>{@code previous}: 이전 ({@code Optional<MethodCursorStore.ExecutionCursor>}).
   *   <li>{@code written}: written ({@code long}).
   *   <li>{@code publishedRevision}: 공개된·리비전 ({@code long}).
   *   <li>{@code complete}: 완료 ({@code boolean}).
   *   <li>{@code closed}: 닫힘 ({@code boolean}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Writer(scope, lease, revision, changes)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
   *       scope}(범위), {@code lease}(사용권), {@code revision}(리비전), {@code changes}(changes).
   *   <li>{@code complete()}: {@code complete} 값을 조회한다. 반환: {@code boolean}.
   *   <li>{@code writtenRows()}: {@code written} 값을 조회한다. 반환: {@code long}.
   *   <li>{@code step(allowance)}: 계산 또는 실행을 한 단계 진행한다. 입력: {@code allowance}(allowance). 사용하는 예외:
   *       {@code IllegalStateException}, {@code IllegalArgumentException}.
   *   <li>{@code revision()}: 리비전 값을 조회·구성한다. 반환: {@code long}. 사용하는 예외: {@code
   *       IllegalStateException}.
   *   <li>{@code discardPrevious()}: discard·이전 처리에서 {@code previous.ifPresent}, {@code
   *       store.discardExecution}을(를) 사용한다. 사용하는 예외: {@code IllegalStateException}.
   *   <li>{@code close()}: 보유한 자원과 작업 상태를 종료·해제한다.
   * </ul>
   */
  public final class Writer implements AutoCloseable {
    private final CertifiedMethodRunner<S, A>.CursorLease lease;
    private final Iterator<CertifiedMethodRunner.CursorRow<S, A>> rows;
    private final MethodCursorStore.ExecutionCursor draft;
    private final MethodGraphStore.Batch changes;
    private final long expectedRevision;
    private final Optional<MethodCursorStore.ExecutionCursor> previous;
    private long written, publishedRevision;
    private boolean complete, closed;

    private Writer(
        String scope,
        CertifiedMethodRunner<S, A>.CursorLease lease,
        long revision,
        MethodGraphStore.Batch changes) {
      expectedRevision = revision;
      this.changes = Objects.requireNonNull(changes);
      this.lease = Objects.requireNonNull(lease);
      try {
        previous = store.execution(scope);
        draft = store.beginExecution(scope, encode(lease.header()));
        rows = lease.rows();
      } catch (RuntimeException | Error failure) {
        lease.close();
        throw failure;
      }
    }

    public boolean complete() {
      return complete;
    }

    public long writtenRows() {
      return written;
    }

    public void step(int allowance) {
      if (closed) throw new IllegalStateException("Cursor writer closed");
      if (complete) return;
      if (allowance < 1 || allowance > 256)
        throw new IllegalArgumentException("Cursor row allowance must be 1..256");
      try {
        var batch = new ArrayList<String>();
        while (batch.size() < allowance && rows.hasNext()) batch.add(encode(rows.next()));
        if (!batch.isEmpty()) written = store.appendExecution(draft.id(), written, batch);
        if (rows.hasNext()) return;
        var sealed = store.sealExecution(draft.id(), written);
        var checkpoints = new ArrayList<>(changes.checkpoints());
        checkpoints.add(MethodCursorStore.reference(sealed));
        publishedRevision =
            store.commit(
                expectedRevision,
                new MethodGraphStore.Batch(
                    changes.sources(), changes.methods(), changes.observations(), checkpoints));
        complete = true;
        lease.close();
      } catch (RuntimeException | Error failure) {
        try {
          close();
        } catch (RuntimeException | Error cleanup) {
          failure.addSuppressed(cleanup);
        }
        throw failure;
      }
    }

    public long revision() {
      if (!complete) throw new IllegalStateException("Cursor not published");
      return publishedRevision;
    }

    /** Call after publication while holding the job lease; referenced cursors cannot be deleted. */
    public void discardPrevious() {
      if (!complete) throw new IllegalStateException("Cursor not published");
      previous.ifPresent(old -> store.discardExecution(old.id()));
    }

    public void close() {
      if (closed) return;
      closed = true;
      lease.close();
      if (!complete) store.discardExecution(draft.id());
    }
  }

  public Reader<CertifiedMethodRunner<S, A>> read(
      String scope,
      Template<S, A> template,
      Transition<S, A> transition,
      Function<S, Optional<Exit>> classifier,
      CertifiedInterval.Precision precision,
      Supplier<? extends Map<S, CertifiedInterval>> absorptionMaps) {
    return read(
        scope,
        header -> {
          var target =
              CertifiedMethodRunner.restoreCursor(
                  header, template, transition, classifier, precision, absorptionMaps);
          return new RestoreTarget<>(
              target::accept, target::finish, target::close, target::retainedStates);
        });
  }

  /**
   * {@code PersistentMethodCursor}에서 사용하는 복원·대상의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code accept}: 수락 ({@code Consumer<CertifiedMethodRunner.CursorRow<S, A>>}).
   *   <li>{@code finish}: 완료 ({@code Supplier<R>}).
   *   <li>{@code cancel}: cancel ({@code Runnable}).
   *   <li>{@code retainedStates}: 보유·상태목록 ({@code LongSupplier}).
   * </ul>
   */
  public record RestoreTarget<S, A, R>(
      Consumer<CertifiedMethodRunner.CursorRow<S, A>> accept,
      Supplier<R> finish,
      Runnable cancel,
      LongSupplier retainedStates) {}

  public <R> Reader<R> read(
      String scope,
      Function<CertifiedMethodRunner.CursorHeader<S>, RestoreTarget<S, A, R>> factory) {
    var cursor =
        store
            .execution(scope)
            .orElseThrow(() -> new IllegalArgumentException("Execution checkpoint not found"));
    CertifiedMethodRunner.CursorHeader<S> header = decode(cursor.header(), headerType);
    return new Reader<>(cursor, factory.apply(header));
  }

  /**
   * {@code PersistentMethodCursor} 내부의 읽기변환기 처리를 맡는다.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code cursor}: 진행커서 ({@code MethodCursorStore.ExecutionCursor}).
   *   <li>{@code restoration}: 복원 ({@code RestoreTarget<S, A, R>}).
   *   <li>{@code result}: 결과 ({@code R}).
   *   <li>{@code offset}: 시작위치 ({@code long}).
   *   <li>{@code failed}: failed ({@code boolean}).
   *   <li>{@code closed}: 닫힘 ({@code boolean}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Reader(cursor, restoration)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code cursor}(진행커서),
   *       {@code restoration}(복원).
   *   <li>{@code complete()}: 완료 값을 조회·구성한다. 반환: {@code boolean}.
   *   <li>{@code readRows()}: {@code offset} 값을 조회한다. 반환: {@code long}.
   *   <li>{@code retainedStates()}: 보유·상태목록 값을 조회·구성한다. 반환: {@code long}.
   *   <li>{@code step(allowance)}: 계산 또는 실행을 한 단계 진행한다. 입력: {@code allowance}(allowance). 사용하는 예외:
   *       {@code IllegalStateException}.
   *   <li>{@code result()}: 최종 또는 현재 계산 결과를 구성한다. 반환: {@code R}. 사용하는 예외: {@code
   *       IllegalStateException}.
   *   <li>{@code close()}: 보유한 자원과 작업 상태를 종료·해제한다.
   * </ul>
   */
  public final class Reader<R> implements AutoCloseable {
    private final MethodCursorStore.ExecutionCursor cursor;
    private final RestoreTarget<S, A, R> restoration;
    private R result;
    private long offset;
    private boolean failed, closed;

    private Reader(MethodCursorStore.ExecutionCursor cursor, RestoreTarget<S, A, R> restoration) {
      this.cursor = cursor;
      this.restoration = restoration;
    }

    public boolean complete() {
      return result != null;
    }

    public long readRows() {
      return offset;
    }

    public long retainedStates() {
      return restoration.retainedStates().getAsLong();
    }

    public void step(int allowance) {
      if (failed) throw new IllegalStateException("Cursor restoration failed");
      if (closed) throw new IllegalStateException("Cursor reader closed");
      if (complete()) return;
      try {
        var page = store.executionRows(cursor.id(), offset, allowance);
        for (var row : page.rows()) {
          restoration.accept().accept(decode(row.content(), rowType));
          offset++;
        }
        if (page.complete()) result = Objects.requireNonNull(restoration.finish().get());
      } catch (RuntimeException | Error failure) {
        failed = true;
        try {
          close();
        } catch (RuntimeException | Error cleanup) {
          failure.addSuppressed(cleanup);
        }
        throw failure;
      }
    }

    public R result() {
      if (!complete()) throw new IllegalStateException("Cursor restoration incomplete");
      return result;
    }

    @Override
    public void close() {
      if (closed) return;
      closed = true;
      if (!complete()) restoration.cancel().run();
    }
  }
}
