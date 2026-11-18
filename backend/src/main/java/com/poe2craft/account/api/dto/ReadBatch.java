package com.poe2craft.account.api.dto;

import com.poe2craft.account.application.AccountLibrary;
import java.util.List;

/** generation은 클라이언트가 읽은 보관함 세대이며 references는 한 번에 읽을 문서 키 목록이다. */
public record ReadBatch(Long generation, List<AccountLibrary.ReadKey> references) {}
