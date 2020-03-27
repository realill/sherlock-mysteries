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

import java.util.concurrent.ExecutionException;
import javax.inject.Inject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalSearchServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.truth.Truth;
import com.google.mystery.data.DataManager;
import com.google.mystery.data.model.Clue;
import com.google.mystery.data.model.DirectoryEntry;
import com.google.mystery.data.model.Hint;
import com.google.mystery.data.model.Question;
import com.google.mystery.data.model.Story;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {AdminManagerTestingConfig.class})
public class AdminManagerIntegrationTest {
  private static final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(
          new LocalDatastoreServiceTestConfig(), new LocalSearchServiceTestConfig());

  private static String CASE_DATA_ID = "caseDataId";
  @Inject DataManager dataManager;
  @Inject SearchManager searchManager;
  @Inject AdminManager adminManager;

  @BeforeClass
  public static void setup() {
    helper.setUp();
  }

  @AfterClass
  public static void tearDown() throws InterruptedException, ExecutionException {
    helper.tearDown();
  }

  @Test
  public void deleteCaseData_cleansAll() throws Exception {
    adminManager.createCaseData(CASE_DATA_ID);
    dataManager.addClue(new Clue(CASE_DATA_ID, "id", "name", "description", "keywords", null));
    dataManager.addHint(
        new Hint(CASE_DATA_ID, ImmutableSet.of(), "hint", "id", ImmutableList.of()));
    dataManager.addQuestion(new Question(CASE_DATA_ID, "question", "answer", 1, 1));
    dataManager.addStory(
        Story.simple(
            CASE_DATA_ID, "id", "title", "text", null, ImmutableList.of(), "latlong", null));
    searchManager.addClues(CASE_DATA_ID, dataManager.queryAllClues(CASE_DATA_ID));
    searchManager.addDirectoryEntries(
        CASE_DATA_ID, ImmutableList.of(new DirectoryEntry("location", "name")));

    adminManager.deleteCaseData(CASE_DATA_ID);
    Truth.assertThat(dataManager.queryAllCaseData()).isEmpty();
    Truth.assertThat(dataManager.queryAllClues(CASE_DATA_ID)).isEmpty();
    Truth.assertThat(dataManager.queryAllHints(CASE_DATA_ID)).isEmpty();
    Truth.assertThat(dataManager.queryAllStories(CASE_DATA_ID)).isEmpty();
    Truth.assertThat(dataManager.getQuestionsSize(CASE_DATA_ID)).isEqualTo(0);

    Truth.assertThat(searchManager.searchCluesIds(CASE_DATA_ID, "name")).isEmpty();
    Truth.assertThat(searchManager.searchDir(CASE_DATA_ID, "name")).isEmpty();
  }
}
