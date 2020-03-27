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

import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import com.google.api.client.util.Lists;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.truth.Truth;
import com.google.mystery.actions.model.SherlockRequest;
import com.google.mystery.actions.model.SherlockRequestBuilder;
import com.google.mystery.actions.model.SherlockResponse;
import com.google.mystery.actions.model.SherlockResponseBuilder;
import com.google.mystery.config.SherlockConfig;
import com.google.mystery.data.model.Hint;
import com.google.mystery.data.model.Session;
import com.google.mystery.data.model.Session.State;
import com.google.mystery.data.model.SessionActiveSuggestion;

@RunWith(MockitoJUnitRunner.class)
public class SuggestionsManagerTest {
  @Mock SessionManager sessionManager;
  @Mock SherlockConfig config;
  @InjectMocks private SuggestionsManager manager;

  private void mockSession(Object... params) {
    List<SessionActiveSuggestion> suggestions = Lists.newArrayList();
    for (int i = 0; i < params.length / 2; i++) {
      suggestions.add(
          new SessionActiveSuggestion(params[i * 2].toString(), (Integer) params[i * 2 + 1]));
    }
    Mockito.when(sessionManager.getSession())
        .thenReturn(
            Session.builder("sessionid")
                .activeSuggestions(suggestions)
                .state(State.CASE_STARTED)
                .build());
  }

  @Test
  public void suggest_activeInOrder() {
    SherlockRequest request = createRequest();
    SherlockResponseBuilder response = SherlockResponse.builder("response");

    mockSession("sug1", 10, "sug2", 5, "sug3", 20, "sug4", 40, "sug5", 15);
    manager.suggest(request, response);
    Truth.assertThat(response.getSuggestions()).containsExactly("sug4", "sug3", "sug5");
  }

  private SherlockRequest createRequest() {
    return new SherlockRequestBuilder(null, "sessionid", "action").build();
  }

  @Test
  public void suggest_sameElements() {
    SherlockRequest request = createRequest();
    SherlockResponseBuilder response = SherlockResponse.builder("response");
    mockSession("sug1", 3, "sug2", 10, "sug3", 5, "sug4", 5, "sug5", 10);
    manager.suggest(request, response);
    Truth.assertThat(response.getSuggestions()).containsAllOf("sug2", "sug5");
    Truth.assertThat(response.getSuggestions()).containsAnyOf("sug3", "sug4");
  }

  @Test
  public void suggest_twoActveSuggestions() {
    SherlockRequest request = createRequest();
    SherlockResponseBuilder response = SherlockResponse.builder("response");
    mockSession("sug1", 3, "sug2", 10);
    manager.suggest(request, response);
    Truth.assertThat(response.getSuggestions()).containsExactly("sug1", "sug2");
  }

  @Test
  public void suggest_hintSuggestion() {
    SherlockRequest request = createRequest();
    SherlockResponseBuilder response =
        SherlockResponse.builder("response")
            .hint(
                new Hint(
                    "caseDataId",
                    ImmutableSet.of(),
                    "hint1",
                    "hintid",
                    ImmutableList.of("sug1", "sug2")));
    mockSession();
    manager.suggest(request, response);
    Mockito.verify(sessionManager, Mockito.times(1))
        .addActiveSuggestions(Matchers.eq(ImmutableList.of("sug1", "sug2")));
  }

  @Test
  public void suggest_ageActiveSuggestions() {
    SherlockRequest request = createRequest();
    SherlockResponseBuilder response = SherlockResponse.builder("response");
    mockSession();
    manager.suggest(request, response);
    Mockito.verify(sessionManager, Mockito.times(1)).ageActiveSuggestions();
  }
}
