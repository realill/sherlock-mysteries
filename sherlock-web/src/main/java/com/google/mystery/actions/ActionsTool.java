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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.mystery.actions.messages.MessageException;
import com.google.mystery.actions.messages.MessagesManager;
import com.google.mystery.actions.model.SherlockRequest;
import com.google.mystery.actions.model.SherlockResponseBuilder;
import com.google.mystery.assets.AssetsManager;
import com.google.mystery.config.SherlockConfig;
import com.google.mystery.data.model.Clue;
import com.google.mystery.data.model.Hint;
import com.google.mystery.data.model.Session;
import com.google.mystery.data.model.Story;

public class ActionsTool {
  @Inject
  private SherlockConfig config;
  @Inject
  private MessagesManager messages;
  @Inject
  private AssetsManager assetsManager;
  @Inject
  private SessionManager sessionManager;

  /**
   * Showing story introduction.
   */
  public void caseIntroduction(SherlockRequest request, SherlockResponseBuilder response)
      throws MessageException, IOException {
    response.prestoryText(messages.message("repeatCaseIntroduction"));
    Story story = getStory(request.getCaseDataId(), Story.CASE_INTRODUCTION_ID);
    response.story(story);
    openInBrowser(request, response);
  }

  /**
   * Showing final solution again.
   */
  public void finalSolution(SherlockRequest request, SherlockResponseBuilder response)
      throws MessageException, IOException {
    response.prestoryText(messages.message("repeatFinalSolution"));
    Story story = getStory(request.getCaseDataId(), Story.FINAL_SOLUTION_ID);
    response.story(story);
    openInBrowser(request, response);
  }

  private static final Logger logger = Logger.getLogger(ActionsTool.class.getName());

  public void showADLink(SherlockResponseBuilder response) {
    response.imageUrlIfNotPresent(config.getPlaceholderUrl(), "Sherlock Mysteries");
    response.linkOut("Sherlock Mysteries", config.getAssistantDirectoryUrl());
  }

  public void message(SherlockResponseBuilder response, String message)
      throws IOException, MessageException {
    response.storyText(messages.message(message));
  }

  public void message(SherlockResponseBuilder response, String message, Map<String, Object> context)
      throws IOException, MessageException {
    response.storyText(messages.message(message, context));
  }

  public Story getStory(String caseDataId, String storyid) {
    return assetsManager.getStory(caseDataId, storyid);
  }

  public void openInBrowser(SherlockRequest request, SherlockResponseBuilder response) {
    response.linkOut("in Browser", config.getSessionURL(request.getSessionid()));
  }

  /**
   * Check for hints and clues after visiting new story place. It will add new hints and clues and
   * modify response to reflect it.
   *
   * <p>
   * TODO: should be moved into other place.
   */
  public void handleHintsCluesAndSuggestions(SherlockRequest request, Story story,
      SherlockResponseBuilder response) throws MessageException, IOException {
    Session session = sessionManager.getSession();
    List<Hint> allHints = assetsManager.getAllHints(request.getCaseDataId());
    Set<String> locations = Sets.newHashSet(session.getLocationsBacklog());
    locations.add(story.getId());
    Hint finalHint = null;
    for (Hint hint : allHints) {
      if (!session.getUsedHints().contains(hint.getId())
          && locations.containsAll(hint.getPrecondition())) {
        finalHint = hint;
        break;
      }
    }

    if (finalHint != null) {
      response.hint(finalHint);
    }

    List<Clue> clues = Lists.newArrayList();
    for (String clueid : story.getClues()) {
      if (!session.getClues().contains(clueid)) {
        Clue clue = assetsManager.getClue(request.getCaseDataId(), clueid);
        if (clue != null) {
          clues.add(clue);
          response.addClue(clue);
          if (clue.getImageUrl() != null) {
            response.imageUrlIfNotPresent(clue.getImageUrl(), clue.getName());
          }
        } else {
          logger.warning(String.format("Clue with clueid=%s not found", clueid));
        }
      }
    }

    ImmutableMap.Builder<String, Object> mapBuilder = ImmutableMap.builder();
    mapBuilder.put("clues", clues);
    if (finalHint != null) {
      mapBuilder.put("hint", finalHint);
    }
    mapBuilder.put("isspeaker", request.isSpeaker());
    String afterStory = messages.message("afterStory", mapBuilder.build()).trim();
    response.afterstoryText(afterStory);

    if (response.getStoryAudioURL() != null && !request.isCanvas()) {
      response.continueSuggestions();
    }
  }

  public void error(SherlockResponseBuilder response) {
    try {
      response.storyText(messages.message("error"));
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Error loading error message", e);
      response.storyText("Game got unexpected error");
    }
  }
}
