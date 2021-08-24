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

import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalSearchServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.truth.Truth;
import com.google.mystery.config.SherlockConfig;
import com.google.mystery.data.model.DirectoryEntry;
import com.google.mystery.data.model.Hint;
import com.google.mystery.data.model.Story;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {GeneralTestingConfig.class})
public class AssertsImportIntegrationTest {
  private static final String CASE_DATA_ID = "caseDataId";

  private static final LocalServiceTestHelper helper = new LocalServiceTestHelper(
      new LocalDatastoreServiceTestConfig(), new LocalSearchServiceTestConfig());

  @Inject
  LongOperationsManager assetsImportManager;

  @Inject
  AssetsManager assetsManager;
  @Inject
  SherlockConfig config;

  @BeforeClass
  public static void setup() {
    helper.setUp();
  }

  @AfterClass
  public static void tearDown() throws InterruptedException, ExecutionException {
    helper.tearDown();
  }

  private boolean wasDataImported = false;

  @Before
  public void init() throws Exception {
    if (!wasDataImported) {
      assetsImportManager.importGeneralAssets(Mockito.mock(HttpRequestInitializer.class),
          "base-data");
      assetsImportManager.importCaseAssets(Mockito.mock(HttpRequestInitializer.class), "test-case",
          CASE_DATA_ID);
      wasDataImported = true;
    }
  }

  @Test
  public void streetAndLocation() {
    Truth.assertThat(assetsManager.checkLocation("201 test street")).isEqualTo("201 test street");
    Truth.assertThat(assetsManager.checkLocation("Buckingham Palace"))
        .isEqualTo("buckingham palace");
  }

  @Test
  public void directory() {
    assertDirectorySearch("bank", "Barclays", "25 lombard street", "Bank");
    assertDirectorySearch("Stephen Cook", "Stephen Cook", "101A Regent Street");
    assertDirectorySearch("Smith", "William Smith", "15 Oxford Street");
  }

  @Test
  public void stories() {
    assertStory("caseIntroduction", "Test Case Introduction", "You are welcomed", Story.SIMPLE,
        ImmutableList.of("map", "newspaper"));
    assertStory("timesArticle2", "Crime", "was robbed", Story.TIMES_ARTICLE, ImmutableList.of());
    assertStory("timesArticle2", "Crime", "was robbed", Story.TIMES_ARTICLE, ImmutableList.of());
    assertStory("buckingham palace", "Buckingham Palace", "we got approached", Story.LOCATION,
        ImmutableList.of());
  }

  @Test
  public void hints() {
    Truth.assertThat(assetsManager.getAllHints(CASE_DATA_ID).size()).isEqualTo(3);
    Hint hint2 = assetsManager.getAllHints(CASE_DATA_ID).get(1);
    Truth.assertThat(hint2.getHint()).isEqualTo(
        "You can look through clues at any moment of investigation. They also will be available on you game log page");
    Truth.assertThat(hint2.getPrecondition()).isEqualTo(ImmutableSet.of("4 whitehall place"));
  }

  @Test
  public void hints_suggestions() {
    Hint hint1 = assetsManager.getAllHints(CASE_DATA_ID).get(0);
    Truth.assertThat(hint1.getSuggestions()).containsExactly("suggestion1", "suggestion2");
  }

  protected void assertStory(String id, String title, String subtext, String type,
      List<String> clues) {
    Story story = assetsManager.getStory(CASE_DATA_ID, id);
    Truth.assertThat(story).isNotNull();
    Truth.assertThat(story.getId()).isEqualTo(id);
    Truth.assertThat(story.getTitle()).isEqualTo(title);
    Truth.assertThat(story.getText()).contains(subtext);
    Truth.assertThat(story.getType()).isEqualTo(type);
    Truth.assertThat(story.getClues()).containsExactlyElementsIn(clues);
  }

  protected void assertDirectorySearch(String query, String name, String address) {
    assertDirectorySearch(query, name, address, null);
  }

  protected void assertDirectorySearch(String query, String name, String address, String category) {
    Truth.assertThat(assetsManager.searchDir(CASE_DATA_ID, query)).hasSize(1);
    DirectoryEntry entry = assetsManager.searchDir(CASE_DATA_ID, query).get(0);
    Truth.assertThat(entry.getName()).isEqualTo(name);
    Truth.assertThat(entry.getLocation().toLowerCase()).isEqualTo(address.toLowerCase());
  }
}
