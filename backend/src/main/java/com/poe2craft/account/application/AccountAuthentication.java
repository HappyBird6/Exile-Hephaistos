package com.poe2craft.account.application;

/** Authentication provider boundary; account identity and library ownership remain stable. */
public interface AccountAuthentication {
  AccountLibrary.Account authenticate(String username, String password, boolean register);
}
