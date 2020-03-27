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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import com.google.appengine.api.datastore.AsyncDatastoreService;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.CompositeFilter;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.datastore.Text;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.mystery.data.model.Case;
import com.google.mystery.data.model.CaseData;
import com.google.mystery.data.model.Clue;
import com.google.mystery.data.model.Hint;
import com.google.mystery.data.model.Question;
import com.google.mystery.data.model.Session;
import com.google.mystery.data.model.SessionLog;
import com.google.mystery.data.model.Story;
import com.google.mystery.util.JsonUtil;

public class DataManager {
  public static final String CASE_DATA_ID = "caseDataId";
  public static final String STORY_KIND = "StoryV2";
  public static final String SESSION_KIND = "Session";
  public static final String SESSION_LOG_KIND = "SessionLog";
  public static final String CLUE_KIND = "ClueV2";
  public static final String QUESTION_KIND = "QuestionV2";
  public static final String HINT_KIND = "HintV2";
  public static final String CASE_KIND = "CaseV2";
  public static final String CASE_DATA_KIND = "CaseDataV2";
  private static final String CONFIG_KIND = "Config";
  // TODO: move cache out of here
  @Inject
  private CacheManager cacheManager;

  /** used to access datastore. */
  private final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

  private final AsyncDatastoreService asyncDatastore =
      DatastoreServiceFactory.getAsyncDatastoreService();

  public List<String> getConfig(String key) {
    Key entityKey = KeyFactory.createKey(CONFIG_KIND, key);
    try {
      Entity entity = datastore.get(entityKey);
      JsonArray valueJson =
          new JsonParser().parse(((Text) entity.getProperty("value")).getValue()).getAsJsonArray();
      return JsonUtil.fromStringArray(valueJson);
    } catch (EntityNotFoundException e) {
      return ImmutableList.of();
    }
  }

  public void putConfig(String key, List<String> value) {
    Entity configEntity = new Entity(CONFIG_KIND, key);
    configEntity.setUnindexedProperty("value", new Text(JsonUtil.toStringArray(value).toString()));
    datastore.put(configEntity);
  }

  public List<SessionLog> getSessionLogs(String sessionid) {
    Query query = new Query(SESSION_LOG_KIND);
    query.setFilter(new Query.FilterPredicate("sessionid", FilterOperator.EQUAL, sessionid));
    query.addSort("created", SortDirection.ASCENDING);
    Iterable<Entity> entites = datastore.prepare(query).asIterable();
    ImmutableList.Builder<SessionLog> sessionLogs = ImmutableList.builder();
    for (Entity sessionLogEntities : entites) {
      sessionLogs.add(SessionLog.fromEntity(sessionLogEntities));
    }
    return sessionLogs.build();
  }

  public void pushSessionLog(SessionLog sessionLog) {
    datastore.put(SessionLog.toEntity(sessionLog));
  }

  /** @param session session to rewrite with */
  public void putSession(Session session) {
    asyncDatastore.put(Session.toEntity(session));
    cacheManager.putSession(session);
  }

  public Session getSession(String sessionid) {
    Session session = cacheManager.getSession(sessionid);
    if (session != null) {
      return session;
    }
    Key key = KeyFactory.createKey(SESSION_KIND, sessionid);
    try {
      return Session.fromEntity(datastore.get(key));
    } catch (EntityNotFoundException e) {
      return null;
    }
  }

  /**
   * Looking up all stories by story type
   *
   * @param type of story too look for
   */
  public List<Story> queryStoryByType(String type, String caseDataId) {
    Query query = new Query(STORY_KIND);
    query.setFilter(new CompositeFilter(CompositeFilterOperator.AND,
        ImmutableList.of(new Query.FilterPredicate("type", FilterOperator.EQUAL, type),
            new Query.FilterPredicate(CASE_DATA_ID, FilterOperator.EQUAL, caseDataId))));
    Iterable<Entity> entites = datastore.prepare(query).asIterable();
    ImmutableList.Builder<Story> stories = ImmutableList.builder();
    for (Entity storyEntities : entites) {
      stories.add(Story.fromEntity(storyEntities));
    }
    return stories.build();
  }

  public void addStory(Story story) {
    Entity storyEntity = Story.toEntity(story);
    datastore.put(storyEntity);
  }

  public void addClue(Clue clue) {
    Entity clueEntity = Clue.toEntity(clue);
    datastore.put(clueEntity);
  }

  public void addQuestion(Question question) {
    Entity questionEntity = Question.toEntity(question);
    datastore.put(questionEntity);
  }

  public void addHint(Hint hint) {
    Entity hintEntity = Hint.toEntity(hint);
    datastore.put(hintEntity);
  }

  public Story getStory(String caseDataId, String id) {
    Query query = new Query(STORY_KIND);
    query.setFilter(new CompositeFilter(CompositeFilterOperator.AND,
        ImmutableList.of(new Query.FilterPredicate("id", FilterOperator.EQUAL, id),
            new Query.FilterPredicate(CASE_DATA_ID, FilterOperator.EQUAL, caseDataId))));
    Entity entity = datastore.prepare(query).asSingleEntity();
    if (entity != null) {
      return Story.fromEntity(entity);
    }
    return null;
  }

  /** Removing all stories */
  public void clearStories(String caseDataId) {
    clearByCaseDataId(STORY_KIND, caseDataId);
  }

  /** Removing all clues */
  public void clearClues(String caseDataId) {
    clearByCaseDataId(CLUE_KIND, caseDataId);
  }

  /** Removing all questions */
  public void clearQuestions(String caseDataId) {
    clearByCaseDataId(QUESTION_KIND, caseDataId);
  }

  /** Removing all Hints */
  public void clearHints(String caseDataId) {
    clearByCaseDataId(HINT_KIND, caseDataId);
  }

  /** Clear all entities by kind and caseDataId. */
  protected void clearByCaseDataId(String kind, String caseDataId) {
    Query query = new Query(kind);
    query.setFilter(new Query.FilterPredicate(CASE_DATA_ID, FilterOperator.EQUAL, caseDataId));
    query.setKeysOnly();
    Iterator<Entity> result = datastore.prepare(query).asIterator();
    List<Key> keysToDelete = new ArrayList<>();
    do {
      keysToDelete.clear();
      for (int i = 0; i < 100 && result.hasNext(); i++) {
        keysToDelete.add(result.next().getKey());
      }
      if (!keysToDelete.isEmpty()) {
        datastore.delete(keysToDelete);
      }
    } while (!keysToDelete.isEmpty());
  }

  public void clearSessionLog(String sessionid) {
    Query query = new Query(SESSION_LOG_KIND);
    query.setFilter(new Query.FilterPredicate("sessionid", FilterOperator.EQUAL, sessionid));
    Iterable<Entity> entities = datastore.prepare(query).asIterable();
    for (Entity entity : entities) {
      datastore.delete(entity.getKey());
    }
  }

  public List<Hint> queryAllHints(String caseDataId) {
    Query query = new Query(HINT_KIND);
    query.setFilter(new Query.FilterPredicate(CASE_DATA_ID, FilterOperator.EQUAL, caseDataId));
    Iterable<Entity> entries = datastore.prepare(query).asIterable();
    ImmutableList.Builder<Hint> hints = ImmutableList.builder();
    for (Entity entryEntity : entries) {
      hints.add(Hint.fromEntity(entryEntity));
    }
    return hints.build();
  }

  public Clue getClue(String caseDataId, String clueid) {
    Query query = new Query(CLUE_KIND);
    query.setFilter(new CompositeFilter(CompositeFilterOperator.AND,
        ImmutableList.of(new Query.FilterPredicate("clueId", FilterOperator.EQUAL, clueid),
            new Query.FilterPredicate(CASE_DATA_ID, FilterOperator.EQUAL, caseDataId))));
    Iterator<Entity> i = datastore.prepare(query).asIterator();
    if (i.hasNext()) {
      return Clue.fromEntity(i.next());
    }
    return null;
  }

  public List<Clue> queryAllClues(String caseDataId) {
    Query query = new Query(CLUE_KIND);
    query.setFilter(new Query.FilterPredicate(CASE_DATA_ID, FilterOperator.EQUAL, caseDataId));
    Iterable<Entity> entries = datastore.prepare(query).asIterable();
    ImmutableList.Builder<Clue> clues = ImmutableList.builder();
    for (Entity entity : entries) {
      clues.add(Clue.fromEntity(entity));
    }
    return clues.build();
  }

  public Question getQuestion(String caseDataId, int index) {
    Query query = new Query(QUESTION_KIND);
    query.setFilter(new CompositeFilter(CompositeFilterOperator.AND,
        ImmutableList.of(new Query.FilterPredicate("order", FilterOperator.EQUAL, index),
            new Query.FilterPredicate(CASE_DATA_ID, FilterOperator.EQUAL, caseDataId))));
    return Question.fromEntity(datastore.prepare(query).asSingleEntity());
  }

  public int getQuestionsSize(String caseDataId) {
    Query query = new Query(QUESTION_KIND);
    query.setFilter(new Query.FilterPredicate(CASE_DATA_ID, FilterOperator.EQUAL, caseDataId));
    return datastore.prepare(query).countEntities(FetchOptions.Builder.withLimit(50));
  }

  public List<Story> queryAllStories(String caseDataId) {
    Query query = new Query(STORY_KIND);
    query.setFilter(new Query.FilterPredicate(CASE_DATA_ID, FilterOperator.EQUAL, caseDataId));
    Iterable<Entity> entites = datastore.prepare(query).asIterable();
    ImmutableList.Builder<Story> stories = ImmutableList.builder();
    for (Entity storyEntities : entites) {
      stories.add(Story.fromEntity(storyEntities));
    }
    return stories.build();
  }

  public Hint getHint(String caseDataId, String hintid) {
    Query query = new Query(HINT_KIND);
    query.setFilter(new CompositeFilter(CompositeFilterOperator.AND,
        ImmutableList.of(new Query.FilterPredicate("hintId", FilterOperator.EQUAL, hintid),
            new Query.FilterPredicate(CASE_DATA_ID, FilterOperator.EQUAL, caseDataId))));
    Iterator<Entity> i = datastore.prepare(query).asIterator();
    if (i.hasNext()) {
      return Hint.fromEntity(i.next());
    }
    return null;
  }

  public List<SessionLog> getSessionLogs(String sessionid, long time) {
    Query query = new Query(SESSION_LOG_KIND);
    query.setFilter(new CompositeFilter(CompositeFilterOperator.AND,
        ImmutableList.of(new Query.FilterPredicate("sessionid", FilterOperator.EQUAL, sessionid),
            new Query.FilterPredicate("created", FilterOperator.GREATER_THAN, time))));
    query.addSort("created", SortDirection.ASCENDING);
    Iterable<Entity> entites = datastore.prepare(query).asIterable();
    ImmutableList.Builder<SessionLog> sessionLogs = ImmutableList.builder();
    for (Entity sessionLogEntities : entites) {
      sessionLogs.add(SessionLog.fromEntity(sessionLogEntities));
    }
    return sessionLogs.build();
  }

  //
  // Admin methods
  //
  /**
   * Admin methods.
   *
   * @param newCase
   */
  public void addCase(Case newCase) {
    Entity entity = Case.toEntity(newCase);
    datastore.put(entity);
  }

  public List<Case> queryAllCases() {
    Query query = new Query(CASE_KIND);
    Iterable<Entity> entites = datastore.prepare(query).asIterable();
    ImmutableList.Builder<Case> cases = ImmutableList.builder();
    for (Entity e : entites) {
      cases.add(Case.fromEntity(e));
    }
    return cases.build();
  }

  public List<CaseData> queryAllCaseData() {
    Query query = new Query(CASE_DATA_KIND);
    Iterable<Entity> entites = datastore.prepare(query).asIterable();
    ImmutableList.Builder<CaseData> caseDataBuilder = ImmutableList.builder();
    for (Entity e : entites) {
      caseDataBuilder.add(CaseData.fromEntity(e));
    }
    return caseDataBuilder.build();
  }

  public Case getCase(String caseId) {
    Key key = KeyFactory.createKey(CASE_KIND, caseId);
    try {
      return Case.fromEntity(datastore.get(key));
    } catch (EntityNotFoundException e) {
      return null;
    }
  }

  public void deleteCase(String caseId) {
    Key key = KeyFactory.createKey(CASE_KIND, caseId);
    datastore.delete(key);
  }

  public void updateCaseData(CaseData caseData) {
    Entity entity = CaseData.toEntity(caseData);
    datastore.put(entity);
  }

  public CaseData getCaseData(String caseDataId) {
    Key key = KeyFactory.createKey(CASE_DATA_KIND, caseDataId);
    try {
      return CaseData.fromEntity(datastore.get(key));
    } catch (EntityNotFoundException e) {
      return null;
    }
  }

  public void deleteCaseData(String caseDataId) {
    Key key = KeyFactory.createKey(CASE_DATA_KIND, caseDataId);
    datastore.delete(key);
  }

  public Map<String, String> getConfigMap() {
    ImmutableMap.Builder<String, String> resultBuilder = ImmutableMap.builder();
    Query query = new Query(CONFIG_KIND);
    Iterable<Entity> entites = datastore.prepare(query).asIterable();
    for (Entity entity : entites) {
      JsonArray valueJson =
          new JsonParser().parse(((Text) entity.getProperty("value")).getValue()).getAsJsonArray();
      resultBuilder.put(entity.getKey().getName(), valueJson.get(0).getAsString());
    }
    return resultBuilder.build();
  }

  public void saveConfigMap(Map<String, String> configMap) {
    for (Map.Entry<String, String> e : configMap.entrySet()) {
      putConfig(e.getKey(), ImmutableList.of(e.getValue()));
    }
  }
}
