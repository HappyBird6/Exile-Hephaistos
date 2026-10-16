package com.poe2craft.crafting.application.pathsearch;

import java.util.List;

/** Atomic bounded job documents; the original goal and incomplete cursor are retained. */
public interface AutomaticJobRepository {
  final class BudgetExceeded extends IllegalArgumentException {
    public BudgetExceeded() {
      super("Job document exceeds storage budget");
    }
  }

  List<String> ids();

  byte[] read(String id);

  void save(String id, byte[] value);

  void delete(String id);
}
