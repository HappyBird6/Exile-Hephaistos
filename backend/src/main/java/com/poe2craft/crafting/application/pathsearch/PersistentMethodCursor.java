package com.poe2craft.crafting.application.pathsearch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.poe2craft.crafting.domain.pathsearch.*;
import com.poe2craft.crafting.domain.pathsearch.ExactMethodRunner.*;
import java.util.*;
import java.util.function.*;

/** Incremental cursor I/O; a graph revision publishes only a fully written immutable cursor. */
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
