package com.poe2craft.bootstrap;

import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AdminCredentials {
  private final String username;
  private final String password;

  public AdminCredentials(
      @Value("${app.admin.username:}") String username,
      @Value("${app.admin.password:}") String password) {
    this.username = username;
    this.password = password;
    if (username.isEmpty() && password.isEmpty()) return;
    if (!username.matches("[A-Za-z0-9_.-]{3,64}")
        || password.length() < 16
        || password.getBytes(StandardCharsets.UTF_8).length > 72
        || password.chars().anyMatch(Character::isISOControl)
        || !password.matches(".*[a-z].*")
        || !password.matches(".*[A-Z].*")
        || !password.matches(".*[0-9].*")
        || password
            .toLowerCase(java.util.Locale.ROOT)
            .contains(username.toLowerCase(java.util.Locale.ROOT))
        || password.toLowerCase(java.util.Locale.ROOT).contains("password")
        || password.toLowerCase(java.util.Locale.ROOT).contains("replace"))
      throw new IllegalArgumentException(
          "Admin credentials must both be absent or a valid username and strong password of at least 16 characters and at most 72 UTF-8 bytes");
  }

  public boolean configured() {
    return !username.isEmpty();
  }

  public String username() {
    return username;
  }

  public String password() {
    return password;
  }
}
