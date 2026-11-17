package com.poe2craft.craftingsupport.search.infrastructure;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.craftingsupport.search.application.AutomaticJobRepository;
import java.nio.file.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 크기가 제한된 자동 탐색 JSON을 로컬 파일에 원자적으로 저장한다. 해당 동작과 실패 경계를 검증하는 테스트다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code directory}: 디렉터리 ({@code Path}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code totalBytesAndDocumentCountAreBoundedWithoutDeletingResults()}: 이름에 명시한 시나리오의 실제
 *       결과·보존 상태를 기대값과 비교한다. 사용하는 예외: {@code Exception}.
 *   <li>{@code publicationSurvivesReopeningAndRejectsPathTraversal()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를
 *       기대값과 비교한다. 사용하는 예외: {@code Exception}.
 * </ul>
 */
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
        .isInstanceOf(AutomaticJobRepository.BudgetExceeded.class);
    assertThat(repository.ids()).containsExactlyInAnyOrderElementsOf(ids);
    for (var id : ids)
      assertThat(Files.size(directory.resolve(id + ".json"))).isEqualTo(64L * 1024 * 1024);
    for (var id : ids) repository.save(id, new byte[] {1});
    for (int i = 16; i < 256; i++) repository.save(UUID.randomUUID().toString(), new byte[] {1});
    assertThatThrownBy(() -> repository.save(extra, new byte[] {1}))
        .isInstanceOf(AutomaticJobRepository.BudgetExceeded.class);
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
