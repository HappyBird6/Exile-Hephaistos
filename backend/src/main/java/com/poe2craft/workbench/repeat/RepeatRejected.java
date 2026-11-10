package com.poe2craft.workbench.repeat;

/**
 * Safe, fixed public errors. Never construct these details from an engine exception or item text.
 */
public final class RepeatRejected extends IllegalArgumentException {
  private final int status;
  private final String code;

  public RepeatRejected(int status, String code, String detail) {
    super(detail);
    this.status = status;
    this.code = code;
  }

  public int status() {
    return status;
  }

  public String code() {
    return code;
  }

  public static RepeatRejected input() {
    return new RepeatRejected(
        422,
        "INVALID_CHAOS_REPEAT_REQUEST",
        "Provide a canonical supported item, UUID requestId, Chaos action, known unique omens, one"
            + " of the eight site locales, and an integer maximumUses from 1 to 1000 (default 100).");
  }
}
