package com.poe2craft.crafting.application;

import java.util.*;

public interface AdditionPoolStore {
  Optional<AdditionPoolCache.Data> find(String namespace, String key);

  void save(String namespace, String key, AdditionPoolCache.Data data);
}
