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
      "37144b63e3beedf58213fbeba6abd7f8e76a3209acaa829857780320447deebb";

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
