package com.poe2craft.shared.item;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.core.io.ClassPathResource;

/**
 * 복사 텍스트와 반복 제작 표시가 공유하는 영문 문장·수치 결합 자료다.
 *
 * <p>기존 계약: Shared English source bindings; the same digest protects clipboard and repeat display.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code TEMPLATE_SHA256}: 256; 초기값 {@code
 *       "99b10ffb548f337b0693339ac5523d3c23aa6d1036e4ad47d7b1f6cd9dcb6593"} ({@code String}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code ModifierTextResources()}: 외부 인스턴스 생성이 필요 없는 유틸리티 생성자다.
 *   <li>{@code read()}: 지정한 입력 또는 저장된 값을 읽는다. 반환: {@code byte[]}. 사용하는 예외: {@code
 *       IllegalStateException}, {@code IOException}.
 * </ul>
 */
public final class ModifierTextResources {
  private ModifierTextResources() {}

  public static final String TEMPLATE_SHA256 =
      "99b10ffb548f337b0693339ac5523d3c23aa6d1036e4ad47d7b1f6cd9dcb6593";

  public static byte[] read() throws IOException {
    try (var stream =
        new ClassPathResource("chaosrepeat/modifierTemplates.json").getInputStream()) {
      byte[] bytes = stream.readAllBytes();
      String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
      if (!TEMPLATE_SHA256.equals(digest))
        throw new IllegalStateException("Modifier text source digest differs");
      return bytes;
    } catch (NoSuchAlgorithmException unavailable) {
      throw new IllegalStateException(unavailable);
    }
  }
}
