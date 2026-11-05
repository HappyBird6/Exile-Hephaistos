package com.poe2craft.craftingsupport.search.application;

import java.util.List;

/** Local durable job files; implementation owns file naming and atomic descriptor publication. */
public interface CertifiedJobRepository {
  record Descriptor(String id, MethodGraphStore.Identity identity) {}

  MethodGraphStore create(Descriptor descriptor);

  MethodGraphStore open(Descriptor descriptor);

  List<Descriptor> list();
}
