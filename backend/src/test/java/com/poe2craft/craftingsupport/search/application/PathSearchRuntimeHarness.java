package com.poe2craft.craftingsupport.search.application;

import com.poe2craft.craftingsupport.search.api.PathSearchController;
import com.poe2craft.craftingsupport.search.api.PathSearchErrors;
import com.poe2craft.shared.crafting.infrastructure.WorkbenchDefinitionsLoader;
import com.poe2craft.shared.ruleset.api.RulesetBoundary;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * 경로·탐색·실행환경·Harness 동작을 고정 입력·기대 결과로 검증한다. 각 테스트가 공유하는 준비 값과 시나리오는 아래와 같다.
 *
 * <p>기존 계약: Test-only HTTP bridge to the real MVC controller/filter/service, without database or
 * Docker.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code main(args)}: 애플리케이션 또는 독립 실행 도구를 시작한다. 입력: {@code args}(명령인자). 사용하는 예외: {@code
 *       Exception}.
 * </ul>
 */
public final class PathSearchRuntimeHarness {
  public static void main(String[] args) throws Exception {
    WorkbenchDefinitionsLoader.initialize();
    var fixture = new PathSearchServiceTest();
    // A small work budget makes a real asynchronous pause reproducible for cancel/resume QA.
    var service =
        new PathSearchService(
            fixture.catalog,
            fixture.goals,
            fixture.identity,
            fixture.json,
            java.time.Clock.systemUTC(),
            java.util.concurrent.Executors.newSingleThreadExecutor(),
            777);
    var request = fixture.request(service);
    var mvc =
        MockMvcBuilders.standaloneSetup(new PathSearchController(service, fixture.json))
            .setControllerAdvice(new PathSearchErrors())
            .addFilters(new RulesetBoundary(fixture.json))
            .build();
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 19091), 0);
    server.createContext(
        "/",
        exchange -> {
          try {
            if (exchange.getRequestURI().getPath().equals("/qa/source")) {
              var bytes = fixture.json.writeValueAsBytes(request);
              exchange.getResponseHeaders().set("Content-Type", "application/json");
              exchange.sendResponseHeaders(200, bytes.length);
              exchange.getResponseBody().write(bytes);
            } else {
              var builder =
                  MockMvcRequestBuilders.request(
                      HttpMethod.valueOf(exchange.getRequestMethod()),
                      URI.create(exchange.getRequestURI().toString()));
              exchange
                  .getRequestHeaders()
                  .forEach((key, values) -> builder.header(key, values.toArray()));
              var cookie = exchange.getRequestHeaders().getFirst("Cookie");
              if (cookie != null) {
                for (var pair : cookie.split(";")) {
                  var parts = pair.trim().split("=", 2);
                  if (parts.length == 2)
                    builder.cookie(new jakarta.servlet.http.Cookie(parts[0], parts[1]));
                }
              }
              builder.content(exchange.getRequestBody().readAllBytes());
              var response = mvc.perform(builder).andReturn().getResponse();
              response
                  .getHeaderNames()
                  .forEach(key -> exchange.getResponseHeaders().put(key, response.getHeaders(key)));
              var bytes = response.getContentAsByteArray();
              exchange.sendResponseHeaders(response.getStatus(), bytes.length);
              exchange.getResponseBody().write(bytes);
            }
          } catch (Exception error) {
            exchange.sendResponseHeaders(500, -1);
          } finally {
            exchange.close();
          }
        });
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  server.stop(0);
                  service.close();
                }));
    server.start();
    System.out.println("PATH_SEARCH_RUNTIME_READY 127.0.0.1:19091");
    new CountDownLatch(1).await();
  }
}
