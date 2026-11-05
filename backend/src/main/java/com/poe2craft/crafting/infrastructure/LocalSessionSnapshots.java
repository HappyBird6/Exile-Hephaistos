package com.poe2craft.crafting.infrastructure;

import com.poe2craft.crafting.application.SessionSnapshots;
import java.io.IOException;
import java.nio.file.*;
import java.security.SecureRandom;
import java.util.HexFormat;

/** Separate bounded directory; existing automatic-job files are never touched. */
public final class LocalSessionSnapshots implements SessionSnapshots.Repository {
  private final Path directory;
  private final SecureRandom random = new SecureRandom();

  public LocalSessionSnapshots(Path directory) {
    this.directory = directory.toAbsolutePath().normalize();
  }

  @Override
  public synchronized String create(byte[] document) throws IOException {
    if (document.length > SessionSnapshots.MAX_BYTES) throw new IllegalArgumentException();
    Files.createDirectories(directory);
    try (var paths = Files.list(directory)) {
      var files =
          paths.filter(p -> p.getFileName().toString().matches("[a-f0-9]{48}\\.json")).toList();
      long bytes = 0;
      for (var file : files) bytes += Files.size(file);
      if (files.size() >= 500 || bytes + document.length > 100_000_000) throw new IOException();
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
    if (Files.size(path) > SessionSnapshots.MAX_BYTES) throw new IOException();
    return Files.readAllBytes(path);
  }
}
