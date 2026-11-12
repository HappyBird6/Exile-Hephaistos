package com.poe2craft.account.infrastructure;

import com.poe2craft.account.application.AccountLibrary.Account;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Semaphore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PasswordAccounts {
  private final JdbcTemplate db;
  private final BCryptPasswordEncoder encoder;
  private final String dummy;
  private final Semaphore slots;
  private final int minimum;

  public PasswordAccounts(
      JdbcTemplate db,
      @Value("${account.auth.bcrypt-cost}") int cost,
      @Value("${account.auth.max-concurrent}") int concurrency,
      @Value("${account.auth.minimum-password-length}") int minimum) {
    this.db = db;
    this.encoder = new BCryptPasswordEncoder(cost);
    this.dummy = encoder.encode(UUID.randomUUID().toString());
    this.slots = new Semaphore(concurrency);
    this.minimum = minimum;
  }

  @Transactional
  public Account authenticate(String username, String password, boolean register) {
    if (username == null
        || !username.matches("[A-Za-z0-9_]{3,32}")
        || password == null
        || password.length() < minimum
        || password.getBytes(StandardCharsets.UTF_8).length > 72)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_CREDENTIAL_FORMAT");
    username = username.toLowerCase(Locale.ROOT);
    if (!slots.tryAcquire())
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "AUTH_BUSY");
    try {
      if (register) {
        var account = new Account(UUID.randomUUID(), username);
        String hash = encoder.encode(password);
        // Lock same username registrations together, including across application instances.
        db.queryForList("SELECT pg_advisory_xact_lock(hashtextextended(?,0))", username);
        if (Boolean.TRUE.equals(
            db.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM app_user.password_identity WHERE username=?)",
                Boolean.class,
                username)))
          throw new ResponseStatusException(HttpStatus.CONFLICT, "USERNAME_UNAVAILABLE");
        db.update(
            "INSERT INTO app_user.account(id,display_name) VALUES (?,?)",
            account.id(),
            account.displayName());
        db.update(
            "INSERT INTO app_user.password_identity(username,account_id,password_hash) VALUES (?,?,?)",
            username,
            account.id(),
            hash);
        return account;
      }
      var rows =
          db.queryForList(
              "SELECT p.password_hash,a.id,a.display_name FROM app_user.password_identity p JOIN app_user.account a ON a.id=p.account_id WHERE p.username=?",
              username);
      String hash = rows.isEmpty() ? dummy : (String) rows.getFirst().get("password_hash");
      boolean matches = encoder.matches(password, hash);
      if (rows.isEmpty() || !matches)
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS");
      var row = rows.getFirst();
      return new Account((UUID) row.get("id"), (String) row.get("display_name"));
    } finally {
      slots.release();
    }
  }
}
