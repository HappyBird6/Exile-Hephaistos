package com.poe2craft.season.application.service;

public final class CrawlException extends RuntimeException {
  private final int status;

  public CrawlException(int status, String code) {
    super(code);
    this.status = status;
  }

  public int status() {
    return status;
  }
}
