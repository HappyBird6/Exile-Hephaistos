package com.poe2craft.workbench.api;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.configuration.PublicContracts;
import com.poe2craft.workbench.display.DisplayLocale;
import java.util.*;
import org.junit.jupiter.api.Test;

class PublicContractTest {
  @Test
  void everyPublishedLocaleHasAnExplicitServerFormatter() {
    var codes = PublicContracts.LOCALE_CODES;
    assertThat(Arrays.stream(DisplayLocale.values()).map(DisplayLocale::code).toList())
        .containsExactlyInAnyOrderElementsOf(codes);
  }

  @Test
  void transportReadsThePublicContractRatherThanItsOwnLimit() {
    assertThat(ChaosRepeatController.MAX_BODY_BYTES)
        .isEqualTo(PublicContracts.REPEAT.maximumBodyBytes());
    assertThat(PublicContracts.REPEAT.defaultUses())
        .isBetween(PublicContracts.REPEAT.minimumUses(), PublicContracts.REPEAT.maximumUses());
  }
}
