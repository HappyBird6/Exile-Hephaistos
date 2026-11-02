package com.poe2craft.item;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import com.poe2craft.item.testparser.ItemTextService;
import java.util.List;
import org.junit.jupiter.api.Test;

class RegisteredTextMapperTest {
  private final ItemTextService parser = new ItemTextService();

  @Test
  void everyRegisteredTopBaseWithoutImplicitAcceptsExactNormalIdentity() {
    int checked = 0;
    for (var base : BaseRegistry.topBases().values()) {
      var catalog = ItemCatalogLoader.loadTopBase(base.key());
      if (catalog.base().hasImplicit()) continue;
      String source =
          "Item Class: "
              + base.policy().itemClass()
              + "\nRarity: Normal\n"
              + base.name()
              + "\n--------\nItem Level: 82";
      var result = new SolarTextMapper(catalog).map(parser.parseText(source));
      assertThat(result.issues()).as(base.key()).isEmpty();
      assertThat(result.state().baseItemId()).isEqualTo(base.id());
      assertThat(result.state().snapshotId()).isEqualTo(catalog.metadata().snapshotId());
      assertThat(result.state().explicits()).isEmpty();
      checked++;
    }
    assertThat(checked).isGreaterThan(20);
  }

  @Test
  void ambiguousNameNeverChoosesAnElementOrFirstCatalog() {
    var fire = ItemCatalogLoader.loadTopBase("shrine-fire");
    var ice = ItemCatalogLoader.loadTopBase("shrine-ice");
    var mapper = new SolarTextMapper(List.of(fire, ice));
    var result =
        mapper.map(
            parser.parseText(
                "Item Class: Sceptres\nRarity: Normal\nShrine Sceptre\n--------\nItem Level: 82"));
    assertThat(result.mapped()).isFalse();
    assertThat(result.state()).isNull();
  }

  @Test
  void mismatchAndUnknownPropertiesDoNotProduceLossyState() {
    var catalog = ItemCatalogLoader.loadTopBase("soldier");
    var mapper = new SolarTextMapper(catalog);
    var source =
        "Item Class: Body Armours\nRarity: Normal\nSoldier Cuirass\n--------\nItem Level: 82";
    assertThat(mapper.map(parser.parseText(source.replace("Body Armours", "Amulets"))).mapped())
        .isFalse();
    var withQuality = source + "\nQuality: +20%";
    var parsed = parser.parseText(withQuality);
    assertThat(mapper.map(parsed).state()).isNull();
    assertThat(parsed.text().originalText()).isEqualTo(withQuality);
    for (var suffix : List.of("\nDesecrated Prefix", "\nDesecrated Suffix", "\nCorrupted")) {
      var special = parser.parseText(source + suffix);
      assertThat(mapper.map(special).state()).isNull();
      assertThat(special.text().originalText()).isEqualTo(source + suffix);
    }
  }
}
