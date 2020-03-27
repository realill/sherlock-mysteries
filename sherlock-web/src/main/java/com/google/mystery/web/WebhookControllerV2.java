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

package com.google.mystery.web;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import com.google.actions.api.ActionContext;
import com.google.actions.api.ActionRequest;
import com.google.actions.api.ActionResponse;
import com.google.actions.api.impl.DialogflowRequest;
import com.google.actions.api.response.ResponseBuilder;
import com.google.api.client.util.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.mystery.actions.ActionsManager;
import com.google.mystery.actions.SessionManager;
import com.google.mystery.actions.StartCaseActions;
import com.google.mystery.actions.model.SherlockRequest;
import com.google.mystery.actions.model.SherlockRequest.Flag;
import com.google.mystery.actions.model.SherlockRequestBuilder;
import com.google.mystery.actions.model.SherlockResponse;
import com.google.mystery.assets.AssetsManager;
import com.google.mystery.data.model.Session;

@Controller
public class WebhookControllerV2 {
  private static final String DISABLE_CANVAS_CONTEXT = "disable-canvas";
  private static final String RESPONSE_CONTENT_TYPE = "application/json";
  private static final String RESPONSE_CHARACTER_ENCODING = "utf-8";
  private static final String CAPABILITY_SCREEN_OUTPUT = "actions_capability_screen_output";
  private static final String CAPABILITY_WEB_BROWSER = "actions_capability_web_browser";
  private static final String CAPABILITIES_CANVAS = "actions_capability_interactive_canvas";

  @Inject
  private ActionsManager actionsManager;
  @Inject
  private SessionManager sessionManager;
  @Inject
  private AssetsManager assetsManager;
  @Inject
  private DFV2ResponseGenerator responseGenerator;

  private Logger logger = Logger.getLogger(this.getClass().getName());

  @PostMapping("/webhook")
  public void webhook(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    String content = IOUtils.toString(request.getReader());
    ImmutableMap.Builder<String, String> headers = ImmutableMap.builder();
    Enumeration<String> h = request.getHeaderNames();
    while (h.hasMoreElements()) {
      String headerName = h.nextElement();
      headers.put(headerName, request.getHeader(headerName));
    }


    DialogflowRequest dfRequest = DialogflowRequest.Companion.create(content, headers.build());

    ActionResponse actionResponse = handleRequest(dfRequest);
    response.setCharacterEncoding(RESPONSE_CHARACTER_ENCODING);
    response.setContentType(RESPONSE_CONTENT_TYPE);
    response.getWriter().append(actionResponse.toJson());
  }

  private ActionResponse handleRequest(ActionRequest actionRequest) {
    ResponseBuilder webhookResponse = new ResponseBuilder(true, actionRequest.getSessionId(),
        actionRequest.getConversationData(), actionRequest.getUserStorage());
    // if action is empty, just proxy response back.
    if (Strings.isNullOrEmpty(actionRequest.getWebhookRequest().getQueryResult().getAction())) {
      return webhookResponse.build();
    }
    String sessionid = extractSessionid(actionRequest);
    sessionManager.initSession(sessionid);
    Session session = sessionManager.getSession();

    String caseDataId = assetsManager.getCaseDataId(session);
    if (caseDataId == null && session.getState() != Session.State.NEW) {
      logger.warning("Session without caseDataId " + session.toString());
    }

    ImmutableMap.Builder<String, String> paramsBuilder = ImmutableMap.builder();
    if (actionRequest.getWebhookRequest().getQueryResult().getParameters() != null) {
      for (Map.Entry<String, Object> e : actionRequest.getWebhookRequest().getQueryResult()
          .getParameters().entrySet()) {
        paramsBuilder.put(e.getKey(), e.getValue().toString());
      }
    }
    Map<String, String> params = paramsBuilder.build();

    SherlockRequestBuilder sherlockRequestBuilder = new SherlockRequestBuilder(
        actionRequest.getWebhookRequest().getQueryResult().getQueryText(), sessionid,
        actionRequest.getWebhookRequest().getQueryResult().getAction()).parameters(params)
            .caseDataId(caseDataId);

    boolean foundScreen = false;
    for (ActionContext c : actionRequest.getContexts()) {
      if (c.getName().endsWith(CAPABILITY_SCREEN_OUTPUT)) {
        foundScreen = true;
      } else if (c.getName().endsWith(CAPABILITY_WEB_BROWSER)) {
        sherlockRequestBuilder.addFlag(SherlockRequest.Flag.WEB_BROWSER);
      } else if (c.getName().endsWith(StartCaseActions.CASE_START_CONTEXT)) {
        sherlockRequestBuilder.addFlag(SherlockRequest.Flag.CASE_START);
      } else if (c.getName().endsWith(CAPABILITIES_CANVAS)
          && !hasContext(actionRequest, DISABLE_CANVAS_CONTEXT)) {
        sherlockRequestBuilder.addFlag(Flag.CANVAS);
      }
    }

    if (!foundScreen) {
      sherlockRequestBuilder.addFlag(SherlockRequest.Flag.SPEAKER);
    }

    SherlockRequest sherlockRequest = sherlockRequestBuilder.build();
    logger.info(String.format("sessionid=%s, action=%s, params=%s", sessionid,
        sherlockRequest.getAction(), params));

    SherlockResponse sherlockResponse = actionsManager.request(sherlockRequest);
    responseGenerator.generate(sherlockRequest, sherlockResponse, sessionid, webhookResponse);

    sessionManager.commitSession();
    return webhookResponse.build();
  }

  /**
   * Check if request has context with given name.
   */
  private boolean hasContext(ActionRequest actionRequest, String context) {
    for (ActionContext c : actionRequest.getContexts()) {
      if (c.getName().toLowerCase().endsWith(context.toLowerCase())) {
        return true;
      }
    }
    return false;
  }

  private String extractSessionid(ActionRequest actionRequest) {
    if (actionRequest.getUser() != null) {
      if (actionRequest.getUserStorage() != null
          && actionRequest.getUserStorage().get("sessionid") != null) {
        return actionRequest.getUserStorage().get("sessionid").toString();
      }
      return UUID.randomUUID().toString();
    }
    return actionRequest.getSessionId();
  }

}
