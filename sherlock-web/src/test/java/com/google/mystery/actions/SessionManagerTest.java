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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import com.google.common.collect.ImmutableList;
import com.google.common.truth.Truth;
import com.google.mystery.actions.messages.MessagesManager;
import com.google.mystery.assets.AssetsManager;
import com.google.mystery.data.DataManager;
import com.google.mystery.data.model.SessionActiveSuggestion;
import com.google.mystery.data.model.SessionLog;
import com.google.mystery.data.model.Story;

@RunWith(MockitoJUnitRunner.class)
public class SessionManagerTest {
  @Mock private DataManager dataManager;
  @Mock private AssetsManager assetsManager;
  @Mock private MessagesManager messages;
  @Spy private SessionHolder sessionHolder;
  @InjectMocks private SessionManager sessionManager;

  @Test
  public void sessionLog_refresh() {
    Mockito.when(dataManager.getSessionLogs(eq("sessionid"), eq(123l)))
        .thenReturn(ImmutableList.of());
    Mockito.when(dataManager.getSessionLogs(eq("sessionid"), eq(1000l)))
        .thenReturn(
            ImmutableList.of(new SessionLog("sessionid", "storyid", ImmutableList.of(), "hintid")));

    Truth.assertThat(sessionManager.checkSessionNewData("sessionid", 123)).isFalse();
    Truth.assertThat(sessionManager.checkSessionNewData("sessionid", 1000)).isTrue();
  }

  @Test
  public void ageActiveSessions() throws MalformedURLException {
    Mockito.when(assetsManager.getStory(eq("caseDataId"), Matchers.anyString()))
        .thenReturn(
            new Story(
                "caseDataId",
                "id",
                "title",
                "type",
                "text",
                new URL("http://example.com"),
                new URL("http://example.com/story.ogg")));
    sessionManager.initSession("sessionid");
    sessionManager.addActiveSuggestions(ImmutableList.of("sug1", "sug2"));
    List<SessionActiveSuggestion> activeSuggestion =
        sessionManager.getSession().getActiveSuggestions();
    sessionManager.ageActiveSuggestions();

    Truth.assertThat(activeSuggestion.size()).isEqualTo(2);
    List<SessionActiveSuggestion> agedActiveSuggestion =
        sessionManager.getSession().getActiveSuggestions();
    SessionActiveSuggestion s1 = activeSuggestion.get(0);
    SessionActiveSuggestion s2 = activeSuggestion.get(1);
    Truth.assertThat(agedActiveSuggestion)
        .containsExactly(
            new SessionActiveSuggestion(s1.getSuggestion(), s1.getRelevancy() - 1),
            new SessionActiveSuggestion(s2.getSuggestion(), s2.getRelevancy() - 1));
  }
}
