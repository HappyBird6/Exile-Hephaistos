package com.poe2craft.craftingsupport.recommendation.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.craftingsupport.recommendation.application.AdditionPoolCache;
import com.poe2craft.craftingsupport.recommendation.application.AdditionPoolStore;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 옵션 추가 풀 캐시를 JDBC 저장소에 기록하고 조회한다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code jdbc}: JDBC ({@code JdbcTemplate}).
 *   <li>{@code json}: 이 경계의 JSON 읽기·쓰기 변환기 ({@code ObjectMapper}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code JdbcAdditionPoolStore(jdbc, json)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code jdbc}(JDBC),
 *       {@code json}(JSON).
 *   <li>{@code find(namespace, key)}: 조건에 맞는 항목을 찾는다. 입력: {@code namespace}(이름공간), {@code key}(키).
 *       반환: {@code Optional<AdditionPoolCache.Data>}. 사용하는 예외: {@code IllegalStateException}.
 *   <li>{@code save(namespace, key, data)}: 현재 값을 저장 경계에 기록한다. 입력: {@code namespace}(이름공간), {@code
 *       key}(키), {@code data}(데이터). 사용하는 예외: {@code IllegalStateException}.
 * </ul>
 */
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
