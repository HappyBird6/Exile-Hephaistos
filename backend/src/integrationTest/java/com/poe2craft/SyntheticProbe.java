package com.poe2craft;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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
