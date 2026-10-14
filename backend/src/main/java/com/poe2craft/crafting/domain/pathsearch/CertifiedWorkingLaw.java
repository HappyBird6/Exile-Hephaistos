package com.poe2craft.crafting.domain.pathsearch;

import java.util.Map;

/** Ordered mutable probability law whose payload may live outside the Java heap. */
public interface CertifiedWorkingLaw<K> extends Map<K, CertifiedInterval>, AutoCloseable {
  Map.Entry<K, CertifiedInterval> firstEntry();

  /** Resident entries owned by this map; shared caches are accounted by its store. */
  long residentEntries();

  @Override
  void close();
}
