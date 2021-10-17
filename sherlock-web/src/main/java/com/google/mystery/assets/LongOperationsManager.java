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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.util.Lists;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.mystery.actions.model.SherlockResponse;
import com.google.mystery.assets.GSuiteDataSource.DocEntity;
import com.google.mystery.config.SherlockConfig;
import com.google.mystery.data.DataManager;
import com.google.mystery.data.model.Clue;
import com.google.mystery.data.model.DirectoryEntry;
import com.google.mystery.data.model.Hint;
import com.google.mystery.data.model.Question;
import com.google.mystery.data.model.Story;
import com.google.mystery.data.model.StoryData;
import com.sherlockmysteries.pdata.SMDataManager;

/**
 * Importing of case assets from google sheet logic
 *
 * @author ilyaplatonov
 */
@Singleton
public class LongOperationsManager {
  private static final String STORIES_DOC_METADATA = "storiesDoc";
  @Inject private IImportDataSource client;
  @Inject private DataManager dataManager;
  @Inject private ExecutorService executorService;
  @Inject private AssetsManager assetsManager;
  @Inject private StorageManager voicesManager;
  @Inject private SearchManager searchManager;
  @Inject private StorageManager storageManager;
  @Inject private SherlockConfig config;
  @Inject private SMDataManager smDataManager;

  private final Logger logger = Logger.getLogger(this.getClass().getName());
  private StringWriter latestWriter = new StringWriter();

  {
    latestWriter.write("Import never started\n");
  }

  public void exportSMData(String caseId) {
    final PrintWriter printWriter = createWriter();
    String bucketName = config.getBucketName();
    executorService.submit(
        () -> {
          try {
            smDataManager.generateAndUpload(caseId, bucketName, printWriter);
          } catch (Exception e) {
            e.printStackTrace(printWriter);
            logger.log(Level.SEVERE, "Error running import task", e);
          }
        });
  }

  private void runImport(
      String bucketName,
      HttpRequestInitializer credential,
      String urlOrId,
      PrintWriter logWriter,
      String caseDataId)
      throws IOException {
    Map<String, String> metadata = importCaseDataMetadata(credential, urlOrId);
    String caseId = metadata.get("caseId");
    if (caseId == null) {
      logWriter.println("Can not find caseId metadata. Aborting.");
      return;
    }
    logWriter.println("caseId=" + caseId);
    logWriter.println("Clearing stories");
    dataManager.clearStories(caseDataId);
    if (metadata.get(STORIES_DOC_METADATA) == null) {
      logWriter.println("Import Stories from Stories Sheet");
      importStories(bucketName, logWriter, credential, urlOrId, caseDataId, caseId, config);
    } else {
      logWriter.println("Import Stories from " + metadata.get(STORIES_DOC_METADATA));
      importDocStories(
          bucketName,
          logWriter,
          credential,
          metadata.get(STORIES_DOC_METADATA),
          caseDataId,
          caseId);
    }
    logWriter.println("Clear diretory");
    searchManager.clearDirectory(caseDataId);
    logWriter.println("Starting directory import");
    Collection<DirectoryEntry> entries = importDirectory(logWriter, credential, urlOrId);
    searchManager.addDirectoryEntries(caseDataId, entries);
    logWriter.println("Import directory finished");
    logWriter.println("Clearing clues");
    searchManager.clearClues(caseDataId);
    logWriter.println("Starting clues import");
    Collection<Clue> clues =
        importClues(bucketName, logWriter, credential, urlOrId, caseDataId, caseId);
    searchManager.addClues(caseDataId, clues);

    logWriter.println("Clearing questions");
    dataManager.clearQuestions(caseDataId);
    logWriter.println("Starting questions import");
    importQuestions(logWriter, credential, urlOrId, caseDataId);

    logWriter.println("Clearing hints");
    dataManager.clearHints(caseDataId);
    logWriter.println("Starting hints import");
    importHints(logWriter, credential, urlOrId, caseDataId);
    logWriter.println("Import is finished!");
  }

  private void importHints(
      PrintWriter logWriter, HttpRequestInitializer credential, String urlOrId, String caseDataId)
      throws IOException {
    String range = "Hints!A1:C";
    List<List<Object>> values = client.getData(credential, urlOrId, range);
    if (values == null || values.size() < 1) {
      logWriter.println("No data found for Hints. Aborting");
    } else {
      values = values.subList(1, values.size());
      int counter = 0;
      for (List<Object> row : values) {
        if (row.size() < 2) {
          logWriter.println("Expecting at least 2 collumns in Hints " + row);
        } else {
          String precoditionString = row.get(0).toString().trim();
          String hint = row.get(1).toString();
          List<String> preconditionOriginal =
              Splitter.on(",").omitEmptyStrings().trimResults().splitToList(precoditionString);
          ImmutableSet.Builder<String> pBuilder = ImmutableSet.builder();
          for (String pre : preconditionOriginal) {
            String location = assetsManager.checkLocation(pre);
            if (location != null) {
              pBuilder.add(location);
            } else {
              logWriter.println(String.format("hint precondition has invalid element: %s", pre));
            }
          }
          List<String> suggestions = ImmutableList.of();
          if (row.size() > 2) {
            suggestions =
                Lists.newArrayList(Splitter.on(",").trimResults().split(row.get(2).toString()));
            for (ListIterator<String> i = suggestions.listIterator(); i.hasNext(); ) {
              String suggestion = i.next();
              if (suggestion.length() > 20) {
                logWriter.printf(
                    "Error: %s suggestion is too long limit is 20 chars\n", suggestion);
                i.remove();
              }
            }
          }
          String id = String.format("hint%03d", counter);
          dataManager.addHint(new Hint(caseDataId, pBuilder.build(), hint, id, suggestions));
          counter++;
        }
      }
      logWriter.printf("%s hints are imported\n", counter);
    }
  }

  private void importQuestions(
      PrintWriter logWriter, HttpRequestInitializer credential, String urlOrId, String caseDataId)
      throws IOException {
    String range = "Questions!A1:D";
    List<List<Object>> values = client.getData(credential, urlOrId, range);
    if (values == null || values.size() < 1) {
      logWriter.println("No data found for Questions. Aborting");
    } else {
      values = values.subList(1, values.size());
      int counter = 0;
      for (List<Object> row : values) {
        if (row.size() < 3) {
          logWriter.println("Expecting at least 3 collumns in Texts " + row);
        } else {
          String question = row.get(0).toString();
          String answer = row.get(1).toString();
          int score = Integer.parseInt(row.get(2).toString());
          List<String> possibleAnswers = ImmutableList.of();
          if (row.size() >= 4) {
            possibleAnswers = Splitter.on("\n").trimResults().splitToList(row.get(3).toString());
          }
          dataManager.addQuestion(
              new Question(caseDataId, question, answer, score, counter, possibleAnswers));
          counter++;
        }
      }
      logWriter.printf("%s questions are imported\n", counter);
    }
  }

  private void runImportGeneral(
      HttpRequestInitializer credential, String urlOrId, PrintWriter logWriter) throws IOException {
    logWriter.println("Reading Streets");
    List<String> streets = readAsList(credential, urlOrId, "Streets");
    assetsManager.putStreets(streets);
    logWriter.println("Reading Locations");
    List<String> locations = readAsList(credential, urlOrId, "Locations");
    assetsManager.putLocations(locations);
    logWriter.println(
        String.format("Loaded %s streets and %s locations", streets.size(), locations.size()));
    logWriter.println("Clearing general directory");
    searchManager.clearGeneralDirectory();
    logWriter.println("Starting directory import");
    Collection<DirectoryEntry> entries = importDirectory(logWriter, credential, urlOrId);
    searchManager.addGeneralDirectoryEntries(entries);
    logWriter.println("General import finished");
  }

  private List<String> readAsList(HttpRequestInitializer credential, String urlOrId, String string)
      throws IOException {
    String range = string + "!A1:A";
    List<List<Object>> values = client.getData(credential, urlOrId, range);
    return values
        .stream()
        .filter(l -> !l.isEmpty())
        .map(l -> l.get(0).toString())
        .collect(Collectors.toList());
  }

  /** Importing Businesses page */
  private List<DirectoryEntry> importDirectory(
      PrintWriter logWriter, HttpRequestInitializer credential, String urlOrId) throws IOException {
    List<DirectoryEntry> result = new ArrayList<>();
    String range = "Directory!A1:C";
    List<List<Object>> values = client.getData(credential, urlOrId, range);
    if (values == null || values.size() == 0) {
      logWriter.println("No data found in Businesses!. Aborting");
    } else {
      String category = null;
      for (List<Object> row : values) {
        if (row.size() == 0) {
          // ignore
        } else if (row.size() == 1) {
          category = row.get(0).toString();
        } else {
          String name = row.get(0).toString();
          String location = assetsManager.checkLocation(row.get(1).toString());
          if (location == null) {
            logWriter.println(
                "Invalid location " + row.get(1).toString() + " for " + name + "ignoring");
            continue;
          }

          String keywords = null;
          if (row.size() > 2) {
            keywords = row.get(2).toString();
          }
          DirectoryEntry de = new DirectoryEntry(location, name, category, keywords);
          result.add(de);
        }
      }
      logWriter.printf("%s bussinesses imported\n", result.size());
    }
    return result;
  }

  private void importDocStories(
      String bucketName,
      PrintWriter logWriter,
      HttpRequestInitializer credential,
      String urlOrId,
      String caseDataId,
      String caseId) {
    if (!(client instanceof GSuiteDataSource)) {
      throw new IllegalArgumentException("GSuiteDatasource required for doc stories import.");
    }
    List<String> simpleStoryIds = new ArrayList<>();
    List<String> noLatLongIds = new ArrayList<>();
    List<StoryData> storyData = new ArrayList<>();
    try {
      for (GSuiteDataSource.DocEntity entity :
          ((GSuiteDataSource) client).loadDocsData(credential, urlOrId)) {
        StoryData story = docEntityToStory(entity);
        if (Strings.isNullOrEmpty(story.getId())
            || Strings.isNullOrEmpty(story.getTitle())
            || Strings.isNullOrEmpty(story.getText())) {
          logWriter.println(
              String.format(
                  "Incorrect sorry entry id=%s, title=%s, clues=%s, latlong=%s",
                  story.getId(), story.getTitle(), story.getTitle(), story.getLatlong()));
          continue;
        }
        if (!LongOperationsManager.checkTimesArticle(story.getId())
            && assetsManager.checkLocation(story.getId()) == null) {
          simpleStoryIds.add(story.getId());
        }
        if (Strings.isNullOrEmpty(story.getLatlong())) {
          noLatLongIds.add(story.getId());
        }
        storyData.add(story);
      }
      logWriter.println(
          String.format(
              "Simple stories ids: %s\n" + "Stories without latlong ids: %s",
              simpleStoryIds, noLatLongIds));
    } catch (IOException e) {
      logWriter.println("Error exporting stories: " + e.getMessage());
      logger.log(Level.WARNING, "Error exporting data from doc", e);
      return;
    }

    importStoriesFromData(bucketName, logWriter, storyData, caseId, caseDataId);
  }

  private void importStoriesFromData(
      String bucketName,
      PrintWriter logWriter,
      List<StoryData> storyData,
      String caseId,
      String caseDataId) {

    int locationsCounter = 0;
    int timesArticleCounter = 0;
    int simpleCounter = 0;
    for (StoryData data : storyData) {
      String audioUrl =
          voicesManager.getStoryMediaLink(bucketName, caseId, data.getId());
      String imageUrl = voicesManager.getStoryImageUrl(bucketName, caseId, data.getId());

      URL audioLink = null;
      URL imageLink = null;
      if (audioUrl != null) {
        try {
          audioLink = new URL(audioUrl);
        } catch (MalformedURLException e) {
          logWriter.println("error parsing audio link " + audioUrl + " ignoring it");
        }
      }
      if (imageUrl != null) {
        try {
          imageLink = new URL(imageUrl);
        } catch (MalformedURLException e) {
          logWriter.println("error parsing image link " + imageUrl + " ignoring it");
        }
      }

      List<String> clues = ImmutableList.of();
      if (data.getClues() != null) {
        clues = Splitter.on(",").trimResults().omitEmptyStrings().splitToList(data.getClues());
      }

      if (assetsManager.checkLocation(data.getId()) != null) {
        dataManager.addStory(
            Story.location(
                caseDataId,
                assetsManager.checkLocation(data.getId()),
                data.getTitle(),
                data.getText(),
                audioLink,
                clues,
                data.getLatlong(),
                imageLink));
        locationsCounter++;
      } else if (checkTimesArticle(data.getId())) {
        dataManager.addStory(
            Story.timesArticle(
                caseDataId, data.getId(), data.getTitle(), data.getText(), audioLink, imageLink));
        timesArticleCounter++;
      } else {
        dataManager.addStory(
            Story.simple(
                caseDataId,
                data.getId(),
                data.getTitle(),
                data.getText(),
                audioLink,
                clues,
                data.getLatlong(),
                imageLink));
        simpleCounter++;
      }
    }
    logWriter.printf(
        "%s locations, %s times articles and %s simple stories are imported\n",
        locationsCounter, timesArticleCounter, simpleCounter);
  }

  /** Importing stories section from stories sheet. */
  private void importStories(
      String bucketName,
      PrintWriter logWriter,
      HttpRequestInitializer credential,
      String urlOrId,
      String caseDataId,
      String caseId,
      SherlockConfig config)
      throws IOException {
    String range = "Stories!A1:E";
    List<List<Object>> values = client.getData(credential, urlOrId, range);
    List<StoryData> storiesData = new ArrayList<>();
    if (values == null || values.size() < 1) {
      logWriter.println("No data found. Aborting");
    } else {
      values = values.subList(1, values.size());

      for (List<Object> row : values) {
        if (row.size() < 3) {
          logWriter.println("Expecting at least 3 collumns in Stories " + row);
        } else {
          String id = row.get(0).toString();
          String title = row.get(1).toString();
          String text = row.get(2).toString();
          String clues = row.size() > 3 ? row.get(3).toString() : null;
          String latlong = row.size() > 4 ? row.get(4).toString() : null;
          storiesData.add(new StoryData(id, title, clues, latlong, text));
        }
      }
    }
    importStoriesFromData(bucketName, logWriter, storiesData, caseId, caseDataId);
  }

  private Collection<Clue> importClues(
      String bucketName,
      PrintWriter logWriter,
      HttpRequestInitializer credential,
      String urlOrId,
      String caseDataId,
      String caseId)
      throws IOException {
    String range = "Clues!A1:E";
    List<Clue> clues = new ArrayList<>();
    List<List<Object>> values = client.getData(credential, urlOrId, range);
    if (values == null || values.size() < 1) {
      logWriter.println("No data found for clues. Aborting");
    } else {
      values = values.subList(1, values.size());
      for (List<Object> row : values) {
        if (row.size() < 3) {
          logWriter.println("Expecting at least 3 collumns in Clues " + row);
        } else {
          String id = row.get(0).toString();
          String name = row.get(1).toString();
          String description = row.get(2).toString();
          String keywords = null;

          URL imageUrl = null;
          String imageUrlString = voicesManager.getStoryImageUrl(bucketName, caseId, id);
          if (imageUrlString != null) {
            try {
              imageUrl = new URL(imageUrlString);
            } catch (MalformedURLException e) {
              logWriter.println("Error parsing url: " + imageUrlString);
              logger.warning("Error parsing url: " + imageUrlString);
            }
          }
          if (row.size() >= 4) {
            keywords = row.get(3).toString();
          }
          if (row.size() >= 5) {
            try {
              imageUrl = new URL(row.get(4).toString());
            } catch (MalformedURLException e) {
              logWriter.println("Invalid url during clues import: " + row.get(2));
            }
          }
          Clue clue = new Clue(caseDataId, id, name, description, keywords, imageUrl);
          if (name.isEmpty() || id.isEmpty()) {
            logWriter.println("Wrong clue: " + clue);
          } else {
            dataManager.addClue(clue);
            clues.add(clue);
          }
        }
      }
      logWriter.printf("%s clues are imported\n", clues.size());
    }
    return clues;
  }

  /** @return if story is times article */
  public static boolean checkTimesArticle(String string) {
    return string.startsWith("timesArticle");
  }

  public String getLatestLog() {
    return latestWriter.toString();
  }

  public void importCaseAssets(
      final HttpRequestInitializer credential, final String urlOrId, String caseDataId)
      throws IOException {
    final PrintWriter printWriter = createWriter();
    final String bucketName = config.getBucketName();

    executorService.submit(
        () -> {
          try {
            runImport(bucketName, credential, urlOrId, printWriter, caseDataId);
          } catch (Throwable e) {
            logger.log(Level.SEVERE, "Error running import task", e);
            e.printStackTrace(printWriter);
          }
        });
  }

  public void importGeneralAssets(final HttpRequestInitializer credential, final String urlOrId)
      throws IOException {
    final PrintWriter printWriter = createWriter();

    executorService.submit(
        () -> {
          try {
            runImportGeneral(credential, urlOrId, printWriter);
          } catch (Exception e) {
            logger.log(Level.SEVERE, "Error running import task", e);
            e.printStackTrace(printWriter);
          }
        });
  }

  public PrintWriter createWriter() {
    latestWriter = new StringWriter();
    return new PrintWriter(latestWriter) {
      @Override
      public void println(String x) {
        super.println(x);
        logger.info(x);
      }
    };
  }

  public Map<String, String> importCaseDataMetadata(
      HttpRequestInitializer credential, String urlOrId) throws IOException {
    String range = "Metadata!A1:B";
    Map<String, String> metadata = new HashMap<>();
    List<List<Object>> values = client.getData(credential, urlOrId, range);
    if (values == null || values.size() < 1) {
      throw new IOException("No data found for metadata.");
    } else {
      for (List<Object> row : values) {
        if (row.size() >= 1) {
          String name = row.get(0).toString();
          String value = row.size() >= 2 ? row.get(1).toString() : "";
          metadata.put(name, value);
        }
      }
    }
    return metadata;
  }

  public static StoryData docEntityToStory(DocEntity entity) {
    // going through first content lines and looking
    List<String> lines = Splitter.on("\n").splitToList(entity.getContent());
    int consumedLines = 0;
    Map<String, String> metadata = new HashMap<>();
    for (Iterator<String> i = lines.iterator(); i.hasNext(); ) {
      String line = i.next();
      if (line.trim().isEmpty()) {
        if (!metadata.isEmpty()) {
          break;
        }
      } else if (line.contains(":") && line.length() < 256) {
        List<String> split = Splitter.on(":").trimResults().splitToList(line);
        metadata.put(split.get(0).toLowerCase(), split.get(1));
      } else {
        break;
      }
      consumedLines++;
    }
    String newContent = Joiner.on("\n").join(lines.subList(consumedLines, lines.size())).trim();

    return new StoryData(
        metadata.get("id"),
        entity.getHeader(),
        metadata.get("clues"),
        metadata.get("latlong"),
        newContent);
  }

  public void generateAudio(String caseId, Collection<Story> stories) {
    final PrintWriter printWriter = createWriter();
    final String bucketName = config.getBucketName();

    executorService.submit(
        () -> {
          try {
            printWriter.println("Starting audio import");
            for (Story story : stories) {
              printWriter.println("Generating audio for: " + story.getId());
              List<String> splittedText = splitWithLimit(story.getText(), 5000);
              if (splittedText.size() < 2) {
                String url =
                    storageManager.generateTTSAudio(
                        bucketName,
                        caseId,
                        story.getId().toLowerCase(),
                        SherlockResponse.textToSsml(story.getText()),
                        printWriter);
                printWriter.println("Generated url: " + url);
              } else {
                for (int i = 0; i < splittedText.size(); i++) {
                  String url =
                      storageManager.generateTTSAudio(
                          bucketName,
                          caseId,
                          story.getId().toLowerCase() + "-" + i,
                          SherlockResponse.textToSsml(splittedText.get(i)),
                          printWriter);
                  printWriter.println("Generated url: " + url);
                }
              }
            }
            printWriter.println("Audio generation finished.");
          } catch (Throwable e) {
            logger.log(Level.SEVERE, "Error running import task", e);
            e.printStackTrace(printWriter);
          }
        });
  }

  public static List<String> splitWithLimit(String string, int limit) {
    List<String> result = new ArrayList<>();
    StringBuilder newString = new StringBuilder();
    for (String line : Splitter.on("\n").split(string)) {
      if (newString.length() + line.length() > limit - 1 && newString.length() != 0) {
        result.add(newString.toString());
        newString = new StringBuilder();
      }

      if (newString.length() != 0) {
        newString.append("\n");
      }
      newString.append(line);
    }
    if (newString.length() > 0) {
      result.add(newString.toString());
    }
    return result;
  }
}
