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
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.apache.commons.text.WordUtils;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.mystery.actions.messages.MessageException;
import com.google.mystery.actions.messages.MessagesManager;
import com.google.mystery.actions.model.SherlockAction;
import com.google.mystery.actions.model.SherlockRequest;
import com.google.mystery.actions.model.SherlockResponse;
import com.google.mystery.actions.model.SherlockResponseBuilder;
import com.google.mystery.actions.model.SherlockScreenData.Type;
import com.google.mystery.assets.AssetsManager;
import com.google.mystery.config.SherlockConfig;
import com.google.mystery.data.model.Clue;
import com.google.mystery.data.model.DirectoryEntry;
import com.google.mystery.data.model.Session;
import com.google.mystery.data.model.Session.State;
import com.google.mystery.data.model.Story;

/**
 * Provides navigation and lookup actions and logic.
 *
 * @author ilyaplatonov
 */
public class NavigationActions {
  @Inject
  private AssetsManager assetsManager;
  @Inject
  private SessionManager sessionManager;
  @Inject
  private ActionsTool actionsTool;
  @Inject
  private SherlockConfig config;
  @Inject
  private MessagesManager messages;

  private static final Logger logger = Logger.getLogger(NavigationActions.class.getName());

  @SherlockAction(value = "navigate", requireCase = true)
  public void navigate(SherlockRequest request, SherlockResponseBuilder response)
      throws MessageException, IOException {
    if (request.getParameter("location") == null) {
      logger.warning("navigate action without parameter" + request.getParameters());
      actionsTool.error(response);
      return;
    }
    String location = request.getParameter("location").toString();
    if (assetsManager.checkLocation(location) != null) {
      location = assetsManager.checkLocation(location);
    }
    doNavigate(request, null, location, response);
  }

  /** @param query can be <code>null</code> */
  protected void doNavigate(SherlockRequest request, String query, String location,
      SherlockResponseBuilder response) throws IOException, MessageException {
    Story story = actionsTool.getStory(request.getCaseDataId(), location);
    if (story != null) {
      Session session = sessionManager.getSession();
      String prestory;
      if (query == null || story.getTitle().toLowerCase().contains(query.toLowerCase())) {
        prestory = messages.message("navigate-pre", ImmutableMap.of("title", story.getTitle()));
      } else {
        prestory = messages.message("navigate-pre",
            ImmutableMap.of("query", query, "title", story.getTitle()));
      }
      response.story(story).prestoryText(prestory);

      // TODO: hacky redo
      if (Strings.isNullOrEmpty(response.build().getAfterstoryText()) && request.isSpeaker()) {
        response.endConversation(true);
      }
      if (session.getState() == State.CASE_STARTED
          && !session.getLocationsBacklog().contains(location)) {
        actionsTool.handleHintsCluesAndSuggestions(request, story, response);
        sessionManager.addLocation(location);
        sessionManager.pushSessionLog(story, response);
      }
      response.linkOut("In Browser", config.getSessionURL(request.getSessionid(), story.getId()));
      return;
    }

    response.storyText(messages.message("navigationFail", ImmutableMap.of("location", location)));
  }

  /**
   * Lookup for clues or directory locations
   *
   * <p>
   * params: query, location or clue
   */
  @SherlockAction(value = "lookup", requireCase = true)
  public void lookup(SherlockRequest request, SherlockResponseBuilder response)
      throws MessageException, IOException {
    String query = request.getParameter("query");
    String clueParam = request.getParameter("clue");
    String locationParam = request.getParameter("location");

    if (Strings.isNullOrEmpty(query) && Strings.isNullOrEmpty(clueParam)
        && Strings.isNullOrEmpty(locationParam)) {
      // Asking about what location to investigate.
      actionsTool.message(response, "investigate-question");
      response.addOutContext("location-selection", 1);
      Session session = sessionManager.getSession();
      response.cardsTitle("Previously visited locations");

      if (session.getState() == State.ANSWERS || session.getState() == State.FINISH) {
        Story finalSolution =
            assetsManager.getStory(request.getCaseDataId(), Story.FINAL_SOLUTION_ID);
        addStoryCard(response, finalSolution, "Final Solution");
      }

      // Adding elements in reverse order.
      for (ListIterator<String> i =
          session.getLocationsBacklog().listIterator(session.getLocationsBacklog().size()); i
              .hasPrevious();) {
        String location = i.previous();
        Story story = assetsManager.getStory(request.getCaseDataId(), location);
        addStoryCard(response, story, location);
      }

      Story caseIntroduction =
          assetsManager.getStory(request.getCaseDataId(), Story.CASE_INTRODUCTION_ID);
      addStoryCard(response, caseIntroduction, "Case Introduction");
      return;
    }

    if (!lookupQuery(request, response, query, clueParam, locationParam)) {
      // nothing is found response
      lookupLocation(request, response, query, ImmutableList.of());
    }
  }

  protected void addStoryCard(SherlockResponseBuilder response, Story story, String key) {
    if (story != null) {
      String description =
          WordUtils.wrap(SherlockResponse.textToText(story.getText()), 150).split("\n")[0];
      response.addCard(story.getTitle(), description, story.getImageUrl(), story.getTitle(), key,
          ImmutableList.of());
    }
  }

  /** clue-name is an optional parameter to select clue by name */
  @SherlockAction(value = "list-clues", requireCase = true)
  public void listClues(SherlockRequest request, SherlockResponseBuilder response)
      throws IOException, MessageException {
    Session session = sessionManager.getSession();
    String clueName = !com.google.common.base.Strings.isNullOrEmpty(request.getParameter("clue"))
        ? request.getParameter("clue")
        : request.getParameter("clue-other");

    List<Clue> clues = sessionManager.getSessionClues(request.getCaseDataId());

    Clue clue = null;
    if (!com.google.common.base.Strings.isNullOrEmpty(clueName)) {
      clue = clues.stream().filter(c -> clueName.equalsIgnoreCase(c.getName())).findFirst()
          .orElse(null);
    }
    if (clue != null) {
      lookupClues(request, request.getCaseDataId(), response, clueName, ImmutableList.of(clue),
          session);
    } else {
      actionsTool.message(response, "listClues", ImmutableMap.of("clues", clues));
    }

    if (clues.size() > 1 && clue == null) {
      response.cardsTitle("Clues");
      for (Clue c : clues) {
        response.addCard(c.getName(), c.getDescription(), c.getImageUrl(), c.getName(), c.getName(),
            ImmutableList.of());
      }
      response.screenDataType(Type.BASE, false);
    } else {
      // Showing suggestions
      List<String> suggestions = clues.stream().map(c -> c.getName()).collect(Collectors.toList());
      Collections.shuffle(suggestions);
      suggestions.add(0, "Continue");
      response.suggestions(suggestions);
    }
    if (response.getLinkOutURL() == null) {
      actionsTool.openInBrowser(request, response);
    }
  }

  boolean lookupQuery(SherlockRequest request, SherlockResponseBuilder response, String query,
      String clueParam, String locationParam) throws IOException, MessageException {
    Session session = sessionManager.getSession();
    String location = null;
    if (query != null || locationParam != null) {
      String locationOrEmpty = locationParam == null ? "" : locationParam.toLowerCase();
      String queryOrEmpty = query == null ? "" : query.toLowerCase();
      // This is very hacky, but there is now way to handle this from DF.
      if ("case introduction".equals(queryOrEmpty) || "case introduction".equals(locationOrEmpty)) {
        actionsTool.caseIntroduction(request, response);
        return true;
      } else if ("final solution".equals(queryOrEmpty) || "finish the case".equals(queryOrEmpty)
          || "final solution".equals(locationOrEmpty)) {
        actionsTool.finalSolution(request, response);
        return true;
      }

    }
    if (query != null) {
      location = assetsManager.checkLocation(query);
    }
    if (locationParam != null) {
      location = assetsManager.checkLocation(locationParam);
    }
    // direct location navigation
    if (location != null) {
      if (checkNavigationPermission(session, location)) {
        // TODO: askToNavigate.ftl removal
        doNavigate(request, null, location, response);
      } else {
        response.addOutContext("ask-navigate", 0);
        actionsTool.message(response, "navigationForbiden");
      }
      return true;
    }

    if (locationParam != null) {
      List<DirectoryEntry> dirs = assetsManager.searchDir(request.getCaseDataId(), locationParam);
      lookupLocation(request, response, locationParam, dirs);
      return true;
    }

    if (clueParam != null) {
      List<Clue> clues = assetsManager.searchClues(request.getCaseDataId(), clueParam, session);
      lookupClues(request, request.getCaseDataId(), response, clueParam, clues, session);
      return true;
    }

    if (query != null) {
      List<DirectoryEntry> dirs = assetsManager.searchDir(request.getCaseDataId(), query);
      if (dirs.size() != 0) {
        lookupLocation(request, response, query, dirs);
        return true;
      }
      List<Clue> clues = assetsManager.searchClues(request.getCaseDataId(), query, session);
      if (clues.size() != 0) {
        lookupClues(request, request.getCaseDataId(), response, query, clues, session);
        return true;
      }
    }
    return false;
  }

  private void lookupClues(SherlockRequest request, String caseDataId,
      SherlockResponseBuilder response, String query, List<Clue> foundClues, Session session)
      throws IOException, MessageException {
    if (foundClues.size() == 0) {
      response.storyText(messages.message("lookupCluesNotFound",
          ImmutableMap.of("query", query, "allClues", sessionManager.getSessionClues(caseDataId))));
      return;
    }

    response.screenDataType(Type.BASE);
    if (foundClues.size() == 1) {
      Clue clue = foundClues.get(0);
      // Showing map clue.
      if ("map".equals(clue.getId())) {
        response.screenDataType(Type.MAP);
        response.linkOut("Map", config.getURL(session.getSessionid(), "map", clue.getId()));

        // redirecting to newspaper action
      } else if ("newspaper".equals(clue.getId())) {
        newspaper(request, response);
        return;
      } else {
        response.linkOut("In Browser", config.getCluesURL(session.getSessionid(), clue.getId()));
      }
    }

    response.storyText(
        messages.message("lookupCluesFound", ImmutableMap.of("query", query, "clues", foundClues)));

    for (Clue clue : foundClues) {
      if (clue.getImageUrl() != null) {
        response.imageUrlIfNotPresent(clue.getImageUrl(), clue.getName());
      }
    }
  }

  @SherlockAction(value = "newspaper", requireCase = true)
  public void newspaper(SherlockRequest request, SherlockResponseBuilder response)
      throws MessageException, IOException {
    String article = request.getParameter("article");
    List<Story> articles = assetsManager.getAllAricles(request.getCaseDataId());
    if (Strings.isNullOrEmpty(article)) {
      String text = messages.message("listArticles", ImmutableMap.of("articles", articles));
      response.storyText(text);
    } else {
      Story story = null;
      // Searching for article by title.
      for (Story a : articles) {
        if (article.equalsIgnoreCase(a.getTitle())) {
          story = a;
          break;
        }
      }
      if (story != null) {
        response.storyText(messages.message("article", ImmutableMap.of("article", story)));
      } else {
        // article not found
        response
            .prestoryText(messages.message("articleNotFound", ImmutableMap.of("article", article)));
        response.storyText(messages.message("listArticles", ImmutableMap.of("articles", articles)));
      }
    }

    // searching for newspaper clue and adding its image into response, if exists
    Clue newspaperClue = sessionManager.getSessionClues(request.getCaseDataId()).stream()
        .filter(c -> c.getId().toLowerCase().equals("newspaper")).findFirst().orElse(null);
    if (newspaperClue != null && newspaperClue.getImageUrl() != null) {
      response.imageUrlIfNotPresent(newspaperClue.getImageUrl(), newspaperClue.getName());
    }

    List<String> suggestions = Lists.newArrayList("Continue");
    suggestions.addAll(articles.stream().map(a -> a.getTitle()).collect(Collectors.toList()));
    response.suggestions(suggestions);

    response.linkOut("Newspaper", config.getCluesURL(request.getSessionid(), "newspaper"));
    response.screenDataType(Type.NEWSPAPER);
  }

  /** Checking if this location can be visited in this phase of the game */
  private boolean checkNavigationPermission(Session session, String location) {
    if (session.getState() == State.QUESTIONS) {
      return session.getLocationsBacklog().contains(location.toLowerCase());
    }
    return true;
  }

  private void lookupLocation(SherlockRequest request, SherlockResponseBuilder response,
      String query, List<DirectoryEntry> dirs) throws IOException, MessageException {
    Session session = sessionManager.getSession();
    if (dirs.size() == 0) {
      response.storyText(messages.message("lookupNotFound", ImmutableMap.of("query", query)));
      return;
    }

    if (dirs.size() == 1) {
      DirectoryEntry entry = dirs.get(0);
      if (checkNavigationPermission(session, entry.getLocation())) {
        doNavigate(request, query, entry.getLocation(), response);
      } else {
        response.addOutContext("ask-navigate", 0);
        actionsTool.message(response, "navigationForbiden");
      }
      return;
    }

    int maxSize = 11;
    if (dirs.size() <= maxSize) {
      response.storyText(
          messages.message("lookupFoundMulti", ImmutableMap.of("dirs", dirs, "query", query)));
      return;
    }

    response.storyText(messages.message("lookupFoundTooMuch",
        ImmutableMap.of("dirs", dirs.subList(0, maxSize), "query", query)));
  }

  public void setActionsTool(ActionsTool actionsTool) {
    this.actionsTool = actionsTool;
  }
}
