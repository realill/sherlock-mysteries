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

package com.google.mystery.data.model;

import static com.google.common.collect.ImmutableList.of;

import java.net.URL;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.truth.Truth;

public class ModelTest {
  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

  @Before
  public void setUp() {
    helper.setUp();
  }

  @Test
  public void case_writeRead() {
    Case c =
        new Case(
            "caseid",
            "caseDataId",
            "name",
            ImmutableList.of("an1", "an2"),
            "category",
            true,
            null,
            null,
            "author",
            "voiceActor",
            "illustrationArtist");
    Case loaded = Case.fromEntity(Case.toEntity(c));
    Truth.assertThat(loaded).isEqualTo(c);
  }

  @Test
  public void caseData_writeRead() {
    CaseData c = new CaseData("caseData");
    CaseData loaded = CaseData.fromEntity(CaseData.toEntity(c));
    Truth.assertThat(loaded).isEqualTo(c);
  }

  @Test
  public void session_writeRead() {
    Session s =
        new Session(
            "sessionid",
            Session.State.CASE_STARTED,
            of("locationsBacklog"),
            of("clues"),
            of("usedHints"),
            of("answers"),
            of(true),
            "followupText",
            of(new SessionActiveSuggestion("a", 1)),
            "caseid");
    Session loaded = Session.fromEntity(Session.toEntity(s));
    Truth.assertThat(loaded).isEqualTo(s);
  }

  @Test
  public void clue_writeRead() throws Exception {
    Clue clue =
        new Clue(
            "caseDataId", "id", "name", "description", "keywords", new URL("http://example.org"));
    Clue loaded = Clue.fromEntity(Clue.toEntity(clue));
    Truth.assertThat(loaded).isEqualTo(clue);
  }

  @Test
  public void hint_writeRead() throws Exception {
    Hint hint =
        new Hint(
            "caseDataId", ImmutableSet.of("precondition"), "hint", "id", ImmutableList.of("sug"));
    Hint loaded = Hint.fromEntity(Hint.toEntity(hint));
    Truth.assertThat(loaded).isEqualTo(hint);
  }

  @Test
  public void question_writeRead() throws Exception {
    Question q = new Question("caseDataId", "question", "answer", 1, 2);
    Question loaded = Question.fromEntity(Question.toEntity(q));
    Truth.assertThat(loaded).isEqualTo(q);
  }

  @Test
  public void story_writeRead() throws Exception {
    Story q =
        new Story(
            "caseDataId",
            "id",
            "title",
            "type",
            "text",
            new URL("http://example.org"),
            new URL("http://example.org"),
            ImmutableList.of("clues"),
            "latlong",
            new Date());
    Story loaded = Story.fromEntity(Story.toEntity(q));
    // comparing by string because internal date can differ.
    Truth.assertThat(loaded.toString()).isEqualTo(q.toString());
  }
}
