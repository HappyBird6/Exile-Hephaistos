package com.poe2craft.account.api.dto;

/** 로그인·가입 요청의 username과 password는 인증할 사용자 이름과 비밀번호다. */
public record Credentials(String username, String password) {}
