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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import javax.inject.Inject;
import com.google.mystery.actions.model.SherlockRequest;
import com.google.mystery.actions.model.SherlockResponseBuilder;
import com.google.mystery.config.SherlockConfig;
import com.google.mystery.data.model.Session;
import com.google.mystery.data.model.Session.State;
import com.google.mystery.data.model.SessionActiveSuggestion;

/**
 * Handles suggestions. Called after {@link SherlockResponseBuilder} generated response.
 *
 * @author ilyaplatonov
 */
public class SuggestionsManager {
  @Inject
  SessionManager sessionManager;
  @Inject
  SherlockConfig config;

  /** Propagates suggestion into response */
  public void suggest(SherlockRequest request, SherlockResponseBuilder response) {
    Session session = sessionManager.getSession();
    if ((session.getState() == State.NEW)) {
      if (response.getSuggestions().isEmpty()) {
        response.suggestions("Start New Case", "Game Introduction", "Leave The Game");
      }
    } else if (session.getState() == State.FINISH) {
      if (response.getSuggestions().isEmpty()) {
        response.suggestions("Start New Case", "Revisit", "Case Introduction", "Final Solution",
            "Exit The Game");
      }
    } else if (session.getState() == State.CASE_STARTED) {
      handleActiveCase(request, response);
    } else if (session.getState() == State.QUESTIONS) {
      if (response.getSuggestions().isEmpty()) {
        response.suggestions("Repeat Question", "Next Question", "Revisit", "Show Clues");
      }
    } else if (session.getState() == State.ANSWERS) {
      if (response.getSuggestions().isEmpty()) {
        response.suggestions("Repeat Question");
      }
    }

    if (response.getSuggestions().isEmpty() && response.getStoryAudioURL() != null) {
      response.suggestions("Read Newspaper");
    }
    if (response.getLinkOutURL() == null) {
      response.linkOut("Investigation Log", config.getSessionURL(request.getSessionid()));
    }
  }

  private void handleActiveCase(SherlockRequest request, SherlockResponseBuilder response) {
    sessionManager.ageActiveSuggestions();
    if (response.getHint() != null && !response.getHint().getSuggestions().isEmpty()) {
      sessionManager.addActiveSuggestions(response.getHint().getSuggestions());
    }
    Session session = sessionManager.getSession();
    if (response.getSuggestions().isEmpty() && !session.getActiveSuggestions().isEmpty()) {
      List<String> top = chooseTop(session.getActiveSuggestions());
      response.suggestions(top);
    }
  }

  private List<String> chooseTop(List<SessionActiveSuggestion> activeSuggestions) {
    int[] randomNum = new int[activeSuggestions.size()];
    Integer[] indexes = new Integer[activeSuggestions.size()];
    Random random = new Random();
    for (int i = 0; i < randomNum.length; i++) {
      randomNum[i] = random.nextInt();
      indexes[i] = i;
    }
    Arrays.sort(indexes, new Comparator<Integer>() {
      @Override
      public int compare(Integer o1, Integer o2) {
        SessionActiveSuggestion s1 = activeSuggestions.get(o1);
        SessionActiveSuggestion s2 = activeSuggestions.get(o2);
        int c = Integer.compare(s1.getRelevancy(), s2.getRelevancy());
        if (c != 0) {
          return -c;
        }
        return Integer.compare(randomNum[o1], randomNum[o2]);
      }
    });
    List<String> resultSuggestions = new ArrayList<>();
    // top 3
    for (int i = 0; i < 3 && i < activeSuggestions.size(); i++) {
      resultSuggestions.add(activeSuggestions.get(indexes[i]).getSuggestion());
    }
    return resultSuggestions;
  }
}
