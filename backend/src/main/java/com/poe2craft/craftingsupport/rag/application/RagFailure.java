package com.poe2craft.craftingsupport.rag.application;

/** Fixed safe codes only; provider bodies, prompts and credentials must never become API errors. */
public final class RagFailure extends RuntimeException {
  private final int status;

  public RagFailure(int status, String code) {
    super(code);
    this.status = status;
  }

  public int status() {
    return status;
  }
}
