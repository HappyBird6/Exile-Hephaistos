package com.poe2craft.crafting.domain.goalfilter;

import com.poe2craft.item.ItemState.Rarity;
import java.math.BigDecimal;
import java.util.List;

/** Versioned wire AST. Disabled content is preserved and validated, never normalized. */
public record GoalFilter(
    Integer version, String catalogVersion, General general, List<Group> groups) {
  public record Context(String snapshotId, String baseItemId, Integer itemLevel) {}

  public record General(String baseItemId, Range itemLevel, List<Rarity> rarities) {}

  public record Range(BigDecimal min, BigDecimal max) {
    public boolean contains(BigDecimal value) {
      return (min == null || value.compareTo(min) >= 0)
          && (max == null || value.compareTo(max) <= 0);
    }
  }

  public record Group(String id, Type type, Boolean disabled, Range range, List<Entry> entries) {}

  public record Entry(
      String id, String statId, String unit, Range range, BigDecimal weight, Boolean disabled) {}

  public enum Type {
    AND,
    NOT,
    IF,
    COUNT,
    WEIGHTED_V1,
    WEIGHTED_V2
  }

  public enum Status {
    MATCH,
    NO_MATCH,
    UNKNOWN,
    UNSUPPORTED
  }

  public enum Capability {
    SUPPORTED,
    UNKNOWN,
    UNSUPPORTED
  }

  public enum Presence {
    PRESENT,
    ABSENT,
    UNKNOWN
  }

  public record Issue(String code, String path, String message, Severity severity) {
    public static Issue error(String code, String path, String message) {
      return new Issue(code, path, message, Severity.ERROR);
    }

    public static Issue warning(String code, String path, String message) {
      return new Issue(code, path, message, Severity.WARNING);
    }
  }

  public enum Severity {
    ERROR,
    WARNING
  }
}
