package com.poe2craft.crafting.infrastructure;

import com.poe2craft.crafting.application.pathsearch.AutomaticJobRepository;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public final class LocalAutomaticJobRepository implements AutomaticJobRepository {
  private static final long MAX_BYTES = 64L * 1024 * 1024;
  private final Path directory;

  public LocalAutomaticJobRepository(Path directory) {
    this.directory = directory.toAbsolutePath().normalize();
    try {
      Files.createDirectories(this.directory);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot create automatic job storage", e);
    }
  }

  private Path path(String id) {
    if (!UUID.fromString(id).toString().equals(id))
      throw new IllegalArgumentException("Invalid job id");
    return directory.resolve(id + ".json");
  }

  public List<String> ids() {
    try (var files = Files.list(directory)) {
      return files
          .map(p -> p.getFileName().toString())
          .filter(n -> n.matches("[0-9a-f-]{36}\\.json"))
          .map(n -> n.substring(0, 36))
          .sorted()
          .toList();
    } catch (IOException e) {
      throw new IllegalStateException("Cannot list automatic jobs", e);
    }
  }

  public byte[] read(String id) {
    try {
      var path = path(id);
      if (Files.size(path) > MAX_BYTES) throw new AutomaticJobRepository.BudgetExceeded();
      return Files.readAllBytes(path);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot read automatic job", e);
    }
  }

  public void save(String id, byte[] value) {
    if (value.length > MAX_BYTES) throw new AutomaticJobRepository.BudgetExceeded();
    var target = path(id);
    var temporary = directory.resolve(id + ".pending");
    try {
      Files.write(temporary, value);
      try {
        Files.move(
            temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      } catch (AtomicMoveNotSupportedException e) {
        Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Cannot publish automatic job", e);
    }
  }

  public void delete(String id) {
    try {
      Files.deleteIfExists(path(id));
    } catch (IOException e) {
      throw new IllegalStateException("Cannot expire automatic job", e);
    }
  }
}
