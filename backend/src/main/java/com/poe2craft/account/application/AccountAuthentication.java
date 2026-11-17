package com.poe2craft.account.application;

/**
 * 비밀번호 인증과 사용자 식별의 경계를 정의한다.
 *
 * <p>기존 계약: Authentication provider boundary; account identity and library ownership remain stable.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code authenticate(username, password, register)}: 제출한 인증 정보를 확인한다. 입력: {@code
 *       username}(사용자명), {@code password}(비밀번호), {@code register}(등록여부). 반환: {@code
 *       AccountLibrary.Account}.
 * </ul>
 */
public interface AccountAuthentication {
  AccountLibrary.Account authenticate(String username, String password, boolean register);
}
