package com.poe2craft.season.infrastructure;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.season.api.CrawlModels.*;
import com.poe2craft.season.application.port.CrawlRunner;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PythonCrawlRunner implements CrawlRunner {
  private final ObjectMapper json;
  private final Path root;
  private final String python;

  public PythonCrawlRunner(
      ObjectMapper json,
      @Value("${app.crawl.directory:../data-pipeline/captures}") String directory,
      @Value("${app.crawl.python:python}") String python) {
    this.json = json;
    this.root = Path.of(directory).toAbsolutePath().normalize();
    this.python = python;
  }

  @Override
  public List<Source> capture(Run run) throws Exception {
    Files.createDirectories(root);
    Path realRoot = root.toRealPath();
    Path job = Files.createDirectory(realRoot.resolve(run.id().toString()));
    Path targets = job.resolve("targets.json");
    json.writeValue(targets.toFile(), Map.of("targets", run.targets()));
    ProcessBuilder builder =
        new ProcessBuilder(
            python,
            "-m",
            "poe2etl",
            "crawl",
            "--targets-file",
            targets.toString(),
            "--output",
            job.toString());
    builder.directory(job.toFile());
    // No shell, inherited secret environment, or unbounded stderr log.
    var environment = builder.environment();
    var retained = new java.util.HashMap<String, String>();
    for (String key :
        List.of("PATH", "Path", "SystemRoot", "WINDIR", "PYTHONPATH", "LANG", "LC_ALL")) {
      if (environment.containsKey(key)) retained.put(key, environment.get(key));
    }
    environment.clear();
    environment.putAll(retained);
    environment.put("PYTHONIOENCODING", "utf-8");
    builder.redirectError(ProcessBuilder.Redirect.DISCARD);
    Process process = builder.start();
    var output = new CompletableFuture<byte[]>();
    Thread reader =
        Thread.ofVirtual()
            .start(
                () -> {
                  try {
                    byte[] bytes = process.getInputStream().readNBytes(65537);
                    if (bytes.length > 65536) {
                      process.destroyForcibly();
                      throw new IllegalStateException("CLI_OUTPUT_LIMIT");
                    }
                    output.complete(bytes);
                  } catch (Exception e) {
                    output.completeExceptionally(e);
                  }
                });
    try {
      // Leave recovery a safety margin before the database's ten-minute stale lease.
      if (!process.waitFor(570, TimeUnit.SECONDS)) throw new IllegalStateException("CLI_TIMEOUT");
      if (process.exitValue() != 0) throw new IllegalStateException("CLI_FAILED");
      var result = json.readTree(output.get(5, TimeUnit.SECONDS));
      if (!"RAW_CAPTURED".equals(result.path("status").asText()))
        throw new IllegalStateException("CLI_INVALID_RESULT");
      Path manifest = Path.of(result.path("manifest").asText());
      if (!manifest.isAbsolute()) throw new IllegalStateException("CLI_INVALID_PATH");
      manifest = manifest.toRealPath();
      if (!manifest.startsWith(job.toRealPath())
          || !Files.isRegularFile(manifest)
          || Files.size(manifest) > 262144) throw new IllegalStateException("CLI_INVALID_PATH");
      var document = json.readTree(Files.readAllBytes(manifest));
      if (!"RAW_CAPTURED".equals(document.path("status").asText())
          || !document.path("sources").isArray())
        throw new IllegalStateException("CLI_INVALID_MANIFEST");
      List<Source> sources =
          json.convertValue(document.path("sources"), new TypeReference<List<Source>>() {});
      if (sources.isEmpty() || sources.size() > 100)
        throw new IllegalStateException("CLI_INVALID_SOURCES");
      for (Source s : sources)
        if (s.url() == null
            || !(s.url().equals("https://poe2db.tw/robots.txt")
                || s.url().equals("https://poe2db.tw/us/General_disclaimer")
                || run.targets().stream().anyMatch(t -> t.url().equals(s.url())))
            || s.status() < 100
            || s.status() > 599
            || s.sha256() == null
            || !s.sha256().matches("[0-9a-f]{64}")
            || s.bytes() < 0
            || s.bytes() > 8388608
            || s.fetchedAt() == null) throw new IllegalStateException("CLI_INVALID_SOURCE");
      return List.copyOf(sources);
    } finally {
      process.descendants().forEach(ProcessHandle::destroyForcibly);
      process.destroyForcibly();
      reader.interrupt();
    }
  }
}
