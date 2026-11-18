package com.poe2craft.account.api.dto;

import com.poe2craft.account.application.AccountLibrary.Account;

/** 계정 상태 응답이다. enabled는 인증 기능 사용 여부, user는 로그인 계정, csrf는 세션 요청 검증 토큰이다. */
public record Status(boolean enabled, Account user, String csrf) {}
