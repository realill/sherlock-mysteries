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
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.truth.Truth;
import com.google.mystery.actions.messages.MessagesManager;
import com.google.mystery.actions.model.SherlockContext;
import com.google.mystery.actions.model.SherlockRequest;
import com.google.mystery.actions.model.SherlockResponse;
import com.google.mystery.actions.model.SherlockResponseBuilder;
import com.google.mystery.actions.model.SherlockScreenData;
import com.google.mystery.assets.AssetsManager;
import com.google.mystery.config.SherlockConfig;
import com.google.mystery.data.model.Clue;
import com.google.mystery.data.model.Session;
import com.google.mystery.data.model.Session.State;

@RunWith(MockitoJUnitRunner.class)
public class NavigationActionsTest {
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
  private NavigationActions actions;

  @Before
  public void init() throws Exception {
    actions.setActionsTool(actionsTool);
    TestUtil.mockAssets(assetsManager, messagesManager);
  }


  @Test
  public void lookup_locationCaseIntroduction() throws Exception {
    SherlockRequest request =
        TestUtil.createRequest("lookup", ImmutableMap.of("location", "Case Introduction"));
    TestUtil.mockCaseStartedSession(sessionManager);

    SherlockResponseBuilder responseBuilder = new SherlockResponseBuilder();

    actions.lookup(request, responseBuilder);
    SherlockResponse response = responseBuilder.build();
    Truth.assertThat(response.getCombinedText()).contains("repeatCaseIntroduction message");
  }

  @Test
  public void lookup_noParamAndNoBacklog() throws Exception {
    SherlockRequest request = TestUtil.createRequest();
    TestUtil.mockCaseStartedSession(sessionManager);

    SherlockResponseBuilder responseBuilder = new SherlockResponseBuilder();
    actions.lookup(request, responseBuilder);
    SherlockResponse response = responseBuilder.build();
    Truth.assertThat(response.getCombinedText()).contains("investigate-question message");
    Truth.assertThat(response.getOutContexts())
        .containsExactly(new SherlockContext("location-selection", 1));
  }

  @Test
  public void lookup_oneResult() throws Exception {
    SherlockRequest request = TestUtil.createRequest();
    Mockito.when(assetsManager.searchDir(eq(TestUtil.CASE_DATA_ID), eq("query")))
        .thenReturn(ImmutableList.of(TestUtil.newDataEntry("location")));
    TestUtil.mockCaseStartedSession(sessionManager);

    SherlockResponseBuilder responseBuilder = new SherlockResponseBuilder();
    actions.lookupQuery(request, responseBuilder, "query", null, null);
    SherlockResponse response = responseBuilder.build();
    Truth.assertThat(response.getCombinedText()).contains("navigate-pre message");
    Truth.assertThat(response.getCombinedText()).contains("location asset text");
  }

  @Test
  public void lookup_multiResult() throws Exception {
    SherlockRequest request = TestUtil.createRequest();
    Mockito.when(assetsManager.searchDir(eq(TestUtil.CASE_DATA_ID), eq("query"))).thenReturn(
        ImmutableList.of(TestUtil.newDataEntry("location1"), TestUtil.newDataEntry("location2")));
    TestUtil.mockCaseStartedSession(sessionManager);

    SherlockResponseBuilder responseBuilder = new SherlockResponseBuilder();
    actions.lookupQuery(request, responseBuilder, "query", null, null);
    SherlockResponse response = responseBuilder.build();
    Truth.assertThat(response.getCombinedText()).contains("lookupFoundMulti message");
  }

  @Test
  public void listClues() throws Exception {
    Clue clue1 = TestUtil.newClue("clue1");
    Clue clue2 = TestUtil.newClue("clue2");
    Session session = Session.builder(TestUtil.CASE_SESSIONID).state(State.CASE_STARTED)
        .clues(ImmutableList.of("clue1", "clue2")).build();
    Mockito.when(sessionManager.getSession()).thenReturn(session);
    Mockito.when(sessionManager.getSessionClues(eq(TestUtil.CASE_DATA_ID)))
        .thenReturn(ImmutableList.of(clue1, clue2));

    // list clues
    {
      SherlockRequest request = TestUtil.createRequest("list-clues");
      SherlockResponseBuilder responseBuilder = new SherlockResponseBuilder();
      actions.listClues(request, responseBuilder);
      SherlockResponse response = responseBuilder.build();

      Truth.assertThat(response.getCombinedText()).isEqualTo("listClues message");
    }
    // found clue
    {
      SherlockRequest request = TestUtil.createRequest("list-clues",
          ImmutableMap.<String, String>of("clue", "clue1 name"));
      SherlockResponseBuilder responseBuilder = new SherlockResponseBuilder();
      actions.listClues(request, responseBuilder);
      SherlockResponse response = responseBuilder.build();

      Truth.assertThat(response.getCombinedText()).isEqualTo("lookupCluesFound message");
    }

    // found clue-other
    {
      SherlockRequest request = TestUtil.createRequest("list-clues",
          ImmutableMap.<String, String>of("clue-other", "clue1 name"));
      SherlockResponseBuilder responseBuilder = new SherlockResponseBuilder();
      actions.listClues(request, responseBuilder);
      SherlockResponse response = responseBuilder.build();

      Truth.assertThat(response.getCombinedText()).isEqualTo("lookupCluesFound message");
    }

    // not found clue
    {
      SherlockRequest request = TestUtil.createRequest("list-clues",
          ImmutableMap.<String, String>of("clue-id", "clueNaN"));
      SherlockResponseBuilder responseBuilder = new SherlockResponseBuilder();
      actions.listClues(request, responseBuilder);
      SherlockResponse response = responseBuilder.build();

      Truth.assertThat(response.getCombinedText()).isEqualTo("listClues message");
    }
  }

  @Test
  public void lookupQuery_clues() throws IOException, Exception {
    Clue clue1 = TestUtil.newClue("clue");
    Mockito.when(sessionManager.getSessionClues(eq(TestUtil.CASE_DATA_ID)))
        .thenReturn(ImmutableList.of(clue1));
    Session session =
        Session.builder(TestUtil.CASE_SESSIONID).state(Session.State.CASE_STARTED).build();
    Mockito.when(sessionManager.getSession()).thenReturn(session);
    Mockito.when(assetsManager.searchClues(eq(TestUtil.CASE_DATA_ID), Mockito.eq("clue name"),
        Mockito.eq(session))).thenReturn(ImmutableList.of(clue1));
    SherlockResponseBuilder responseBuilder = new SherlockResponseBuilder();
    SherlockRequest request = TestUtil.createRequest("lookup");
    Truth.assertThat(actions.lookupQuery(request, responseBuilder, "clue name", null, null));

    Truth.assertThat(responseBuilder.build().getCombinedText()).contains("lookupCluesFound");
  }

  @Test
  // searching for newspaper shows newspaper
  public void lookup_newspaper() throws IOException, Exception {
    Clue clue1 = TestUtil.newClue("newspaper");
    Mockito.when(sessionManager.getSessionClues(eq(TestUtil.CASE_DATA_ID)))
        .thenReturn(ImmutableList.of(clue1));
    Session session = Session.builder(TestUtil.CASE_SESSIONID).state(Session.State.CASE_STARTED)
        .clues(ImmutableList.of("newspaper")).build();
    Mockito.when(sessionManager.getSession()).thenReturn(session);
    Mockito.when(assetsManager.searchClues(eq(TestUtil.CASE_DATA_ID), Mockito.eq("newspaper name"),
        Mockito.eq(session))).thenReturn(ImmutableList.of(clue1));
    SherlockResponseBuilder responseBuilder = new SherlockResponseBuilder();
    SherlockRequest request = TestUtil.createRequest("lookup");
    Truth.assertThat(actions.lookupQuery(request, responseBuilder, "newspaper name", null, null));

    SherlockResponse response = responseBuilder.build();
    Truth.assertThat(response.getCombinedText()).contains("listArticles");

    Truth.assertThat(response.getScreenData()).isNotNull();
    Truth.assertThat(response.getScreenData().getType())
        .isEqualTo(SherlockScreenData.Type.NEWSPAPER);
  }

  @Test
  public void lookup_map() throws IOException, Exception {
    Clue clue1 = TestUtil.newClue("map");
    Mockito.when(sessionManager.getSessionClues(eq(TestUtil.CASE_DATA_ID)))
        .thenReturn(ImmutableList.of(clue1));
    Session session =
        Session.builder(TestUtil.CASE_SESSIONID).state(Session.State.CASE_STARTED).build();
    Mockito.when(sessionManager.getSession()).thenReturn(session);
    Mockito.when(assetsManager.searchClues(eq(TestUtil.CASE_DATA_ID), Mockito.eq("map name"),
        Mockito.eq(session))).thenReturn(ImmutableList.of(clue1));
    SherlockResponseBuilder responseBuilder = new SherlockResponseBuilder();
    SherlockRequest request = TestUtil.createRequest("lookup");
    Truth.assertThat(actions.lookupQuery(request, responseBuilder, "map name", null, null));

    SherlockResponse response = responseBuilder.build();
    Truth.assertThat(response.getCombinedText()).contains("lookupCluesFound");
    Truth.assertThat(response.getScreenData()).isNotNull();
    Truth.assertThat(response.getScreenData().getType()).isEqualTo(SherlockScreenData.Type.MAP);
  }

  @Test
  public void newspaper_list() throws IOException, Exception {
    TestUtil.mockCaseStartedSession(sessionManager);
    Mockito.when(assetsManager.getAllAricles(TestUtil.CASE_DATA_ID))
        .thenReturn(ImmutableList.of(TestUtil.story("story1"), TestUtil.story("story2")));
    SherlockRequest request =
        TestUtil.createRequest("newspaper", ImmutableMap.<String, String>of());
    SherlockResponseBuilder responseBuilder = new SherlockResponseBuilder();
    actions.newspaper(request, responseBuilder);
    SherlockResponse response = responseBuilder.build();
    Truth.assertThat(response.getCombinedText()).isEqualTo("listArticles message");
    Truth.assertThat(response.getScreenData()).isNotNull();
    Truth.assertThat(response.getScreenData().getType())
        .isEqualTo(SherlockScreenData.Type.NEWSPAPER);
  }

  @Test
  public void newspaper_readOne() throws IOException, Exception {
    TestUtil.mockCaseStartedSession(sessionManager);
    Mockito.when(assetsManager.getAllAricles(TestUtil.CASE_DATA_ID))
        .thenReturn(ImmutableList.of(TestUtil.story("story1"), TestUtil.story("story2")));
    SherlockRequest request =
        TestUtil.createRequest("newspaper", ImmutableMap.<String, String>of("article", "story1"));
    SherlockResponseBuilder responseBuilder = new SherlockResponseBuilder();
    actions.newspaper(request, responseBuilder);
    SherlockResponse response = responseBuilder.build();
    Truth.assertThat(response.getCombinedText()).isEqualTo("article message");
    Truth.assertThat(response.getScreenData()).isNotNull();
    Truth.assertThat(response.getScreenData().getType())
        .isEqualTo(SherlockScreenData.Type.NEWSPAPER);
  }
}
