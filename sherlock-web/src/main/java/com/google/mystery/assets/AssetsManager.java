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

package com.google.mystery.assets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import com.google.api.client.util.Strings;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.mystery.data.CacheManager;
import com.google.mystery.data.DataManager;
import com.google.mystery.data.model.Case;
import com.google.mystery.data.model.CaseData;
import com.google.mystery.data.model.Clue;
import com.google.mystery.data.model.DirectoryEntry;
import com.google.mystery.data.model.Hint;
import com.google.mystery.data.model.Question;
import com.google.mystery.data.model.Session;
import com.google.mystery.data.model.Story;

public class AssetsManager {

  @Inject
  private DataManager dataManager;
  @Inject
  private SearchManager searchManager;
  @Inject
  private CacheManager cacheManager;

  /** config key to keep locations. */
  static final String LOCATIONS_CONFIG_KEY = "LocationsKey";
  /** config key to keep streets. */
  static final String STREETS_CONFIG_KEY = "StreetsKey";
  /** locally kept set of streets, have to be sync with db. */
  protected Set<String> streets = null;
  /** locally kept set of locations, have to be sync with db. */
  protected Set<String> locations = null;

  @PostConstruct
  public void init() {
    reloadLocations();
  }

  /**
   * Normalizes location and checks if it is valid
   *
   * @param location location to check
   * @return normalized location or <code>null</code> if location is in invalid format
   */
  public String checkLocation(String location) {
    location = normalizeLocation(location);
    if (locations.contains(location)) {
      return location;
    }
    return checkAddress(location);
  }

  /** @return if given location is street and number address */
  public String checkAddress(String location) {
    Matcher locationMatcher = locationPattern.matcher(location);
    if (locationMatcher.matches()) {
      return String.format("%s%s %s", locationMatcher.group(1), locationMatcher.group(2),
          locationMatcher.group(3));
    }
    Matcher revMatcher = revLocationPattern.matcher(location);
    if (revMatcher.matches()) {
      return String.format("%s%s %s", revMatcher.group(2), revMatcher.group(3),
          revMatcher.group(1));
    }
    return null;
  }

  public static String normalizeLocation(String location) {
    return location.trim().toLowerCase().replaceAll("\\s+", " ");
  }

  public void putStreets(List<String> streets) {
    dataManager.putConfig(STREETS_CONFIG_KEY, streets);
    reloadLocations();
  }

  public void putLocations(List<String> locations) {
    dataManager.putConfig(LOCATIONS_CONFIG_KEY, locations);
    reloadLocations();
  }

  private Pattern locationPattern = null;
  private Pattern revLocationPattern = null;

  protected void reloadLocations() {
    streets = ImmutableSet.copyOf(dataManager.getConfig(STREETS_CONFIG_KEY).stream()
        .map(s -> normalizeLocation(s)).iterator());
    locations = ImmutableSet.copyOf(dataManager.getConfig(LOCATIONS_CONFIG_KEY).stream()
        .map(s -> normalizeLocation(s)).iterator());
    // \d{1,3}\s{0,1}(A|B|C|D|)(EC|WC|SE|SW|NW|a b)
    // number or number with A,B,C,D. eg 123 or 123B
    String numberRegex = "(\\d{1,3})\\s{0,1}(a|b|c|d|A|B|C|D|)";
    String streetRegex = "(" + Joiner.on("|").join(streets) + ")";
    locationPattern = Pattern.compile(numberRegex + "\\s" + streetRegex);
    // Baker Street 221B situations
    revLocationPattern = Pattern.compile(streetRegex + "\\s" + numberRegex);
  }

  /**
   * Get story from DB or cache.
   */
  public Story getStory(String caseDataId, String id) {
    return cacheManager.getStory(caseDataId, id, k -> dataManager.getStory(caseDataId, id));
  }

  /** @return all times articles */
  public List<Story> getAllAricles(String caseDataId) {
    List<Story> sorted =
        Lists.newArrayList(dataManager.queryStoryByType(Story.TIMES_ARTICLE, caseDataId));
    Collections.sort(sorted, new Comparator<Story>() {
      @Override
      public int compare(Story o1, Story o2) {
        return o1.getId().compareTo(o2.getId());
      }
    });
    return sorted;
  }

  public Set<String> getStreets() {
    return streets;
  }

  public Set<String> getLocations() {
    return locations;
  }

  public List<Hint> getAllHints(String caseDataId) {
    return dataManager.queryAllHints(caseDataId);
  }

  public Clue getClue(String caseDataId, String clueid) {
    return dataManager.getClue(caseDataId, clueid);
  }

  public Question getQuestion(String caseDataId, int index) {
    return dataManager.getQuestion(caseDataId, index);
  }

  /** @return number of questions */
  public int getQuestionsSize(String caseDataId) {
    return dataManager.getQuestionsSize(caseDataId);
  }

  public List<Story> getAllStories(String caseDataId) {
    return dataManager.queryAllStories(caseDataId);
  }

  /** @return all possible clues from database */
  public List<Clue> getAllClues(String caseDataId) {
    return dataManager.queryAllClues(caseDataId);
  }

  public Hint getHint(String caseDataId, String hintid) {
    return dataManager.getHint(caseDataId, hintid);
  }

  public List<DirectoryEntry> searchDir(String caseDataId, String query) {
    return searchManager.searchDir(caseDataId, query);
  }

  public List<Clue> searchClues(String caseDataId, String query, Session session) {
    Collection<String> ids = searchManager.searchCluesIds(caseDataId, query);
    List<Clue> result = new ArrayList<>();
    for (String id : ids) {
      if (session.getClues().contains(id)) {
        result.add(dataManager.getClue(caseDataId, id));
      }
    }
    return result;
  }

  public String getCaseDataId(Session session) {
    String caseId = session.getCaseid();
    if (caseId == null) {
      return caseId;
    }
    return getCaseDataId(caseId);
  }

  public String getCaseDataId(String caseId) {
    Case c = getCase(caseId);
    if (c == null) {
      return null;
    }
    return c.getCaseDataId();
  }

  public CaseData getCaseData(String caseDataId) {
    return dataManager.getCaseData(caseDataId);
  }

  public Case getCase(String caseId) {
    Case c = cacheManager.getCase(caseId, k -> dataManager.getCase(caseId));
    if (c == null || Strings.isNullOrEmpty(c.getCaseDataId())) {
      return null;
    }
    return c;
  }

  Comparator<Case> caseComparator = new Comparator<Case>() {
    public int compare(Case c1, Case c2) {
      return c1.getCaseId().compareTo(c2.getCaseDataId());
    };
  };

  public List<Case> getAllEnabledCases() {
    return dataManager.queryAllCases().stream().filter(c -> c.isEnabled()).sorted(caseComparator)
        .collect(Collectors.toList());
  }
}
