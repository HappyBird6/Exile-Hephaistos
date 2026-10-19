package com.poe2craft.crafting.infrastructure;

import static org.assertj.core.api.Assertions.*;

import java.nio.file.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalAutomaticJobRepositoryTest {
  @TempDir Path directory;

  @Test
  void totalBytesAndDocumentCountAreBoundedWithoutDeletingResults() throws Exception {
    var repository = new LocalAutomaticJobRepository(directory);
    var ids = new java.util.ArrayList<String>();
    for (int i = 0; i < 16; i++) {
      var id = UUID.randomUUID().toString();
      ids.add(id);
      try (var file =
          new java.io.RandomAccessFile(directory.resolve(id + ".json").toFile(), "rw")) {
        file.setLength(64L * 1024 * 1024);
      }
    }
    var extra = UUID.randomUUID().toString();
    assertThatThrownBy(() -> repository.save(extra, new byte[] {1}))
        .isInstanceOf(
            com.poe2craft.crafting.application.pathsearch.AutomaticJobRepository.BudgetExceeded
                .class);
    assertThat(repository.ids()).containsExactlyInAnyOrderElementsOf(ids);
    for (var id : ids)
      assertThat(Files.size(directory.resolve(id + ".json"))).isEqualTo(64L * 1024 * 1024);
    for (var id : ids) repository.save(id, new byte[] {1});
    for (int i = 16; i < 256; i++) repository.save(UUID.randomUUID().toString(), new byte[] {1});
    assertThatThrownBy(() -> repository.save(extra, new byte[] {1}))
        .isInstanceOf(
            com.poe2craft.crafting.application.pathsearch.AutomaticJobRepository.BudgetExceeded
                .class);
    assertThat(repository.ids()).hasSize(256);
    repository.save(ids.getFirst(), new byte[] {2});
    assertThat(repository.read(ids.getFirst())).containsExactly((byte) 2);
  }

  @Test
  void publicationSurvivesReopeningAndRejectsPathTraversal() throws Exception {
    var id = UUID.randomUUID().toString();
    var repository = new LocalAutomaticJobRepository(directory);
    repository.save(id, new byte[] {1});
    repository.save(id, new byte[] {2, 3});
    Files.writeString(directory.resolve("unrelated.txt"), "keep");
    var reopened = new LocalAutomaticJobRepository(directory);
    assertThat(reopened.ids()).containsExactly(id);
    assertThat(reopened.read(id)).containsExactly((byte) 2, (byte) 3);
    assertThat(directory.resolve(id + ".pending")).doesNotExist();
    assertThatThrownBy(() -> reopened.save("../outside", new byte[0]))
        .isInstanceOf(IllegalArgumentException.class);
    reopened.delete(id);
    assertThat(reopened.ids()).isEmpty();
    assertThat(directory.resolve("unrelated.txt")).hasContent("keep");
  }
}
