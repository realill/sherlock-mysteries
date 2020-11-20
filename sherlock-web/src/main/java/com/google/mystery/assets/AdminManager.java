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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.inject.Inject;
import org.apache.commons.math3.distribution.NormalDistribution;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.mystery.data.CacheManager;
import com.google.mystery.data.DataManager;
import com.google.mystery.data.model.Case;
import com.google.mystery.data.model.CaseData;
import com.google.mystery.data.model.Clue;
import com.google.mystery.data.model.DirectoryEntry;
import com.google.mystery.data.model.Story;
import freemarker.log.Logger;

public class AdminManager {
  @Inject
  private DataManager dataManager;
  @Inject
  private SearchManager searchManager;
  @Inject
  private AssetsManager assetsManager;
  @Inject
  private CacheManager cacheManager;

  static private NormalDistribution nd = new NormalDistribution();
  static private Random random = new Random();

  static private Logger logger = Logger.getLogger(AdminManager.class.getName());

  public void updateCase(Case newCase) throws IOException {
    if (!Strings.isNullOrEmpty(newCase.getCaseDataId())) {
      if (dataManager.getCaseData(newCase.getCaseDataId()) == null) {
        throw new IOException("There are no case data caseDataId=" + newCase.getCaseDataId());
      }
    }
    dataManager.addCase(newCase);
    cacheManager.invalidateCase(newCase.getCaseId());
  }

  public List<Case> getAllCases() {
    return dataManager.queryAllCases();
  }

  public Case getCase(String caseId) {
    return dataManager.getCase(caseId);
  }

  public void deleteCase(String caseId) {
    dataManager.deleteCase(caseId);
  }

  public List<CaseData> getAllCaseData() {
    return dataManager.queryAllCaseData();
  }

  public void createCaseData(String caseDataId) throws IOException {
    if (dataManager.getCaseData(caseDataId) == null) {
      dataManager.updateCaseData(new CaseData(caseDataId));
    } else {
      throw new IOException("caseDataId=" + caseDataId + " already exist");
    }
  }

  public void deleteCaseData(String caseDataId) throws IOException {
    for (Case c : dataManager.queryAllCases()) {
      if (c.getCaseDataId().equals(caseDataId)) {
        throw new IOException("Case caseId=" + c.getCaseId() + " dependent on this case data");
      }
    }
    searchManager.clearDirectory(caseDataId);
    searchManager.clearClues(caseDataId);
    dataManager.clearHints(caseDataId);
    dataManager.clearClues(caseDataId);
    dataManager.clearQuestions(caseDataId);
    dataManager.clearStories(caseDataId);
    dataManager.deleteCaseData(caseDataId);
  }

  /** @return all article for all existing cases */
  public List<Story> getGlobalAllAricles() {
    List<Story> articles = new ArrayList<>();
    for (Case c : dataManager.queryAllCases()) {
      if (!Strings.isNullOrEmpty(c.getCaseDataId())) {
        articles.addAll(dataManager.queryStoryByType(Story.TIMES_ARTICLE, c.getCaseDataId()));
      }
    }

    return articles;
  }

  /** all clues for all existing cases. */
  public Collection<Clue> getGlobalAllClues() {
    List<Clue> clues = new ArrayList<>();
    for (Case c : dataManager.queryAllCases()) {
      if (!Strings.isNullOrEmpty(c.getCaseDataId())) {
        clues.addAll(dataManager.queryAllClues(c.getCaseDataId()));
      }
    }
    return clues;
  }

  private static List<String> lastNames = ImmutableList.of();
  private static List<String> likelyLastNames = ImmutableList.of();
  private static List<String> otherLastNames = ImmutableList.of();
  private static List<String> boyNames = ImmutableList.of();
  private static List<String> girlNames = ImmutableList.of();
  private static List<String> numberPostfixes =
      Arrays.asList("", "", "", "", "", "", "", "A", "B", "C", "D");
  static {
    try {
      lastNames = Files.readAllLines(Paths.get("WEB-INF/data/last-names.txt"));
      boyNames = Files.readAllLines(Paths.get("WEB-INF/data/boy-names.txt"));
      girlNames = Files.readAllLines(Paths.get("WEB-INF/data/girl-names.txt"));
      
      likelyLastNames = lastNames.subList(0, 50);
      otherLastNames = lastNames.subList(50, lastNames.size());
    } catch (IOException e) {
      logger.error("Error initializing data", e);
    }
  }

  public List<DirectoryEntry> genereteDirectory() {
    List<String> streets = new ArrayList<>(assetsManager.getStreets());
    List<DirectoryEntry> result = new ArrayList<>();

    for (int i = 0; i < 500; i++) {
      String firstName;
      /// 10% are females
      if (random.nextInt(10) == 0) {
        firstName = normalDistribution(girlNames);
      } else {
        firstName = normalDistribution(boyNames);
        // 1% of men are Sirs.
        if (random.nextInt(100) == 0) {
          firstName = "Sir " + firstName;
        } else if (random.nextInt(200) == 0) {
          // 0.5% are Lords
          firstName = "Lord " + firstName;
        }
      }
      normalDistribution(boyNames);
      // Using first 50 names as more likely names.
      String lastName = normalDistribution(likelyLastNames);
      // 5/6 chance to get other name.
      if (random.nextInt(6) > 0) {
        // other names are in linear distribution
        lastName = linearDistr(otherLastNames);
      }
      String street = linearDistr(streets);
      String number = (normalIndex(200, 2.5d) + 1) + linearDistr(numberPostfixes);
      DirectoryEntry entry = new DirectoryEntry(number + " " + street, firstName + " " + lastName);
      result.add(entry);
    }
    return result;
  }

  private String normalDistribution(List<String> list) {
    int index = normalIndex(list.size(), 2);

    return list.get(index);
  }

  private int normalIndex(int size, double max) {
    double sample;
    do {
      sample = Math.abs(nd.sample());
    } while (sample >= max);
    int index = (int) Math.floor(sample / (max / size));
    return index;
  }

  private String linearDistr(List<String> list) {
    int index = random.nextInt(list.size());
    return list.get(index);
  }

  public void saveConfig(Map<String, String> config) {
    dataManager.saveConfigMap(config);
    cacheManager.invalidateConfig();
  }
}
