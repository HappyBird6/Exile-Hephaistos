package com.poe2craft.community.application;

import java.util.UUID;

public interface CommunityReactions {
  record Stats(long downloads, long recommendations, boolean recommended) {}

  Stats stats(UUID post, SessionBoard.Actor actor);

  Stats download(UUID post, SessionBoard.Actor actor);

  Stats recommend(UUID post, SessionBoard.Actor actor, boolean enabled);
}
