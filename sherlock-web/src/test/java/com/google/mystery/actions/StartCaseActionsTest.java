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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.truth.Truth;
import com.google.mystery.actions.messages.MessagesManager;
import com.google.mystery.actions.model.SherlockRequest;
import com.google.mystery.actions.model.SherlockResponse;
import com.google.mystery.actions.model.SherlockResponseBuilder;
import com.google.mystery.assets.AssetsManager;
import com.google.mystery.config.SherlockConfig;

@RunWith(MockitoJUnitRunner.class)
public class StartCaseActionsTest {
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
  private StartCaseActions actions;

  @Before
  public void init() {
    actions.setActionsTool(actionsTool);
  }

  @Test
  public void startCase_noCases() throws Exception {
    TestUtil.mockAssets(assetsManager, messagesManager);
    TestUtil.mockNewCaseSession(sessionManager);
    Mockito.when(assetsManager.getAllEnabledCases()).thenReturn(ImmutableList.of());

    SherlockRequest request = TestUtil.createRequest("start-case", ImmutableMap.of());
    SherlockResponseBuilder responseBuilder = new SherlockResponseBuilder();
    actions.startCase(request, responseBuilder);
    SherlockResponse response = responseBuilder.build();
    Truth.assertThat(response.getCombinedText()).contains("newCase-noPlayableCases message");
  }

  @Test
  public void startCase_oneCase() throws Exception {
    TestUtil.mockAssets(assetsManager, messagesManager);
    TestUtil.mockNewCaseSession(sessionManager);
    Mockito.when(assetsManager.getAllEnabledCases())
        .thenReturn(ImmutableList.of(TestUtil.newCase("newCaseId", true)));
    Mockito.when(assetsManager.getCaseDataId(eq("newCaseId"))).thenReturn(TestUtil.CASE_DATA_ID);

    SherlockRequest request = TestUtil.createRequest("start-case");
    SherlockResponseBuilder responseBuilder = new SherlockResponseBuilder();
    actions.startCase(request, responseBuilder);
    SherlockResponse response = responseBuilder.build();
    Truth.assertThat(response.getCombinedText()).contains("caseIntroduction");
  }

  @Test
  public void startCase_oneCase_inCase() throws Exception {
    TestUtil.mockAssets(assetsManager, messagesManager);
    TestUtil.mockCaseStartedSession(sessionManager);
    Mockito.when(assetsManager.getAllEnabledCases())
        .thenReturn(ImmutableList.of(TestUtil.newCase("newCaseId", true)));
    Mockito.when(assetsManager.getCaseDataId(eq("newCaseId"))).thenReturn(TestUtil.CASE_DATA_ID);
    Mockito.when(assetsManager.getCase(eq("newCaseId")))
        .thenReturn(TestUtil.newCase("caseId1", true));

    SherlockRequest request = TestUtil.createRequest("start-case");
    SherlockResponseBuilder responseBuilder = new SherlockResponseBuilder();
    actions.startCase(request, responseBuilder);
    SherlockResponse response = responseBuilder.build();
    Truth.assertThat(response.getCombinedText()).contains("newCase-caseAlreadyStarted message");
    Truth.assertThat(response.getOutContexts()).hasSize(1);
    Truth.assertThat(response.getOutContexts().get(0).getName())
        .isEqualTo("start-case-confirmation");
  }

  @Test
  public void startCase_caseId_inCase() throws Exception {
    TestUtil.mockAssets(assetsManager, messagesManager);
    Mockito.when(assetsManager.getCaseDataId(eq("newCaseId"))).thenReturn(TestUtil.CASE_DATA_ID);
    TestUtil.mockCaseStartedSession(sessionManager);
    Mockito.when(assetsManager.getCase(eq("newCaseId")))
        .thenReturn(TestUtil.newCase("caseId1", true));

    SherlockRequest request =
        TestUtil.createRequest("start-case", ImmutableMap.of("caseId", "newCaseId"));
    SherlockResponseBuilder responseBuilder = new SherlockResponseBuilder();
    actions.startCase(request, responseBuilder);
    SherlockResponse response = responseBuilder.build();
    Truth.assertThat(response.getCombinedText()).contains("newCase-caseAlreadyStarted message");
    Truth.assertThat(response.getOutContexts()).hasSize(1);
    Truth.assertThat(response.getOutContexts().get(0).getName())
        .isEqualTo("start-case-confirmation");
  }

  @Test
  public void startCase_multipleCases() throws Exception {
    TestUtil.mockAssets(assetsManager, messagesManager);
    TestUtil.mockNewCaseSession(sessionManager);
    Mockito.when(assetsManager.getAllEnabledCases()).thenReturn(ImmutableList
        .of(TestUtil.newCase("newCaseId", true), TestUtil.newCase("newCaseId2", true)));

    SherlockRequest request = TestUtil.createRequest("start-case");
    SherlockResponseBuilder responseBuilder = new SherlockResponseBuilder();
    actions.startCase(request, responseBuilder);
    SherlockResponse response = responseBuilder.build();
    Truth.assertThat(response.getCombinedText()).contains("newCase-selection");
    Truth.assertThat(response.getOutContexts()).hasSize(1);
    Truth.assertThat(response.getOutContexts().get(0).getName()).isEqualTo("case-selection");
  }

  @Test
  public void startCase_caseId() throws Exception {
    TestUtil.mockAssets(assetsManager, messagesManager);
    Mockito.when(assetsManager.getCaseDataId(eq("newCaseId"))).thenReturn(TestUtil.CASE_DATA_ID);
    TestUtil.newCaseSession(sessionManager);

    SherlockRequest request =
        TestUtil.createRequest("start-case", ImmutableMap.of("caseId", "newCaseId"));
    SherlockResponseBuilder responseBuilder = new SherlockResponseBuilder();
    actions.startCaseConfirm(request, responseBuilder);
    SherlockResponse response = responseBuilder.build();
    Truth.assertThat(response.getCombinedText()).contains("caseIntroduction asset text");
  }

  @Test
  public void startCase_case() throws Exception {
    TestUtil.mockAssets(assetsManager, messagesManager);
    Mockito.when(assetsManager.getCaseDataId(eq("case1"))).thenReturn(TestUtil.CASE_DATA_ID);

    Mockito.when(assetsManager.getAllEnabledCases()).thenReturn(
        ImmutableList.of(TestUtil.newCase("case1", true), TestUtil.newCase("case2", true)));
    TestUtil.newCaseSession(sessionManager);

    SherlockRequest request =
        TestUtil.createRequest("start-case", ImmutableMap.of("case", "namecase1"));
    SherlockResponseBuilder responseBuilder = new SherlockResponseBuilder();
    actions.startCaseConfirm(request, responseBuilder);
    SherlockResponse response = responseBuilder.build();
    Truth.assertThat(response.getCombinedText()).contains("caseIntroduction asset text");
  }

  @Test
  public void startCaseConfirm_caseDataId() throws Exception {
    TestUtil.mockAssets(assetsManager, messagesManager);
    Mockito.when(assetsManager.getCaseDataId(eq("newCaseId"))).thenReturn(TestUtil.CASE_DATA_ID);
    TestUtil.newCaseSession(sessionManager);

    SherlockRequest request =
        TestUtil.createRequest("start-case-confirm", ImmutableMap.of("caseId", "newCaseId"));
    SherlockResponseBuilder responseBuilder = new SherlockResponseBuilder();
    actions.startCaseConfirm(request, responseBuilder);
    SherlockResponse response = responseBuilder.build();
    Truth.assertThat(response.getCombinedText()).contains("caseIntroduction asset text");
  }

  @Test
  public void startCaseConfirm() throws Exception {
    TestUtil.mockAssets(assetsManager, messagesManager);
    TestUtil.newCaseSession(sessionManager);
    Mockito.when(assetsManager.getAllEnabledCases())
        .thenReturn(ImmutableList.of(TestUtil.newCase("newCaseId", true)));
    Mockito.when(assetsManager.getCaseDataId(eq("newCaseId"))).thenReturn(TestUtil.CASE_DATA_ID);

    SherlockRequest request =
        TestUtil.createRequest("start-case-confirm", ImmutableMap.of("caseId", "newCaseId"));

    SherlockResponseBuilder responseBuilder = new SherlockResponseBuilder();
    actions.startCaseConfirm(request, responseBuilder);
    SherlockResponse response = responseBuilder.build();
    Truth.assertThat(response.getCombinedText()).contains("caseIntroduction asset text");
  }
}
