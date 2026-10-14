package com.poe2craft.crafting.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.crafting.application.pathsearch.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** A job's database and public-input descriptor stay in the configured local directory. */
public final class LocalCertifiedJobRepository implements CertifiedJobRepository {
  private final Path directory;
  private final ObjectMapper json;

  public LocalCertifiedJobRepository(Path directory, ObjectMapper json) {
    this.directory = directory.toAbsolutePath().normalize();
    this.json = json.copy();
    try {
      Files.createDirectories(this.directory);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot create job directory", e);
    }
  }

  private Path path(String id, String suffix) {
    if (!UUID.fromString(id).toString().equals(id))
      throw new IllegalArgumentException("Invalid job ID");
    return directory.resolve(id + suffix);
  }

  public MethodGraphStore create(Descriptor descriptor) {
    var store = open(descriptor);
    try {
      var destination = path(descriptor.id(), ".job.json");
      if (Files.exists(destination))
        throw new IllegalStateException("Job descriptor already exists");
      var pending = path(descriptor.id(), ".job.pending");
      Files.writeString(
          pending, json.writeValueAsString(descriptor), StandardOpenOption.CREATE_NEW);
      Files.move(pending, destination, StandardCopyOption.ATOMIC_MOVE);
      return store;
    } catch (IOException | RuntimeException failure) {
      store.close();
      throw new IllegalStateException("Cannot publish job descriptor", failure);
    }
  }

  public MethodGraphStore open(Descriptor descriptor) {
    return new SqliteMethodGraphStore(
        path(descriptor.id(), ".sqlite"), descriptor.identity(), 1024);
  }

  public List<Descriptor> list() {
    try (var files = Files.list(directory)) {
      var result = new ArrayList<Descriptor>();
      for (var file :
          files.filter(p -> p.getFileName().toString().endsWith(".job.json")).sorted().toList()) {
        try {
          var descriptor = json.readValue(Files.readString(file), Descriptor.class);
          if (!file.equals(path(descriptor.id(), ".job.json")))
            throw new IllegalStateException("Job descriptor filename mismatch");
          result.add(descriptor);
        } catch (IOException | RuntimeException failure) {
          java.util.logging.Logger.getLogger(LocalCertifiedJobRepository.class.getName())
              .warning(
                  "Unreadable local job descriptor retained for recovery: " + file.getFileName());
        }
      }
      return List.copyOf(result);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot read job descriptors", e);
    }
  }
}
