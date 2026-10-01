package com.poe2craft.crafting.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.crafting.application.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;

public final class JdbcAdditionPoolStore implements AdditionPoolStore {
  private final JdbcTemplate jdbc;
  private final ObjectMapper json;

  public JdbcAdditionPoolStore(JdbcTemplate jdbc, ObjectMapper json) {
    this.jdbc = jdbc;
    this.json = json;
  }

  public Optional<AdditionPoolCache.Data> find(String namespace, String key) {
    var rows =
        jdbc.query(
            "select payload::text from crafting.support_addition_pool where namespace=? and pool_key=?",
            (rs, row) -> rs.getString(1),
            namespace,
            key);
    if (rows.isEmpty()) return Optional.empty();
    try {
      return Optional.of(json.readValue(rows.getFirst(), AdditionPoolCache.Data.class));
    } catch (Exception e) {
      throw new IllegalStateException("Stored addition pool could not be verified", e);
    }
  }

  public void save(String namespace, String key, AdditionPoolCache.Data data) {
    try {
      jdbc.update(
          "insert into crafting.support_addition_pool(namespace,pool_key,payload) values (?,?,?::jsonb) on conflict do nothing",
          namespace,
          key,
          json.writeValueAsString(data));
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new IllegalStateException("Could not encode addition pool", e);
    }
  }
}
