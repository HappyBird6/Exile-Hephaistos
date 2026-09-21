package com.poe2craft.codegen;

import org.flywaydb.core.Flyway;
import org.jooq.codegen.GenerationTool;
import org.jooq.meta.jaxb.*;
import org.testcontainers.containers.PostgreSQLContainer;

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
            "season",
            "modifier",
            "item",
            "currency",
            "currencyrule",
            "price",
            "crafting",
            "preset",
            "ai",
            "app_user"
          }) {
        String packageModule = module.equals("app_user") ? "user" : module;
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
