package com.poe2craft.shared.item;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.core.io.ClassPathResource;

/** Shared English source bindings; the same digest protects clipboard and repeat display. */
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
