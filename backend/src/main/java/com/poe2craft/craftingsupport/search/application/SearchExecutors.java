package com.poe2craft.craftingsupport.search.application;

import java.util.concurrent.*;

/** One worker per legacy search service; job limits also bound queued continuations. */
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
