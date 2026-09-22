package com.poe2craft.season.application.port;

import com.poe2craft.season.api.CrawlModels.*;
import java.util.List;

public interface CrawlRunner {
  List<Source> capture(Run run) throws Exception;
}
