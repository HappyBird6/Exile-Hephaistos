package com.poe2craft.craftingsupport.search.application;

import java.util.concurrent.*;

/**
 * 기존 탐색 서비스에 작업 수가 제한된 단일 실행기를 제공한다.
 *
 * <p>기존 계약: One worker per legacy search service; job limits also bound queued continuations.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code SearchExecutors()}: 외부 인스턴스 생성이 필요 없는 유틸리티 생성자다.
 *   <li>{@code singleWorker(name, capacity)}: 단일·작업자 처리에서 {@code thread.setDaemon}을(를) 사용한다. 입력:
 *       {@code name}(이름), {@code capacity}(용량). 반환: {@code ExecutorService}.
 * </ul>
 */
final class SearchExecutors {
  private SearchExecutors() {}

  static ExecutorService singleWorker(String name, int capacity) {
    return new ThreadPoolExecutor(
        1,
        1,
        0,
        TimeUnit.MILLISECONDS,
        new ArrayBlockingQueue<>(capacity),
        task -> {
          var thread = new Thread(task, name);
          thread.setDaemon(true);
          return thread;
        });
  }
}
