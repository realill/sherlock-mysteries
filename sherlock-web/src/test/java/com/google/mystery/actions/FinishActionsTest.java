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

package com.google.mystery.actions;

import static org.mockito.Matchers.eq;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import com.google.common.collect.ImmutableMap;
import com.google.common.truth.Truth;
import com.google.mystery.actions.messages.MessagesManager;
import com.google.mystery.actions.model.SherlockRequest;
import com.google.mystery.actions.model.SherlockRequest.Flag;
import com.google.mystery.actions.model.SherlockRequestBuilder;
import com.google.mystery.actions.model.SherlockResponse;
import com.google.mystery.actions.model.SherlockResponseBuilder;
import com.google.mystery.assets.AssetsManager;
import com.google.mystery.config.SherlockConfig;
import com.google.mystery.data.model.Question;
import com.google.mystery.data.model.Session;
import com.google.mystery.data.model.Session.State;

@RunWith(MockitoJUnitRunner.class)
public class FinishActionsTest {
  @Mock
  private MessagesManager messagesManager;
  @Mock
  private AssetsManager assetsManager;
  @Mock
  private SessionManager sessionManager;
  @Mock
  private SherlockConfig config;
  @InjectMocks
  private ActionsTool actionsTool;
  @InjectMocks
  private FinishActions actions;

  @Before
  public void init() {
    actions.setActionsTool(actionsTool);
  }

  @Test
  public void answer() throws Exception {
    TestUtil.mockAssets(assetsManager, messagesManager);
    Mockito.when(sessionManager.getSession())
        .thenReturn(Session.builder(TestUtil.CASE_SESSIONID).state(State.QUESTIONS).build());
    Mockito.when(assetsManager.getQuestion(eq(TestUtil.CASE_DATA_ID), eq(0)))
        .thenReturn(new Question(TestUtil.CASE_SESSIONID, "question1", "answer1", 20, 0));
    SherlockRequest request =
        TestUtil.createRequest("answer", ImmutableMap.of("answer", "my answer"));
    SherlockResponseBuilder response = new SherlockResponseBuilder();
    actions.answer(request, response);
    SherlockResponse r = response.build();
    Truth.assertThat(r.getStoryText()).contains("finishAnswerConfirm message");

    // answer is in input
    request = new SherlockRequestBuilder("my answer", TestUtil.CASE_SESSIONID, "answer")
        .caseDataId(TestUtil.CASE_DATA_ID).build();
    response = new SherlockResponseBuilder();
    actions.answer(request, response);
    r = response.build();
    Truth.assertThat(r.getStoryText()).contains("finishAnswerConfirm message");
  }

  @Test
  public void skipQuestion() throws Exception {
    TestUtil.mockAssets(assetsManager, messagesManager);
    Mockito.when(sessionManager.getSession())
        .thenReturn(Session.builder(TestUtil.CASE_SESSIONID).state(State.QUESTIONS).build());
    mockQuestions(question("q1"), question("q2"));
    SherlockRequest request = TestUtil.createRequest("skip-question", ImmutableMap.of());
    SherlockResponseBuilder response = new SherlockResponseBuilder();
    actions.skipQuestion(request, response);
    SherlockResponse r = response.build();
    Truth.assertThat(r.getStoryText()).contains("finishQuestion message");
  }

  @Test
  public void finalSolition() throws Exception {
    TestUtil.mockAssets(assetsManager, messagesManager);
    Mockito.when(sessionManager.getSession()).thenReturn(Session.builder(TestUtil.CASE_SESSIONID)
        .addAnswer("my anwer").state(State.QUESTIONS).build());
    mockQuestions(question("q1"), question("q2"));
    SherlockRequest request = TestUtil.createRequest("skip-question", ImmutableMap.of());
    SherlockResponseBuilder response = new SherlockResponseBuilder();
    actions.skipQuestion(request, response);
    SherlockResponse r = response.build();
    Truth.assertThat(r.getStoryText()).contains("finalSolution asset text");
    Truth.assertThat(response.getSuggestions()).containsExactly("Go on");
  }

  @Test
  public void finishAfterQuestions() throws Exception {
    TestUtil.mockAssets(assetsManager, messagesManager);
    Mockito.when(sessionManager.getSession())
        .thenReturn(Session.builder(TestUtil.CASE_SESSIONID).state(State.ANSWERS).build());
    SherlockRequest request = TestUtil.createRequest("finish", ImmutableMap.of());
    SherlockResponseBuilder response = new SherlockResponseBuilder();
    actions.finish(request, response);
    SherlockResponse r = response.build();
    Truth.assertThat(r.getPrestoryText()).contains("repeatFinalSolution");
    Truth.assertThat(r.getStoryText()).contains("finalSolution asset text");
  }

  @Test
  public void finalSolitionCanvas() throws Exception {
    TestUtil.mockAssets(assetsManager, messagesManager);
    Mockito.when(sessionManager.getSession()).thenReturn(Session.builder(TestUtil.CASE_SESSIONID)
        .state(State.QUESTIONS).addAnswer("my answer").build());
    mockQuestions(question("q1"), question("q2"));
    SherlockRequest request = TestUtil.createRequest("answer-confirm",
        ImmutableMap.of("answer", "my answer"), Flag.CANVAS);
    SherlockResponseBuilder response = new SherlockResponseBuilder();
    actions.skipQuestion(request, response);
    SherlockResponse r = response.build();
    Truth.assertThat(r.getStoryText()).contains("finalSolution asset text");
    Truth.assertThat(response.getSuggestions()).isEmpty();
  }

  private Question question(String name) {
    return new Question(TestUtil.CASE_SESSIONID, name + "-question", "", 20, 0);
  }

  private void mockQuestions(Question... questions) {
    for (int i = 0; i < questions.length; i++) {
      Mockito.when(assetsManager.getQuestion(eq(TestUtil.CASE_DATA_ID), eq(i)))
          .thenReturn(questions[i]);
    }
    Mockito.when(assetsManager.getQuestionsSize(eq(TestUtil.CASE_DATA_ID)))
        .thenReturn(questions.length);
  }
}
