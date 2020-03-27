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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import com.google.appengine.api.search.Document;
import com.google.appengine.api.search.Field;
import com.google.appengine.api.search.GetRequest;
import com.google.appengine.api.search.Index;
import com.google.appengine.api.search.IndexSpec;
import com.google.appengine.api.search.Query;
import com.google.appengine.api.search.QueryOptions;
import com.google.appengine.api.search.Results;
import com.google.appengine.api.search.ScoredDocument;
import com.google.appengine.api.search.SearchException;
import com.google.appengine.api.search.SearchServiceFactory;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.mystery.config.SherlockConfig;
import com.google.mystery.data.model.Clue;
import com.google.mystery.data.model.DirectoryEntry;

/**
 * Handles search across various cases and data.
 *
 * @author ilyaplatonov
 */
public class SearchManager {
  @Inject SherlockConfig config;
  /** excluded during search. */
  private static Set<String> MEANINGLESS_WORDS =
      ImmutableSet.of("a", "the", "find", "search", "at", "to");

  private Logger logger = Logger.getLogger(this.getClass().getName());
  /** indexes used by this object. */
  private Map<String, Index> indexes = new HashMap<>();

  public List<DirectoryEntry> searchDir(String caseDataId, String query) {
    query = escapeQuery(normalize(query));
    if (query.isEmpty()) {
      return ImmutableList.of();
    }
    QueryOptions options =
        QueryOptions.newBuilder().setLimit(20).setFieldsToSnippet("location", "name").build();
    ImmutableList.Builder<DirectoryEntry> resultBuilder = ImmutableList.builder();
    try {
      Query q = Query.newBuilder().setOptions(options).build(query);
      Set<DirectoryEntry> entries = new LinkedHashSet<>();

      Iterators.addAll(
          entries,
          getCaseDirectory(caseDataId)
              .search(q)
              .getResults()
              .stream()
              .map(d -> toDirectoryEntry(d))
              .iterator());
      Iterators.addAll(
          entries,
          getGeneralDirectory()
              .search(q)
              .getResults()
              .stream()
              .map(d -> toDirectoryEntry(d))
              .iterator());
      resultBuilder.addAll(
          entries
              .stream()
              .sorted((d1, d2) -> d1.getName().compareTo(d2.getName()))
              .limit(10)
              .iterator());

    } catch (SearchException e) {
      logger.log(Level.SEVERE, "Search error", e);
    }
    return resultBuilder.build();
  }

  protected static DirectoryEntry toDirectoryEntry(ScoredDocument doc) {
    String location = doc.getOnlyField("location").getAtom();
    String name = doc.getOnlyField("name").getText();
    return new DirectoryEntry(location, name);
  }

  public List<String> searchCluesIds(String caseDataId, String query) {
    query = escapeQuery(normalize(query));
    ImmutableList.Builder<String> resultBuilder = ImmutableList.builder();
    try {
      QueryOptions options = QueryOptions.newBuilder().setLimit(20).setFieldsToSnippet().build();
      Query q = Query.newBuilder().setOptions(options).build(query);

      Results<ScoredDocument> searchResult = getCaseClues(caseDataId).search(q);

      for (ScoredDocument doc : searchResult) {
        resultBuilder.add(doc.getId());
      }
    } catch (SearchException e) {
      logger.log(Level.SEVERE, "Search error", e);
    }
    return resultBuilder.build();
  }

  public void addClues(String caseDataId, Collection<Clue> clues) {
    addClues(getCaseClues(caseDataId), clues);
  }

  private void addClues(Index index, Collection<Clue> clues) {
    for (Clue clue : clues) {
      Document.Builder doc = Document.newBuilder();
      doc.setId(clue.getId());
      doc.addField(Field.newBuilder().setName("name").setText(clue.getName()));
      if (clue.getKeywords() != null) {
        doc.addField(Field.newBuilder().setName("keywords").setText(clue.getKeywords()));
      }
      index.put(doc);
    }
  }

  private void clearIndex(Index index) {
    while (true) {
      List<Document> docs =
          index
              .getRange(GetRequest.newBuilder().setReturningIdsOnly(true).setLimit(10).build())
              .getResults();
      if (docs.isEmpty()) {
        break;
      }
      List<String> ids = new ArrayList<>();
      for (Document doc : docs) {
        ids.add(doc.getId());
      }
      index.deleteAsync(ids);
    }
  }

  /** putting all words into quotes for searching */
  public static String escapeQuery(String string) {
    string = string.replaceAll("\"", "");
    List<String> result = Lists.newArrayList();

    for (String word : Splitter.on(" ").omitEmptyStrings().split(string)) {
      result.add("\"" + word + "\"");
    }
    return Joiner.on(" ").join(result);
  }

  protected Index getIndex(String name) {
    if (indexes.containsKey(name)) {
      return indexes.get(name);
    }
    synchronized (this) {
      if (indexes.containsKey(name)) {
        return indexes.get(name);
      }
      Index index =
          SearchServiceFactory.getSearchService()
              .getIndex(IndexSpec.newBuilder().setName(name).build());
      indexes.put(name, index);
      return index;
    }
  }
  /** @return directory for all cases */
  protected Index getCaseClues(String caseDataId) {
    return getIndex("CaseClues-" + caseDataId);
  }

  /** @return directory for all cases */
  protected Index getCaseDirectory(String caseDataId) {
    return getIndex("CaseDirectory-" + caseDataId);
  }

  /** @return directory for all cases */
  protected Index getGeneralDirectory() {
    return getIndex("GeneralDirectory");
  }

  private void addDirectoryEntries(Index index, Collection<DirectoryEntry> dirs) {
    for (DirectoryEntry entry : dirs) {
      Document.Builder doc = Document.newBuilder();
      doc.setId(UUID.randomUUID().toString());
      doc.addField(Field.newBuilder().setName("location").setAtom(entry.getLocation()));
      doc.addField(Field.newBuilder().setName("name").setText(entry.getName()));
      doc.addField(Field.newBuilder().setName("nameNorm").setText(normalize(entry.getName())));

      if (entry.getKeywords() != null) {
        doc.addField(Field.newBuilder().setName("keywords").setText(entry.getKeywords()));
        doc.addField(
            Field.newBuilder().setName("keywordsNorm").setText(normalize(entry.getKeywords())));
      }
      if (entry.getCategory() != null) {
        doc.addField(Field.newBuilder().setName("category").setText(entry.getCategory()));
        doc.addField(
            Field.newBuilder().setName("categoryNorm").setText(normalize(entry.getCategory())));
      }
      index.put(doc);
    }
  }

  public void addDirectoryEntries(String caseDataId, Collection<DirectoryEntry> dirs) {
    addDirectoryEntries(getCaseDirectory(caseDataId), dirs);
  }

  public void addGeneralDirectoryEntries(Collection<DirectoryEntry> dirs) {
    addDirectoryEntries(getGeneralDirectory(), dirs);
  }

  /** Normalizing string for searching */
  public static String normalize(String string) {
    string = string.replaceAll("&", " and ");
    string = string.replaceAll("'", "");
    string = string.replaceAll(",", " ");
    string = string.replaceAll("\\.", " ");
    string = string.replaceAll("\\bCo\\b", " company ");
    string = string.replaceAll("\\bLTD\\b", " limited ");
    // non-break space
    string = string.replaceAll(String.valueOf('\u00A0'), " ");
    List<String> result = Lists.newArrayList();

    for (String word : Splitter.on(" ").omitEmptyStrings().split(string)) {
      if (!MEANINGLESS_WORDS.contains(word.toLowerCase())) {
        result.add(word);
      }
    }
    return Joiner.on(" ").join(result);
  }

  public void clearGeneralDirectory() {
    clearIndex(getGeneralDirectory());
  }

  public void clearClues(String caseDataId) {
    clearIndex(getCaseClues(caseDataId));
  }

  public void clearDirectory(String caseDataId) {
    clearIndex(getCaseDirectory(caseDataId));
  }
}
