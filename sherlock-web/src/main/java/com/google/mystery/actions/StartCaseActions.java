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
import javax.inject.Inject;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.mystery.actions.messages.MessageException;
import com.google.mystery.actions.model.SherlockAction;
import com.google.mystery.actions.model.SherlockRequest;
import com.google.mystery.actions.model.SherlockRequestBuilder;
import com.google.mystery.actions.model.SherlockResponseBuilder;
import com.google.mystery.actions.model.SherlockScreenData.Type;
import com.google.mystery.assets.AssetsManager;
import com.google.mystery.data.model.Case;
import com.google.mystery.data.model.Session;
import com.google.mystery.data.model.Session.State;
import com.google.mystery.data.model.Story;
import freemarker.log.Logger;

public class StartCaseActions {

  private static final String CASE_ID_ATTRIBUTE = "caseId";
  public static final String CASE_START_CONTEXT = "case-start";
  @Inject
  private AssetsManager assetsManager;
  @Inject
  private SessionManager sessionManager;
  @Inject
  private ActionsTool actionsTool;
  private Logger logger = Logger.getLogger(StartCaseActions.class.getName());

  @SherlockAction(value = "start-case-confirm", requireCase = false)
  public void startCaseConfirm(SherlockRequest request, SherlockResponseBuilder response)
      throws MessageException, IOException {
    String caseId = request.getParameter(CASE_ID_ATTRIBUTE);
    String caseParameter = request.getParameter("case");
    if (caseParameter != null) {
      List<Case> cases = assetsManager.getAllEnabledCases();
      for (Case c : cases) {
        if (c.getName().equalsIgnoreCase(caseParameter)) {
          caseId = c.getCaseId();
          break;
        }
      }
    }
    if (caseId != null) {
      startCaseInternal(request, response, caseId);
    } else {
      logger.error("Can not find caseid.");
    }
  }

  protected void startCaseInternal(SherlockRequest request, SherlockResponseBuilder response,
      String caseId) throws IOException, MessageException {
    String caseDataId = assetsManager.getCaseDataId(caseId);
    if (caseDataId == null) {
      actionsTool.message(response, "newCase-noSuchCase");
      return;
    }
    sessionManager.startCase(request.getSessionid(), caseId);
    SherlockRequest newRequest = new SherlockRequestBuilder(request).caseDataId(caseDataId).build();
    Story story = actionsTool.getStory(newRequest.getCaseDataId(), "caseIntroduction");
    String prestory =
        actionsTool.getStory(newRequest.getCaseDataId(), "caseStartPrestory").getText();
    response.story(story).prestoryText(prestory);
    if (story.getImageUrl() != null) {
      response.imageUrlIfNotPresent(story.getImageUrl(), story.getTitle());
    }

    actionsTool.handleHintsCluesAndSuggestions(newRequest, story, response);

    sessionManager.pushSessionLog(story, response);
    actionsTool.openInBrowser(request, response);
    response.addOutContext(CASE_START_CONTEXT, 1);
  }

  @SherlockAction(value = "start-case", requireCase = false)
  public void startCase(SherlockRequest request, SherlockResponseBuilder response)
      throws MessageException, IOException {
    Session session = sessionManager.getSession();
    String caseId = request.getParameter(CASE_ID_ATTRIBUTE);

    if (Strings.isNullOrEmpty(caseId)) {
      List<Case> cases = assetsManager.getAllEnabledCases();
      if (cases.size() == 0) {
        actionsTool.message(response, "newCase-noPlayableCases");
        return;
      } else if (cases.size() == 1) {
        caseId = cases.get(0).getCaseId();
      } else {
        actionsTool.message(response, "newCase-selection", ImmutableMap.of("cases", cases));
        response.addOutContext("case-selection");
        if (cases.size() <= 1) {
          for (Case c : cases) {
            if (c.getAlternativeNames().size() > 0) {
              response.addSuggestion(c.getAlternativeNames().get(0));
            } else {
              response.addSuggestion(c.getName());
            }
          }
        } else {
          for (Case c : cases) {
            response.addCard(c.getName(), c.getCategory(), c.getImageUrl(), c.getName(),
                c.getName(), c.getAlternativeNames());
          }
          response.cardsTitle("Choose New Case");
          response.screenDataType(Type.BASE, false);
        }
        return;
      }
    }
    // caseId is never null here.
    if (session.getState() == Session.State.NEW || session.getState() == State.FINISH) {
      startCaseInternal(request, response, caseId);
    } else {
      Case c = assetsManager.getCase(caseId);
      if (c != null) {
        actionsTool.message(response, "newCase-caseAlreadyStarted", ImmutableMap.of("case", c));
        response.addOutContext("start-case-confirmation");
        response.addParameter(CASE_ID_ATTRIBUTE, caseId);
        response.suggestions("Case Introduction", "Yes I am sure");
      } else {
        actionsTool.message(response, "newCase-noSuchCase");
      }
    }
  }

  public void setActionsTool(ActionsTool actionsTool) {
    this.actionsTool = actionsTool;
  }
}
