package com.poe2craft.crafting.application.pathsearch;

import java.util.*;

/** Durable calculation catalog; absence or pending work is never a zero-probability result. */
public interface MethodGraphStore extends AutoCloseable {
  String VERSION = "method-graph-store-v1";

  record Identity(
      String data,
      String rules,
      String rolls,
      String templates,
      String goals,
      String arithmetic,
      String season,
      String originalInput) {
    public Identity {
      for (var value :
          List.of(data, rules, rolls, templates, goals, arithmetic, season, originalInput))
        if (value.isBlank()) throw new IllegalArgumentException("Complete store identity required");
    }
  }

  record Source(long id, int depth, boolean discovered, String content) {
    public Source {
      if (id < 0 || depth < 0 || content == null || content.isBlank())
        throw new IllegalArgumentException("Invalid source");
    }
  }

  record Method(String id, long source, String program) {
    public Method {
      if (id == null || id.isBlank() || source < 0 || program == null || program.isBlank())
        throw new IllegalArgumentException("Invalid method");
    }
  }

  enum Status {
    REQUESTED,
    PARTIAL,
    READY,
    UNSUPPORTED
  }

  record Observation(String method, int cycles, Status status, Long operator, String summary) {
    public Observation {
      if (method == null
          || method.isBlank()
          || !List.of(100, 300, 500).contains(cycles)
          || status == null) throw new IllegalArgumentException("Invalid observation");
      if ((status == Status.READY || status == Status.UNSUPPORTED)
          && (operator == null || summary == null))
        throw new IllegalArgumentException(
            "A terminal observation retains its operator and summary");
    }
  }

  record Checkpoint(String name, String content) {
    public Checkpoint {
      if (name == null || name.isBlank() || content == null)
        throw new IllegalArgumentException("Invalid checkpoint");
    }
  }

  record Batch(
      List<Source> sources,
      List<Method> methods,
      List<Observation> observations,
      List<Checkpoint> checkpoints) {
    public Batch {
      sources = List.copyOf(sources);
      methods = List.copyOf(methods);
      observations = List.copyOf(observations);
      checkpoints = List.copyOf(checkpoints);
    }
  }

  record MethodRow(long ordinal, int depth, Method method) {}

  record Cursor(int depth, long ordinal) {}

  record Page(long revision, List<MethodRow> methods, Cursor next) {}

  record Metrics(
      long sources,
      long methods,
      long operators,
      long observations,
      long ready,
      long diskBytes,
      long reads,
      long writes,
      int cacheKiB) {}

  long revision();

  long commit(long expectedRevision, Batch batch);

  Optional<Source> source(long id);

  Optional<Method> method(String id);

  Page methods(long expectedRevision, Cursor after, int limit);

  Optional<Observation> observation(String method, int cycles);

  Optional<Checkpoint> checkpoint(String name);

  /** Digest equality only selects candidates; actual canonical content must also match. */
  long internOperator(String digest, String canonicalContent);

  String operator(long id);

  Metrics metrics();

  @Override
  void close();
}
