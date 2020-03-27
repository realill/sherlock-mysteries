// Copyright 2020 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.mystery.data;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.PostConstruct;
import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheFactory;
import com.google.mystery.data.model.Case;
import com.google.mystery.data.model.Session;
import com.google.mystery.data.model.Story;

/**
 * Handles cache.
 *
 * @author ilyaplatonov
 */
public class CacheManager {
  private Cache cache;

  @PostConstruct
  public void init() {
    try {
      CacheFactory cacheFactory = javax.cache.CacheManager.getInstance().getCacheFactory();
      cache = cacheFactory.createCache(Collections.emptyMap());
    } catch (CacheException e) {
      throw new IllegalArgumentException(e);
    }
  }

  public void putSession(Session session) {
    String key = "session-" + session.getSessionid();
    cache.put(key, session);
  }

  public Session getSession(String sessionid) {
    String key = "session-" + sessionid;
    return (Session) cache.get(key);
  }

  public Case getCase(String caseId, Function<?, ?> mappingFunction) {
    String key = "case-" + caseId;
    return (Case) cache.computeIfAbsent(key, mappingFunction);
  }

  public Story getStory(String caseDataId, String storyId, Function<?, ?> mappingFunction) {
    String key = "story-" + caseDataId + "-" + storyId;
    return (Story) cache.computeIfAbsent(key, mappingFunction);
  }

  public Map<String, String> getConfig(Function<?, ?> mappingFunction) {
    return (Map<String, String>) cache.computeIfAbsent("config", mappingFunction);
  }

  public void invalidateCase(String caseId) {
    String key = "case-" + caseId;
    cache.remove(key);
  }

  public void invalidateConfig() {
    cache.remove("config");
  }
}
