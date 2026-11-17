package com.poe2craft;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Synthetic·Probe 동작을 고정 입력·기대 결과로 검증한다. 각 테스트가 공유하는 준비 값과 시나리오는 아래와 같다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code id}: 이 값의 식별자 ({@code Long}).
 *   <li>{@code label}: 표시이름 ({@code String}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code SyntheticProbe()}: 기본 생성자다.
 *   <li>{@code SyntheticProbe(id, label)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code id}(식별자), {@code
 *       label}(표시이름).
 * </ul>
 */
@Entity
@Table(name = "synthetic_probe")
class SyntheticProbe {
  @Id private Long id;
  private String label;

  protected SyntheticProbe() {}

  SyntheticProbe(long id, String label) {
    this.id = id;
    this.label = label;
  }
}
