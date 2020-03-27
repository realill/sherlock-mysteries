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

import static org.mockito.Matchers.eq;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalSearchServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.common.collect.ImmutableList;
import com.google.common.truth.Truth;
import com.google.mystery.actions.TestUtil;
import com.google.mystery.config.SherlockConfig;
import com.google.mystery.data.DataManager;
import com.google.mystery.data.model.Case;
import com.google.mystery.data.model.Clue;
import com.google.mystery.data.model.Session;
import com.google.mystery.data.model.SessionBuilder;

@RunWith(MockitoJUnitRunner.class)
public class AssetsManagerTest {
  private static String CASE_DATA_ID = "caseDataId";
  private final LocalServiceTestHelper helper = new LocalServiceTestHelper(
      new LocalDatastoreServiceTestConfig(), new LocalSearchServiceTestConfig());
  @Mock
  DataManager dataManager;
  @InjectMocks
  AssetsManager assetsManager;
  @Mock
  SearchManager searchManager;
  @Spy
  SherlockConfig config;

  @Before
  public void setUp() {
    helper.setUp();
  }

  @After
  public void tearDown() {
    helper.tearDown();
  }

  Clue portrait =
      new Clue("caseDataId", "portrait", "Portrait", "some portrait", "queen victoria", null);
  Clue note1 = new Clue("caseDataId", "note1", "First Note", "some clue", "1 1st", null);
  Clue note2 = new Clue("caseDataId", "note2", "Second Note", "some clue", "2 2nd", null);
  Clue note3 = new Clue("caseDataId", "note3", "Third Note", "some clue", "3 3rd", null);

  @Test
  public void getAllEnabledCases() {
    Case case1 = TestUtil.newCase("caseid1", true);
    Case case2 = TestUtil.newCase("caseid2", false);
    Case case3 = TestUtil.newCase("caseid3", true);
    Mockito.when(dataManager.queryAllCases()).thenReturn(ImmutableList.of(case1, case2, case3));
    Truth.assertThat(assetsManager.getAllEnabledCases()).containsExactly(case1, case3);
  }

  @Test
  public void searchClues() {
    Mockito.when(searchManager.searchCluesIds(eq(CASE_DATA_ID), eq("query")))
        .thenReturn(ImmutableList.of("portrait", "note2"));
    Mockito.when(dataManager.getClue(eq(CASE_DATA_ID), eq("portrait"))).thenReturn(portrait);
    Mockito.when(dataManager.getClue(eq(CASE_DATA_ID), eq("note2"))).thenReturn(note2);
    Session session = new SessionBuilder("sessionid").addClue("portrait").addClue("note2").build();
    Truth.assertThat(assetsManager.searchClues(CASE_DATA_ID, "query", session))
        .containsExactly(portrait, note2);
  }

  @Test
  public void checkLocation() throws Exception {
    Mockito.when(dataManager.getConfig(AssetsManager.LOCATIONS_CONFIG_KEY))
        .thenReturn(ImmutableList.of("location one", "location two", "pub"));
    Mockito.when(dataManager.getConfig(AssetsManager.STREETS_CONFIG_KEY))
        .thenReturn(ImmutableList.of("first avenue", "second avenue", "my street"));
    assetsManager.reloadLocations();

    Truth.assertThat(assetsManager.checkLocation("location one")).isEqualTo("location one");
    Truth.assertThat(assetsManager.checkLocation(" locaTion   Two ")).isEqualTo("location two");
    Truth.assertThat(assetsManager.checkLocation("unknown location")).isNull();

    Truth.assertThat(assetsManager.checkLocation("21 first avenue")).isEqualTo("21 first avenue");
    Truth.assertThat(assetsManager.checkLocation("21c My Street ")).isEqualTo("21c my street");
    Truth.assertThat(assetsManager.checkLocation("21 c My Street ")).isEqualTo("21c my street");

    Truth.assertThat(assetsManager.checkLocation("second Avenue 221b"))
        .isEqualTo("221b second avenue");
    Truth.assertThat(assetsManager.checkLocation("second Avenue 221  b"))
        .isEqualTo("221b second avenue");
  }

  @Test
  public void checkLocation_upperCase() throws Exception {
    Mockito.when(dataManager.getConfig(AssetsManager.LOCATIONS_CONFIG_KEY))
        .thenReturn(ImmutableList.of());
    Mockito.when(dataManager.getConfig(AssetsManager.STREETS_CONFIG_KEY))
        .thenReturn(ImmutableList.of("Euston Road"));
    assetsManager.reloadLocations();

    Truth.assertThat(assetsManager.checkLocation("39C Euston Road")).isEqualTo("39c euston road");
    Truth.assertThat(assetsManager.checkLocation("39C EUSTON ROAD")).isEqualTo("39c euston road");
  }
}
