package com.poe2craft.shared.ids;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;

/**
 * 공통 FE/BE 사례로 식별자와 자유 텍스트의 분리, 왕복 보존, 충돌 거부를 검증한다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code json}: 이 경계의 JSON 읽기·쓰기 변환기 ({@code ObjectMapper}).
 *   <li>{@code codec}: 저장·전송 표현의 변환기 ({@code GameIdCodec}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code sharedFixtureRoundTripsIncludingSanctificationKeysWithoutChangingText()}: 신성화 맵 키까지
 *       왕복 변환되고 자유 텍스트는 바뀌지 않으며 전송 크기가 줄어드는지 검증한다. 사용하는 예외: {@code Exception}.
 *   <li>{@code collidingKeysAndCorruptMappingDigestAreRejected()}: 복원 후 겹치는 키와 위조한 사전 다이제스트를 거절하는지
 *       검증한다. 사용하는 예외: {@code Exception}.
 * </ul>
 */
class GameIdCodecTest {
  private final ObjectMapper json = new ObjectMapper();
  private final GameIdCodec codec = new GameIdCodec(GameIdMappings.active());

  @Test
  void sharedFixtureRoundTripsIncludingSanctificationKeysWithoutChangingText() throws Exception {
    var fixture =
        json.readTree(
            Files.readString(Path.of("../docs/contracts/identifier-transport-v1/fixture.json")));
    assertEquals(fixture.get("compact"), codec.encode(fixture.get("plain")));
    assertEquals(fixture.get("plain"), codec.decode(fixture.get("compact")));
    assertTrue(
        json.writeValueAsBytes(fixture.get("compact")).length
            < json.writeValueAsBytes(fixture.get("plain")).length);
  }

  @Test
  void collidingKeysAndCorruptMappingDigestAreRejected() throws Exception {
    var fixture =
        json.readTree(
            Files.readString(Path.of("../docs/contracts/identifier-transport-v1/fixture.json")));
    var plain =
        (com.fasterxml.jackson.databind.node.ObjectNode)
            fixture.at("/plain/state/explicits/0/values");
    plain.put(fixture.at("/compact/state/explicits/0/values").fieldNames().next(), 999);
    assertThrows(IllegalArgumentException.class, () -> codec.encode(fixture.get("plain")));
    var mapping =
        json.readTree(Files.readString(Path.of("src/main/resources/contracts/game-ids-v1.json")));
    ((com.fasterxml.jackson.databind.node.ObjectNode) mapping).put("identity", "forged");
    assertThrows(IllegalArgumentException.class, () -> new GameIdMappings(mapping));
  }
}
