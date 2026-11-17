package com.poe2craft.codegen;

import org.flywaydb.core.Flyway;
import org.jooq.codegen.GenerationTool;
import org.jooq.meta.jaxb.*;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Generate·Jooq의 실행·조회 경계를 담당한다.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code main(args)}: 애플리케이션 또는 독립 실행 도구를 시작한다. 입력: {@code args}(명령인자). 사용하는 예외: {@code
 *       Exception}.
 * </ul>
 */
public final class GenerateJooq {
  public static void main(String[] args) throws Exception {
    // No configurable database URL: code generation must never connect to production.
    try (var postgres = new PostgreSQLContainer<>("postgres:17.6-alpine")) {
      postgres.start();
      Flyway.configure()
          .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
          .locations("filesystem:" + args[0])
          .load()
          .migrate();
      for (String module :
          new String[] {
            "season", "modifier", "item", "currency", "currencyrule", "price", "crafting", "preset"
          }) {
        String packageModule = module;
        GenerationTool.generate(
            new Configuration()
                .withJdbc(
                    new Jdbc()
                        .withDriver("org.postgresql.Driver")
                        .withUrl(postgres.getJdbcUrl())
                        .withUser(postgres.getUsername())
                        .withPassword(postgres.getPassword()))
                .withGenerator(
                    new Generator()
                        .withDatabase(
                            new Database()
                                .withName("org.jooq.meta.postgres.PostgresDatabase")
                                // Kept only in migration history; no runtime collection feature.
                                .withExcludes("crawl_.*")
                                .withInputSchema(module))
                        .withTarget(
                            new Target()
                                .withPackageName(
                                    "com.poe2craft." + packageModule + ".infrastructure.jooq")
                                .withDirectory(args[1]))));
      }
    }
  }
}
