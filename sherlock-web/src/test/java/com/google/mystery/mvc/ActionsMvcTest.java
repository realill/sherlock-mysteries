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

package com.google.mystery.mvc;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import com.google.api.client.http.HttpRequestInitializer;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalSearchServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.common.collect.ImmutableList;
import com.google.common.truth.Truth;
import com.google.mystery.actions.SessionManager;
import com.google.mystery.assets.AssetsManager;
import com.google.mystery.assets.LongOperationsManager;
import com.google.mystery.config.SherlockConfig;
import com.google.mystery.data.DataManager;
import com.google.mystery.data.model.Case;
import com.google.mystery.data.model.Session;
import com.sherlockmysteries.pdata.Case.MapType;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = {MvcTestingConfig.class})
public class ActionsMvcTest {
  private static final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(
          new LocalDatastoreServiceTestConfig(), new LocalSearchServiceTestConfig());
  @Inject MysteryMvcTester tester;
  @Inject LongOperationsManager assetsImportManager;
  @Inject AssetsManager assetsManager;
  @Inject SessionManager sessionManager;
  @Inject DataManager dataManager;
  @Inject SherlockConfig config;

  @BeforeClass
  public static void setup() {
    helper.setUp();
  }

  private boolean wasDataImported = false;

  @Before
  public void init() throws Exception {
    if (!wasDataImported) {
      assetsImportManager.importGeneralAssets(
          Mockito.mock(HttpRequestInitializer.class), "base-data");
      assetsImportManager.importCaseAssets(
          Mockito.mock(HttpRequestInitializer.class), "test-case", "caseDataId");
      dataManager.addCase(
          new Case(
              "clay-pots",
              "caseDataId",
              "Clay Pots",
              ImmutableList.of(),
              "category",
              true,
              null,
              null,
              "author",
              "voiceActor",
              "illustrationArtist",
              MapType.BIRDEYE));
      wasDataImported = true;
    }
    tester.restartSession();
  }

  @Test
  public void startCase_caseStartPrestory() throws Exception {
    tester.request(TesterRequest.action("start-case")).assertSsmlContains("Case prestory");
  }

  @Test
  public void startCase_hint() throws Exception {
    tester
        .request(TesterRequest.action("start-case"))
        .assertSsmlContains("You are welcomed")
        .assertSsmlContains("Search for Scotland Yard to visit it");
  }

  @Test
  public void inputunknown_lookup() throws Exception {
    tester.request(TesterRequest.action("start-case"));
    tester
        .request(TesterRequest.action("input.unknown").input("scotland yard"))
        .assertSsmlContains("scotland yard");
  }

  @Test
  public void startCase_clues() throws Exception {
    tester
        .request(TesterRequest.action("start-case"))
        .assertSsmlContains("london map")
        .assertSsmlContains("newspaper");
  }

  @Test
  public void navigate_hints() throws Exception {
    tester
        .request(TesterRequest.action("start-case"))
        .assertSsmlContains("Search for Scotland Yard to visit it");
    tester
        .navigate("4 Whitehall Place")
        .assertSsmlContains("They also will be available on you game log page");
    tester
        .navigate("4 Whitehall Place")
        .assertSsmlDoesNotContain("They also will be available on you game log page");
  }

  @Test
  public void navigate_clues() throws Exception {
    tester
        .request(TesterRequest.action("start-case"))
        .assertSsmlContains("map")
        .assertSsmlContains("newspaper");
    tester
        .navigate("4 Whitehall Place")
        .assertSsmlContains("They also will be available on you game log page")
        .assertSsmlContains("portrait");
    tester.navigate("4 Whitehall Place").assertSsmlDoesNotContain("clue: portrait");
  }

  @Test
  public void navigate_audio() throws Exception {
    tester.request(TesterRequest.action("start-case")).assertSsmlContains("You are welcomed");
    tester
        .request(TesterRequest.action("navigate").param("location", "101a regent street"))
        .assertSsmlContains("Stephen Cook")
        .assertEndConversation();
    ;
  }

  @Test
  public void navigate_audioOnSpeakerEnds() throws Exception {
    tester.request(TesterRequest.action("start-case")).assertSsmlContains("You are welcomed");
    tester
        .request(
            TesterRequest.action("navigate")
                .param("location", "101a regent street")
                .speakerDevice())
        .assertSsmlContains("Stephen Cook")
        .assertNotEndConversation();
  }

  @Test
  public void navigate_simple() throws Exception {
    tester.request(TesterRequest.action("start-case")).assertSsmlContains("You are welcomed");
    tester
        .request(TesterRequest.action("navigate").param("location", "221B Baker Street"))
        .assertSsmlContains("left the house");
    tester
        .request(TesterRequest.action("status"))
        .assertSsmlContains(" 1 ")
        .assertSsmlContains("Sherlock Holmes");
  }

  @Test
  public void listClues_hasCaseClues() throws Exception {
    tester.request(TesterRequest.action("start-case"));
    tester
        .request(TesterRequest.action("list-clues"))
        .assertSsmlContains("london map")
        .assertSsmlContains("newspaper");
    tester.navigate("4 Whitehall Place");
    tester
        .request(TesterRequest.action("list-clues"))
        .assertSsmlContains("london map")
        .assertSsmlContains("newspaper")
        .assertSsmlContains("portrait");
    tester
        .request(TesterRequest.action("list-clues").param("clue", "london map"))
        .assertSsmlContains("london map")
        .assertSsmlContains("game log page");
  }

  @Test
  public void lookupLocation_findLocation() throws Exception {
    tester.request(TesterRequest.action("start-case"));
    tester
        .request(TesterRequest.action("lookup").param("location", "test location"))
        .assertSsmlContains("test location");
  }

  @Test
  public void lookupLocation_findClue() throws Exception {
    tester.request(TesterRequest.action("start-case"));
    tester
        .request(TesterRequest.action("lookup").param("clue", "queen victoria"))
        .assertSsmlDoesNotContain("portrait");
    tester.navigate("4 Whitehall Place");
    tester
        .request(TesterRequest.action("lookup").param("clue", "queen victoria"))
        .assertSsmlContains("portrait");
  }

  @Test
  public void lookupLocation_generalQuery() throws Exception {
    tester.request(TesterRequest.action("start-case"));
    tester.navigate("4 Whitehall Place");
    tester
        .request(TesterRequest.action("lookup").param("query", "queen victoria"))
        .assertSsmlDoesNotContain("portrait")
        .assertSsmlContains("Buckingham Palace");
    tester
        .request(TesterRequest.action("lookup").param("query", "newspaper"))
        .assertSsmlContains("today's times");
  }

  @Test
  public void finishConfirmed_contexts() throws Exception {
    tester.startCase();
    tester
        .request(TesterRequest.action("finish-confirm"))
        .assertSsmlContains("Where does Queen Victoria live")
        .assertOutContext("question");
  }

  @Test
  public void finish_answerQuestions() throws Exception {
    tester.startCase();
    tester.request(TesterRequest.action("finish-confirm"));
    tester
        .request(TesterRequest.action("answer").param("answer", "My first answer"))
        .assertSsmlContains("My first answer")
        .assertOutContext("answer-confirm");
    tester.request(TesterRequest.action("answer-confirm").param("answer", "My first answer"));
    tester
        .request(TesterRequest.action("answer-confirm").param("answer", "you"))
        .assertSsmlContains("last additional question");
    tester
        .request(TesterRequest.action("answer-confirm").param("answer", "no one"))
        .assertSsmlContains("answered all the questions")
        .assertSsmlContains("goal of this")
        .assertOutContext("answer");
  }

  @Test
  public void answer_reject() throws Exception {
    tester.startCase();
    tester.request(TesterRequest.action("finish-confirm"));
    tester.action("answer-reject").assertSsmlContains("review evidence");
  }

  @Test
  public void answerQuestions_repeat() throws Exception {
    tester.startCase();
    tester
        .request(TesterRequest.action("finish-confirm"))
        .assertSsmlContains("Where does Queen Victoria live");
    tester
        .request(TesterRequest.action("finish-answer-repeat"))
        .assertSsmlContains("Where does Queen Victoria live");
    tester.request(TesterRequest.action("answer").param("answer", "My first answer"));
    tester.request(TesterRequest.action("answer-confirm").param("answer", "My first answer"));
    tester
        .request(TesterRequest.action("finish-answer-repeat"))
        .assertSsmlContains("who was robbed");
    tester
        .request(TesterRequest.action("answer-confirm").param("answer", "you"))
        .assertSsmlContains("last additional question");
    tester
        .request(TesterRequest.action("finish-answer-repeat"))
        .assertSsmlContains("what is my name");
  }

  @Test
  public void validateAnswers_allCorrect() throws Exception {
    toValidation();
    tester
        .request(TesterRequest.action("validate-answer-confirm"))
        .assertSsmlContains("4 shillings")
        .assertSsmlContains("2 pence");
    tester
        .request(TesterRequest.action("validate-answer-confirm"))
        .assertSsmlContains("4 shillings")
        .assertSsmlContains("2 pence");
    tester
        .request(TesterRequest.action("validate-answer-confirm"))
        .assertSsmlContains("all 3")
        .assertSsmlContains("9 shillings")
        .assertSsmlContains("2 pence");

    Truth.assertThat(sessionManager.getDBSession(tester.getSessionId()).getState())
        .isEqualTo(Session.State.FINISH);
  }

  @Test
  public void validateAnswers_allIncorrect() throws Exception {
    toValidation();
    tester
        .request(TesterRequest.action("validate-answer-reject"))
        .assertSsmlDoesNotContain("shilling");
    tester
        .request(TesterRequest.action("validate-answer-reject"))
        .assertSsmlDoesNotContain("shilling");
    tester
        .request(TesterRequest.action("validate-answer-reject"))
        .assertSsmlContains("you haven't help")
        .assertSsmlContains("legwork");

    Truth.assertThat(sessionManager.getDBSession(tester.getSessionId()).getState())
        .isEqualTo(Session.State.FINISH);
  }

  @Test
  public void navigate_afterFinish() throws Exception {
    toFinish();
    Truth.assertThat(sessionManager.getDBSession(tester.getSessionId()).getState())
        .isEqualTo(Session.State.FINISH);

    tester.navigate("4 Whitehall Place").assertSsmlContains("scotland yard");
    Truth.assertThat(sessionManager.getDBSession(tester.getSessionId()).getState())
        .isEqualTo(Session.State.FINISH);
  }

  @Test
  public void validateAnswers_oneReject() throws Exception {
    toValidation();
    tester
        .request(TesterRequest.action("validate-answer-confirm"))
        .assertSsmlContains("4 shillings")
        .assertSsmlContains("2 pence");
    tester
        .request(TesterRequest.action("validate-answer-reject"))
        .assertSsmlDoesNotContain("4 shillings");
    tester
        .request(TesterRequest.action("validate-answer-confirm"))
        .assertSsmlContains("5 shillings")
        .assertSsmlContains("2 out of 3");

    Truth.assertThat(sessionManager.getDBSession(tester.getSessionId()).getState())
        .isEqualTo(Session.State.FINISH);
  }

  private void toValidation() throws Exception {
    tester.startCase();
    tester.request(TesterRequest.action("finish-confirm"));
    tester.request(TesterRequest.action("answer-confirm").param("answer", "My first answer"));
    tester.request(TesterRequest.action("answer-confirm").param("answer", "you"));
    tester.request(TesterRequest.action("answer-confirm").param("answer", "you again"));
  }

  @Test
  public void welcome_questions() throws Exception {
    tester.startCase();
    tester.request(TesterRequest.action("finish-confirm"));
    tester.request(TesterRequest.action("welcome")).assertSsmlContains("where does queen victoria");
  }

  @Test
  public void welcome_validation() throws Exception {
    toValidation();
    tester.request(TesterRequest.action("welcome")).assertSsmlContains("where does queen victoria");
  }

  @Test
  public void welcome_finish() throws Exception {
    toFinish();
    tester.request(TesterRequest.action("welcome")).assertSsmlContains("you have finished");
  }

  private void toFinish() throws Exception {
    toValidation();
    tester.request(TesterRequest.action("validate-answer-confirm"));
    tester.request(TesterRequest.action("validate-answer-confirm"));
    tester.request(TesterRequest.action("validate-answer-confirm"));
  }

  @Test
  public void validate_repeat() throws Exception {
    toValidation();
    tester
        .request(TesterRequest.action("finish-validate-repeat"))
        .assertSsmlContains("where does queen victoria")
        .assertOutContext("validate-answer-confirm");
    tester.request(TesterRequest.action("validate-answer-confirm"));
    tester
        .request(TesterRequest.action("finish-validate-repeat"))
        .assertSsmlContains("who was robbed")
        .assertOutContext("validate-answer-confirm");
    tester.request(TesterRequest.action("validate-answer-confirm"));
    tester
        .request(TesterRequest.action("finish-validate-repeat"))
        .assertSsmlContains("my name")
        .assertOutContext("validate-answer-confirm");
  }

  @Test
  public void news_all() throws Exception {
    tester.startCase();
    tester
        .request(TesterRequest.action("newspaper").param("article", "all"))
        .assertSsmlContains("crime");
  }

  @Test
  public void news_crime() throws Exception {
    tester.startCase();
    tester
        .request(TesterRequest.action("newspaper").param("article", "Crime"))
        .assertSsmlContains("crime");
  }

  @Test
  public void navigate_questionForbiden() throws Exception {
    tester.startCase();
    tester.navigate("4 Whitehall Place");
    tester.request(TesterRequest.action("finish-confirm"));
    tester.lookup("4 Whitehall Place").assertSsmlContains("scotland yard");
    tester.lookup("101 A Regent Street").assertSsmlContains("We can not go");
    tester.lookup("Buckingham Palace").assertSsmlContains("We can not go");
  }

  @Test
  public void navigate_answersForbiden() throws Exception {
    tester.startCase();
    tester.navigate("4 Whitehall Place");
    tester.request(TesterRequest.action("finish-confirm"));
    tester.lookup("4 Whitehall Place").assertSsmlContains("scotland yard");
    tester.lookup("101 A Regent Street").assertSsmlContains("We can not go");
    tester.lookup("Buckingham Palace").assertSsmlContains("We can not go");
  }

  @Test
  public void logLink() throws Exception {
    tester.startCase();
    tester.request(TesterRequest.action("log-link")).assertLinkOutContains("/session/");
  }
}
