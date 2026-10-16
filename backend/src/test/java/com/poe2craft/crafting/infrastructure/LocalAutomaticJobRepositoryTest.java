package com.poe2craft.crafting.infrastructure;

import static org.assertj.core.api.Assertions.*;

import java.nio.file.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalAutomaticJobRepositoryTest {
  @TempDir Path directory;

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
