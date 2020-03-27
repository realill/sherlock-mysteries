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
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.commons.lang3.reflect.MethodUtils;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.mystery.Utils;
import com.google.mystery.actions.messages.MessageException;
import com.google.mystery.actions.messages.MessagesManager;
import com.google.mystery.actions.model.SherlockAction;
import com.google.mystery.actions.model.SherlockRequest;
import com.google.mystery.actions.model.SherlockResponse;
import com.google.mystery.actions.model.SherlockResponseBuilder;
import com.google.mystery.actions.model.SherlockScreenData.Type;
import com.google.mystery.config.SherlockConfig;
import com.google.mystery.data.model.Session;
import com.google.mystery.data.model.Session.State;
import com.google.mystery.data.model.Story;

/**
 * Handles different api.ai agent actions.
 *
 * @author ilyaplatonov
 */
@Singleton
public class ActionsManager {
  private Map<String, BiConsumer<SherlockRequest, SherlockResponseBuilder>> actionsFunctions = null;
  private Map<String, SherlockAction> actions = null;
  private static final Logger logger = Logger.getLogger(ActionsManager.class.getName());

  @Inject
  private SessionManager sessionManager;
  @Inject
  private ActionsTool actionsTool;
  @Inject
  private SherlockConfig config;
  @Inject
  private SuggestionsManager suggestionsManager;
  @Inject
  private MessagesManager messages;

  @Inject
  private StartCaseActions startCaseActions;
  @Inject
  private NavigationActions navigationActions;
  @Inject
  private FinishActions finishActions;

  public SherlockResponse request(SherlockRequest request) {
    BiConsumer<SherlockRequest, SherlockResponseBuilder> actionFunction =
        actionsFunctions.get(request.getAction().toLowerCase());
    SherlockAction action = actions.get(request.getAction().toLowerCase());

    try {
      SherlockResponseBuilder responseBuilder = new SherlockResponseBuilder();
      final Session session = sessionManager.getSession();
      // indicates that one of prerequisites are not met
      boolean stopAction = false;
      if (actionFunction == null) {
        logger.severe("action=" + request.getAction() + " not found");
        actionsTool.error(responseBuilder);
        return responseBuilder.build();
      }
      if (action != null && action.requireCase()) {
        if (session.getState() == Session.State.NEW) {
          actionsTool.message(responseBuilder, "caseRequired");
          stopAction = true;
        }
      }
      if (!stopAction && action.state().length != 0) {
        if (!Arrays.stream(action.state()).anyMatch(s -> s.equals(session.getState()))) {
          logger.severe(String.format("%s action requires one of %s states", action.value(),
              Arrays.toString(action.state())));
          welcome(request, responseBuilder);
          stopAction = true;
        }
      }
      if (!stopAction) {
        actionFunction.accept(request, responseBuilder);
      }
      // session could have changed after action
      Session newSession = sessionManager.getSession();
      if (newSession.getState() == State.QUESTIONS) {
        responseBuilder.addOutContext("question", 1);
      } else if (newSession.getState() == State.ANSWERS) {
        responseBuilder.addOutContext("answer", 1);
      }
      // Adding suggestions
      suggestionsManager.suggest(request, responseBuilder);

      SherlockResponse response = responseBuilder.build();
      // saving clues and hint into session if any
      sessionManager.addClue(response.getClues());
      if (response.getHint() != null) {
        sessionManager.addHint(response.getHint());
      }
      return response;
    } catch (Throwable t) {
      logger.log(Level.SEVERE, request.getAction() + " action calling error", t);
      SherlockResponseBuilder responseBuilder = new SherlockResponseBuilder();
      actionsTool.error(responseBuilder);
      return responseBuilder.build();
    }
  }

  @SherlockAction(value = "test", requireCase = false)
  public void test(SherlockRequest request, SherlockResponseBuilder response)
      throws MessageException, IOException {
    response.storyText("This is test response");
  }

  @SherlockAction(value = "input.unknown", requireCase = false)
  public void inputUnknown(SherlockRequest request, SherlockResponseBuilder response)
      throws MessageException, IOException {
    Session session = sessionManager.getSession();
    if (session.getState() == State.CASE_STARTED && !Strings.isNullOrEmpty(request.getInput())) {
      if (navigationActions.lookupQuery(request, response, request.getInput(), null, null)) {
        return;
      }
    }

    actionsTool.message(response, "unknown");
  }

  @SherlockAction(value = "case-introduction", requireCase = true)
  public void caseIntroduction(SherlockRequest request, SherlockResponseBuilder response)
      throws MessageException, IOException {
    actionsTool.caseIntroduction(request, response);
  }

  @SherlockAction(value = "how-to-play", requireCase = false)
  public void whoToPlay(SherlockRequest request, SherlockResponseBuilder response)
      throws MessageException, IOException {
    actionsTool.message(response, "howToPlay", ImmutableMap.of("isspeaker", request.isSpeaker()));
    howtoplayImage(request, response);
    response.screenDataType(Type.BASE);
  }

  @SherlockAction(value = "exit", requireCase = false)
  public void exit(SherlockRequest request, SherlockResponseBuilder response)
      throws MessageException, IOException {
    actionsTool.message(response, "bye");
    response.endConversation(true);
  }

  @SherlockAction(value = "reset-session", requireCase = false)
  public void resetSession(SherlockRequest request, SherlockResponseBuilder response)
      throws MessageException, IOException {
    sessionManager.resetSession(request.getSessionid());
    actionsTool.message(response, "sessionReset");
  }

  @SherlockAction(value = "story", requireCase = false)
  public void story(SherlockRequest request, SherlockResponseBuilder response) {
    if (request.getParameter("id") == null) {
      logger.warning("story action without id" + request.getParameters());
      actionsTool.error(response);
    }
    String assetId = request.getParameter("id").toString();

    storyAsset(request, response, assetId);
  }

  @SherlockAction(value = "game-introduction", requireCase = false)
  public void gameIntroduction(SherlockRequest request, SherlockResponseBuilder response)
      throws MessageException, IOException {
    actionsTool.message(response, "gameIntroduction");
    howtoplayImage(request, response);
    response.screenDataType(Type.BASE);
  }

  private void howtoplayImage(SherlockRequest request, SherlockResponseBuilder response) {
    response.linkOut("How To Play", config.getURL(request.getSessionid(), "howtoplay/", null));
    response.imageUrlIfNotPresent(
        Utils.checkedURL(
            "https://storage.googleapis.com/" + config.getBucketName() + "/images/background.png"),
        "How To Play");
  }

  @SherlockAction(value = "media-followup", requireCase = false)
  public void mediaFollowup(SherlockRequest request, SherlockResponseBuilder response)
      throws MessageException, IOException {
    Session session = sessionManager.getSession();
    if (handleFollowup(request, response, session)) {
      if (session.getState() == State.ANSWERS) {
        // answer follow up is question. Hacky ..
        response.confirmRejectSuggestions();
        response.addOutContext("validate-answer-confirm", 3);
      }
    } else if (session.getState() == State.CASE_STARTED) {
      actionsTool.message(response, "whatIsNext");
    } else {
      welcome(request, response);
    }

  }

  @SherlockAction(value = "new-surface-followup", requireCase = false)
  public void newSurfaceFollowup(SherlockRequest request, SherlockResponseBuilder response)
      throws MessageException, IOException {
    if (request.hasBrowser()) {
      logLink(request, response);
    } else {
      Session session = sessionManager.getSession();
      if (session.getState() == State.CASE_STARTED) {
        actionsTool.message(response, "whatIsNext");
      } else {
        welcome(request, response);
      }
    }
  }

  /**
   * Checks if session has followup and if found puts it into response and removes it from session
   *
   * @return if follow up is found
   * @throws MessageException
   * @throws IOException
   */
  private boolean handleFollowup(SherlockRequest request, SherlockResponseBuilder response,
      Session session) throws IOException, MessageException {
    if (session.getState() == State.CASE_STARTED || session.getState() == State.ANSWERS) {
      if (session.getFollowupText() != null) {
        // asking for transfer to other device
        if (request.isSpeaker() && request.isNewCase()) {
          response.storyText("");
          response.otherSurface("Investigation Log",
              session.getFollowupText() + "\n" + messages.message("otherSurfaceMessage"));
        } else {
          response.storyText("");
          response.afterstoryText(session.getFollowupText());
        }
        sessionManager.setFollowupText(null);
        return true;
      }
      if (request.isSpeaker() && request.isNewCase()) {
        response.otherSurface("Investigation Log", messages.message("otherSurfaceMessage"));
      }
    }
    return false;
  }

  @SherlockAction(value = "welcome", requireCase = false)
  public void welcome(SherlockRequest request, SherlockResponseBuilder response)
      throws MessageException, IOException {
    Session session = sessionManager.getSession();
    if (session.getState() == State.NEW) {
      actionsTool.message(response, "welcomeNew");
      response.screenDataType(Type.MENU);
    } else if (session.getState() == State.CASE_STARTED) {
      if (!handleFollowup(request, response, session)) {
        actionsTool.message(response, "welcomeCase");
      }
    } else if (session.getState() == State.QUESTIONS) {
      finishActions.finishWelcome(request, response);
    } else if (session.getState() == State.ANSWERS) {
      // TODO handles final solution followup
      if (handleFollowup(request, response, session)) {
        response.confirmRejectSuggestions();
        response.addOutContext("validate-answer-confirm", 3);
      } else {
        finishActions.finishWelcome(request, response);
      }

    } else if (session.getState() == State.FINISH) {
      FinalStatus stats = finishActions.stats(request.getCaseDataId(), sessionManager.getSession());
      actionsTool.message(response, "welcomeFinish", ImmutableMap.of("stats", stats));
      actionsTool.showADLink(response);
      response.screenDataType(Type.MENU);
    }
  }

  @SherlockAction(value = "log-link", requireCase = false)
  public void logLink(SherlockRequest request, SherlockResponseBuilder response)
      throws IOException, MessageException {
    if (request.hasBrowser()) {
      response.storyText(messages.message("log-link"));
      logoImage(response);
      response.linkOut("Investigation Log", config.getSessionURL(request.getSessionid()));
    } else {
      response.storyText("PLACEHOLDER");
      response.otherSurface("Investigation Log", messages.message("otherSurfaceMessage"));
    }
  }

  public void logoImage(SherlockResponseBuilder response) {
    response.imageUrlIfNotPresent(config.getPlaceholderUrl(), "Sherlock Mysteries");
  }

  protected Story storyAsset(SherlockRequest request, SherlockResponseBuilder response, String id) {
    Story story = actionsTool.getStory(request.getCaseDataId(), id);
    storyAsset(response, story);
    return story;
  }

  /**
   * @param prestory prefix to add before telling story
   * @param id
   * @return
   */
  protected void storyAsset(SherlockResponseBuilder response, Story story) {
    if (story == null) {
      response.storyText("No asset found");
      return;
    }
    response.story(story);
  }

  @PostConstruct
  public void init() {
    actionsFunctions = new HashMap<>();
    actions = new HashMap<>();
    initActions(this);
    initActions(startCaseActions);
    initActions(navigationActions);
    initActions(finishActions);
  }

  /** Lookups for all @SherlockAction annotations and initializes {@link #actionsFunctions} */
  protected void initActions(final Object object) {
    for (final Method method : MethodUtils.getMethodsWithAnnotation(object.getClass(),
        SherlockAction.class, true, true)) {
      final SherlockAction sherlockAction =
          MethodUtils.getAnnotation(method, SherlockAction.class, true, true);
      for (String action : sherlockAction.value()) {
        actionsFunctions.put(action.toLowerCase(),
            new BiConsumer<SherlockRequest, SherlockResponseBuilder>() {
              @Override
              public void accept(SherlockRequest t, SherlockResponseBuilder u) {
                try {
                  method.invoke(object, t, u);
                } catch (Exception e) {
                  throw new IllegalArgumentException(sherlockAction.value() + " calling execption",
                      e);
                }
              }
            });
        actions.put(action.toLowerCase(), sherlockAction);
      }
    }
  }

  public void setActionsTool(ActionsTool actionsTool) {
    this.actionsTool = actionsTool;
  }
}
