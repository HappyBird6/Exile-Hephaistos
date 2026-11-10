package com.poe2craft.shared.item;

import java.util.List;

/** Clipboard observations, not inferred game limits or resolved installed-Augment identities. */
public record ItemObservations(
    Integer socketCount,
    List<String> augmentEffects,
    List<GrantedSkill> grantedSkills,
    Double augmentEffectScale) {
  public ItemObservations(
      Integer socketCount, List<String> augmentEffects, List<GrantedSkill> grantedSkills) {
    this(socketCount, augmentEffects, grantedSkills, null);
  }

  public record GrantedSkill(String name, Integer level) {
    public GrantedSkill {
      if (name == null || name.isBlank() || name.length() > 256 || (level != null && level < 1))
        throw new IllegalArgumentException("Invalid observed granted skill");
    }
  }

  public ItemObservations {
    if (augmentEffectScale != null
        && (!Double.isFinite(augmentEffectScale)
            || augmentEffectScale < 0.01
            || augmentEffectScale > 1000))
      throw new IllegalArgumentException("Invalid observed socket-effect scale");
    if (socketCount != null && socketCount < 0)
      throw new IllegalArgumentException("Invalid observed socket count");
    augmentEffects = List.copyOf(augmentEffects);
    grantedSkills = List.copyOf(grantedSkills);
    if (augmentEffects.size() > 128
        || grantedSkills.size() > 16
        || augmentEffects.stream().anyMatch(s -> s.isBlank() || s.length() > 4096)
        || grantedSkills.stream().map(GrantedSkill::name).distinct().count()
            != grantedSkills.size())
      throw new IllegalArgumentException("Invalid clipboard observations");
  }

  public boolean needsAugmentResolution() {
    return !augmentEffects.isEmpty();
  }
}
