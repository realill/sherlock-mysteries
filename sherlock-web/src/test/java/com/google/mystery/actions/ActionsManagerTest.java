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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import java.io.IOException;
import java.net.URL;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import com.google.common.collect.ImmutableMap;
import com.google.common.truth.Truth;
import com.google.mystery.actions.messages.MessagesManager;
import com.google.mystery.actions.model.SherlockRequest;
import com.google.mystery.actions.model.SherlockResponse;
import com.google.mystery.assets.AssetsManager;
import com.google.mystery.config.SherlockConfig;
import com.google.mystery.data.model.Session;
import com.google.mystery.data.model.Session.State;
import com.google.mystery.data.model.Story;

@RunWith(MockitoJUnitRunner.class)
public class ActionsManagerTest {

  @Mock
  private MessagesManager messagesManager;
  @Mock
  private AssetsManager assetsManager;
  @Mock
  private SessionManager sessionManager;
  @Mock
  private SuggestionsManager suggestionsManager;
  @Mock
  private SherlockConfig config;
  @Mock
  private StartCaseActions startCaseActions;
  @Mock
  private NavigationActions navigationActions;
  @Mock
  private FinishActions finishActions;

  @InjectMocks
  private ActionsTool actionsTool;
  @InjectMocks
  private ActionsManager manager;

  @Before
  public void init() throws Exception {
    manager.init();
    TestUtil.mockAssets(assetsManager, messagesManager);
    Mockito.when(config.getBaseUrl()).thenReturn(new URL("http://example.org/"));
    manager.setActionsTool(actionsTool);
  }

  /** Check new start suggestions */
  @Test
  public void newState_suggestions() throws IOException, Exception {
    TestUtil.mockCaseStartedSession(sessionManager);
    SherlockRequest request = TestUtil.createRequest("welcome");
    manager.request(request);
    Mockito.verify(suggestionsManager, Mockito.times(1)).suggest(Matchers.eq(request), any());
  }

  @Test
  public void error_answer() throws IOException, Exception {
    TestUtil.mockCaseStartedSession(sessionManager);
    SherlockRequest request = TestUtil.createRequest("error", ImmutableMap.of(), null);
    SherlockResponse response = manager.request(request);
    Truth.assertThat(response.getCombinedText()).isEqualTo("error message");
  }

  @Test
  public void wellcome_answer() throws IOException, Exception {
    TestUtil.mockCaseStartedSession(sessionManager);
    SherlockRequest request = TestUtil.createRequest("welcome", ImmutableMap.<String, String>of());
    SherlockResponse response = manager.request(request);
    Truth.assertThat(response.getCombinedText()).isEqualTo("welcomeCase message");
  }

  @Test
  public void wellcome_followup() throws IOException, Exception {
    Mockito.when(sessionManager.getSession()).thenReturn(Session.builder(TestUtil.CASE_SESSIONID)
        .state(State.CASE_STARTED).followupText("follow up").build());
    SherlockRequest request = TestUtil.createRequest("welcome");
    SherlockResponse response = manager.request(request);
    Truth.assertThat(response.getCombinedText().trim()).isEqualTo("follow up");
    Truth.assertThat(response.getEndConversation()).isEqualTo(false);
  }

  @Test
  public void media_followup() throws IOException, Exception {
    Mockito.when(sessionManager.getSession()).thenReturn(Session.builder(TestUtil.CASE_SESSIONID)
        .state(State.CASE_STARTED).followupText("follow up").build());
    SherlockRequest request = TestUtil.createRequest("media-followup");
    SherlockResponse response = manager.request(request);
    Truth.assertThat(response.getCombinedText().trim()).isEqualTo("follow up");
    Truth.assertThat(response.getEndConversation()).isEqualTo(false);
  }

  @Test
  public void mediaFollowup_WhatIsNext() throws IOException, Exception {
    Mockito.when(sessionManager.getSession())
        .thenReturn(Session.builder(TestUtil.CASE_SESSIONID).state(State.CASE_STARTED).build());
    SherlockRequest request = TestUtil.createRequest("media-followup");
    SherlockResponse response = manager.request(request);
    Truth.assertThat(response.getCombinedText().trim()).isEqualTo("whatIsNext message");
    Truth.assertThat(response.getEndConversation()).isEqualTo(false);
  }

  @Test
  public void media_followup_nonspeaker() throws IOException, Exception {
    Mockito.when(sessionManager.getSession()).thenReturn(Session.builder(TestUtil.CASE_SESSIONID)
        .state(State.CASE_STARTED).followupText("follow up").build());
    SherlockRequest request = TestUtil.createRequest("media-followup", ImmutableMap.of(), null);
    SherlockResponse response = manager.request(request);
    Truth.assertThat(response.getCombinedText().trim()).isEqualTo("follow up");
    Truth.assertThat(response.getEndConversation()).isEqualTo(false);
  }

  @Test
  public void story_asnwer() throws IOException, Exception {
    TestUtil.mockCaseStartedSession(sessionManager);
    SherlockRequest request =
        TestUtil.createRequest("story", ImmutableMap.<String, String>of("id", "storyid"));
    SherlockResponse response = manager.request(request);
    Truth.assertThat(response.getCombinedText().trim()).isEqualTo("storyid asset text");
  }

  @Test
  public void audio_file() throws IOException, Exception {
    TestUtil.mockCaseStartedSession(sessionManager);
    SherlockRequest request =
        TestUtil.createRequest("story", ImmutableMap.<String, String>of("id", "storyid"));
    Mockito.when(assetsManager.getStory(eq(TestUtil.CASE_DATA_ID), eq("storyid")))
        .thenReturn(new Story(TestUtil.CASE_DATA_ID, "storyid", "storyid title", "stiryid type",
            "storyid text", new URL("http://example.com/storyid.ogg"),
            new URL("http://example.com/storyid.jpg")));
    SherlockResponse response = manager.request(request);
    Truth.assertThat(response.getStoryAudioURL().toString())
        .isEqualTo("http://example.com/storyid.ogg");
  }

  @Test
  public void unknownAction_error() throws IOException, Exception {
    Mockito.when(sessionManager.getSession())
        .thenReturn(Session.builder(TestUtil.CASE_SESSIONID).build());
    SherlockRequest request = TestUtil.createRequest("unknown-action", ImmutableMap.of(), null);
    SherlockResponse response = manager.request(request);
    Truth.assertThat(response.getCombinedText()).isEqualTo("error message");
  }

  @Test
  public void navigate_require_case() throws IOException, Exception {
    Mockito.when(sessionManager.getSession())
        .thenReturn(Session.builder(TestUtil.CASE_SESSIONID).build());
    SherlockRequest request =
        TestUtil.createRequest("navigate", ImmutableMap.<String, String>of("location", "2EC"));
    SherlockResponse response = manager.request(request);
    Truth.assertThat(response.getCombinedText()).isEqualTo("caseRequired message");
  }

  @Test
  public void answerConfirm_requesresAnswersState() throws IOException, Exception {
    TestUtil.mockCaseStartedSession(sessionManager);
    SherlockRequest request =
        TestUtil.createRequest("answer-confirm", ImmutableMap.<String, String>of("answer", "yes"));
    SherlockResponse response = manager.request(request);
    Truth.assertThat(response.getCombinedText()).isEqualTo("welcomeCase message");
  }
}
