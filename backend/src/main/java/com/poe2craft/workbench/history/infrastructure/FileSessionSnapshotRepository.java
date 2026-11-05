package com.poe2craft.workbench.history.infrastructure;

import com.poe2craft.configuration.BackendSettings;
import com.poe2craft.workbench.history.application.SessionSnapshotRepository;
import com.poe2craft.workbench.history.application.SessionSnapshotValidator;
import java.io.IOException;
import java.nio.file.*;
import java.security.SecureRandom;
import java.util.HexFormat;

/** Separate bounded directory; existing automatic-job files are never touched. */
public final class FileSessionSnapshotRepository implements SessionSnapshotRepository {
  private final Path directory;
  private final BackendSettings.SessionHistory settings;
  private final SecureRandom random = new SecureRandom();

  public FileSessionSnapshotRepository(Path directory) {
    this(directory, BackendSettings.defaults().sessionHistory());
  }

  public FileSessionSnapshotRepository(Path directory, BackendSettings.SessionHistory settings) {
    this.directory = directory.toAbsolutePath().normalize();
    this.settings = java.util.Objects.requireNonNull(settings);
  }

  @Override
  public synchronized String create(byte[] document) throws IOException {
    if (document.length > SessionSnapshotValidator.MAX_BYTES) throw new IllegalArgumentException();
    Files.createDirectories(directory);
    try (var paths = Files.list(directory)) {
      var files =
          paths.filter(p -> p.getFileName().toString().matches("[a-f0-9]{48}\\.json")).toList();
      long bytes = 0;
      for (var file : files) bytes += Files.size(file);
      if (files.size() >= settings.maxDocuments()
          || bytes + document.length > settings.maxStoredBytes()) throw new IOException();
    }
    byte[] token = new byte[24];
    random.nextBytes(token);
    String id = HexFormat.of().formatHex(token);
    Path pending = Files.createTempFile(directory, ".snapshot-", ".pending");
    try {
      Files.write(pending, document);
      Files.move(pending, directory.resolve(id + ".json"), StandardCopyOption.ATOMIC_MOVE);
    } finally {
      Files.deleteIfExists(pending);
    }
    return id;
  }

  @Override
  public byte[] read(String id) throws IOException {
    if (!id.matches("[a-f0-9]{48}")) throw new NoSuchFileException("snapshot");
    Path path = directory.resolve(id + ".json");
    if (Files.size(path) > SessionSnapshotValidator.MAX_BYTES) throw new IOException();
    return Files.readAllBytes(path);
  }
}
