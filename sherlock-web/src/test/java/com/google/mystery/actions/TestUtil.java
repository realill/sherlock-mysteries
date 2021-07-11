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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Map;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.mystery.actions.messages.MessagesManager;
import com.google.mystery.actions.model.SherlockRequest;
import com.google.mystery.actions.model.SherlockRequestBuilder;
import com.google.mystery.assets.AssetsManager;
import com.google.mystery.data.model.Case;
import com.google.mystery.data.model.Clue;
import com.google.mystery.data.model.DirectoryEntry;
import com.google.mystery.data.model.Session;
import com.google.mystery.data.model.Session.State;
import com.google.mystery.data.model.Story;

public class TestUtil {
  public static final String CASE_DATA_ID = "case-data-id";
  public static String CASE_SESSIONID = "case-mySessionId";

  protected static void newCaseSession(SessionManager sessionManager) {
    Mockito.when(sessionManager.getSession())
        .thenReturn(Session.builder(CASE_SESSIONID).state(State.CASE_STARTED).build());
  }

  protected static SherlockRequest createRequest(
      String action, Map<String, String> params, SherlockRequest.Flag flag) {
    return createRequest(null, action, params, flag);
  }

  protected static SherlockRequest createRequest(String action, Map<String, String> params) {
    return createRequest(null, action, params, SherlockRequest.Flag.SPEAKER);
  }

  protected static SherlockRequest createRequest(String action) {
    return createRequest(action, ImmutableMap.of());
  }

  protected static SherlockRequest createRequest() {
    return createRequest("test query");
  }

  protected static SherlockRequest createRequest(
      String input, String action, Map<String, String> params, SherlockRequest.Flag flag) {
    SherlockRequestBuilder builder =
        new SherlockRequestBuilder(input, TestUtil.CASE_SESSIONID, action)
            .parameters(params)
            .caseDataId(TestUtil.CASE_DATA_ID);
    if (flag != null) {
      builder.addFlag(flag);
    }
    return builder.build();
  }

  protected static SherlockRequest createInputRequest(String input, String action) {
    return createRequest(input, action, ImmutableMap.of(), null);
  }

  protected static void mockAssets(AssetsManager assetsManager, MessagesManager messagesManager)
      throws Exception {
    Mockito.when(messagesManager.message((String) any()))
        .then(
            new Answer<String>() {
              @Override
              public String answer(InvocationOnMock invocation) throws Throwable {
                return invocation.getArguments()[0].toString() + " message";
              }
            });

    Mockito.when(messagesManager.message((String) any(), any()))
        .then(
            new Answer<String>() {
              @Override
              public String answer(InvocationOnMock invocation) throws Throwable {
                return invocation.getArguments()[0].toString() + " message";
              }
            });

    Mockito.when(assetsManager.getStory(eq(TestUtil.CASE_DATA_ID), (String) any()))
        .then(
            new Answer<Story>() {
              @Override
              public Story answer(InvocationOnMock invocation) throws Throwable {
                String id = invocation.getArguments()[1].toString();
                return new Story(
                    TestUtil.CASE_DATA_ID,
                    id,
                    id + " title",
                    id + "type",
                    id + " asset text",
                    null,
                    null);
              }
            });
  }

  public static void mockCaseStartedSession(SessionManager sessionManager) {
    Mockito.when(sessionManager.getSession())
        .thenReturn(Session.builder(TestUtil.CASE_SESSIONID).state(State.CASE_STARTED).build());
  }

  protected static void mockNewCaseSession(SessionManager sessionManager) {
    Mockito.when(sessionManager.getSession())
        .thenReturn(Session.builder(TestUtil.CASE_SESSIONID).state(State.NEW).build());
  }

  public static Case newCase(String id, boolean enabled) {
    return new Case(
        id,
        "data" + id,
        "name" + id,
        ImmutableList.of(),
        "category" + id,
        enabled,
        null,
        null,
        "author",
        "voiceActor",
        "illustrationArtist");
  }

  public static Clue newClue(String id) {
    try {
      return new Clue(
          TestUtil.CASE_DATA_ID,
          id,
          id + " name",
          id + " desc",
          id + " keywords",
          new URL("http://example.org/" + id));
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException(e);
    }
  }

  public static DirectoryEntry newDataEntry(String location) {
    return new DirectoryEntry("location", location + "name");
  }

  public static Story story(String name) {
    return new Story(
        TestUtil.CASE_DATA_ID,
        name + "id",
        name,
        Story.LOCATION,
        name + " text",
        null,
        null,
        ImmutableList.of(),
        "latlong",
        new Date());
  }
}
