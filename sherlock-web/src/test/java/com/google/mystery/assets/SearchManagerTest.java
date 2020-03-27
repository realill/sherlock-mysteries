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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalSearchServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.truth.Truth;
import com.google.mystery.config.SherlockConfig;
import com.google.mystery.data.model.Clue;
import com.google.mystery.data.model.DirectoryEntry;

@RunWith(MockitoJUnitRunner.class)
public class SearchManagerTest {
  private static final String CASE_DATA_ID = "caseDataId";
  @Spy SherlockConfig config;
  @InjectMocks SearchManager searchManager;

  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(
          new LocalDatastoreServiceTestConfig(), new LocalSearchServiceTestConfig());

  @Before
  public void setUp() {
    helper.setUp();
  }

  DirectoryEntry jasper =
      new DirectoryEntry("30EC", "Sir Jasper Meeks", "HOLMES LECTURE", "Chief Medical Examiner");
  DirectoryEntry jasperClean = clean(jasper);
  DirectoryEntry murray =
      new DirectoryEntry("22SW", "H.R. murray", "HOLMES LECTURE", "Criminologist");
  DirectoryEntry murrayClean = clean(murray);
  DirectoryEntry billJohn = new DirectoryEntry("22SW", "bill & john");
  DirectoryEntry aberdeenNavCo = new DirectoryEntry("26EC", "Aberdeen Navigation Co");
  DirectoryEntry drMorgan = new DirectoryEntry("26EC", "Doctor Dr Richard Morgan Office");

  Clue portrait =
      new Clue(CASE_DATA_ID, "portrait", "Portrait", "some portrait", "queen victoria", null);
  Clue note1 = new Clue(CASE_DATA_ID, "note1", "First Note", "some clue", "1 1st", null);
  Clue note2 = new Clue(CASE_DATA_ID, "note2", "Second Note", "some clue", "2 2nd", null);
  Clue note3 = new Clue(CASE_DATA_ID, "note3", "Third Note", "some clue", "3 3rd", null);

  static DirectoryEntry clean(DirectoryEntry e) {
    return new DirectoryEntry(e.getLocation(), e.getName());
  }

  @Test
  public void clues_search_one() throws Exception {
    List<Clue> clues = Lists.newArrayList();
    clues.add(portrait);
    searchManager.addClues(CASE_DATA_ID, clues);
    Truth.assertThat(searchManager.searchCluesIds(CASE_DATA_ID, "portrait"))
        .containsExactly("portrait");
    Truth.assertThat(searchManager.searchCluesIds(CASE_DATA_ID, "queen victoria"))
        .containsExactly("portrait");
  }

  @Test
  public void clues_search_multiple() throws Exception {
    List<Clue> clues = Lists.newArrayList();
    clues.add(portrait);
    clues.add(note1);
    clues.add(note2);
    searchManager.addClues(CASE_DATA_ID, clues);

    Truth.assertThat(searchManager.searchCluesIds(CASE_DATA_ID, "note"))
        .containsExactly("note1", "note2");
    Truth.assertThat(searchManager.searchCluesIds(CASE_DATA_ID, "first")).containsExactly("note1");
    Truth.assertThat(searchManager.searchCluesIds(CASE_DATA_ID, "notefound")).isEmpty();
  }

  @Test
  public void generalDirectory_search_one() throws Exception {
    List<DirectoryEntry> dirs = Lists.newArrayList();
    dirs.add(jasper);

    searchManager.addGeneralDirectoryEntries(dirs);
    Truth.assertThat(searchDir("Sir")).containsExactly(jasperClean);
    Truth.assertThat(searchDir("medical examiner")).containsExactly(jasperClean);
    Truth.assertThat(searchDir("Lecture holmes")).containsExactly(jasperClean);
    Truth.assertThat(searchDir("MEEKS")).containsExactly(jasperClean);
    Truth.assertThat(searchDir("sir medical")).containsExactly(jasperClean);
  }

  @Test
  public void search_DrMorgan() throws Exception {
    List<DirectoryEntry> dirs = Lists.newArrayList();
    dirs.add(drMorgan);

    searchManager.addGeneralDirectoryEntries(dirs);
    Truth.assertThat(searchDir("Dr Morgan")).containsExactly(drMorgan);
  }

  private List<DirectoryEntry> searchDir(String query) {
    return searchManager.searchDir(CASE_DATA_ID, query);
  }

  @Test
  public void caseDirectory_search_one() throws Exception {
    List<DirectoryEntry> dirs = Lists.newArrayList();
    dirs.add(jasper);

    searchManager.addDirectoryEntries(CASE_DATA_ID, dirs);
    Truth.assertThat(searchDir("Jasper Meeks")).containsExactly(jasperClean);
  }

  @Test
  public void generalDirectory_search_multi() throws Exception {
    List<DirectoryEntry> dirs = Lists.newArrayList();
    dirs.add(jasper);
    dirs.add(murray);
    dirs.add(billJohn);
    dirs.add(aberdeenNavCo);
    searchManager.addGeneralDirectoryEntries(dirs);

    Truth.assertThat(searchDir("JASPER")).containsExactly(jasperClean);
    Truth.assertThat(searchDir("murray")).containsExactly(murrayClean);
    Truth.assertThat(searchDir("Lecture holmes")).containsExactly(jasperClean, murrayClean);
    Truth.assertThat(searchDir("h r")).containsExactly(murrayClean);
    Truth.assertThat(searchDir("bill and john")).containsExactly(billJohn);

    Truth.assertThat(searchDir("bill, and. john")).containsExactly(billJohn);
    Truth.assertThat(searchDir("Aberdeen Navigation company")).containsExactly(aberdeenNavCo);
  }

  @Test
  public void combinedDirectory_search_multi() throws Exception {
    List<DirectoryEntry> dirs = Lists.newArrayList();
    dirs.add(jasper);
    dirs.add(murray);
    searchManager.addGeneralDirectoryEntries(dirs);
    searchManager.addDirectoryEntries(
        CASE_DATA_ID, ImmutableList.of(murray, billJohn, aberdeenNavCo));

    Truth.assertThat(searchDir("JASPER")).containsExactly(jasperClean);
    Truth.assertThat(searchDir("murray")).containsExactly(murrayClean);
    Truth.assertThat(searchDir("Lecture holmes")).containsExactly(jasperClean, murrayClean);
    Truth.assertThat(searchDir("h r")).containsExactly(murrayClean);
    Truth.assertThat(searchDir("bill and john")).containsExactly(billJohn);

    Truth.assertThat(searchDir("bill, and. john")).containsExactly(billJohn);
    Truth.assertThat(searchDir("Aberdeen Navigation company")).containsExactly(aberdeenNavCo);
  }

  @Test
  public void escape() {
    Truth.assertThat(SearchManager.escapeQuery("big game")).isEqualTo("\"big\" \"game\"");
    Truth.assertThat(SearchManager.escapeQuery("\"big game\"")).isEqualTo("\"big\" \"game\"");
  }

  @Test
  public void normalize() {
    Truth.assertThat(SearchManager.normalize("the big a game")).isEqualTo("big game");
  }
}
