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

package com.google.mystery.mvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import org.apache.commons.lang3.text.WordUtils;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import com.google.api.services.dialogflow_fulfillment.v2.model.Context;
import com.google.api.services.dialogflow_fulfillment.v2.model.QueryResult;
import com.google.api.services.dialogflow_fulfillment.v2.model.WebhookRequest;
import com.google.gson.Gson;
import com.google.gson.JsonPrimitive;

/**
 * Uses spring mvc testing tools to test sherlock mystery engine as blackbox. This is not
 * thread-safe implementation and have state. You need to call {@link #restartSession()} between
 * tests
 *
 * @author ilyaplatonov
 */
public class MysteryMvcTester {
  public static final String APPLICATION_JSON_UTF8_VALUE = "application/json;charset=UTF-8";
  public static final MediaType APPLICATION_JSON_UTF8 =
      MediaType.valueOf(APPLICATION_JSON_UTF8_VALUE);
  @Inject
  private WebApplicationContext wac;
  protected MockMvc mvc;
  private String sessionId = UUID.randomUUID().toString();
  private Gson gson = new Gson();

  @PostConstruct
  public void init() {
    this.mvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    // Configuration.defaultConfiguration();
  }

  public void restartSession() {
    sessionId = UUID.randomUUID().toString();
  }

  public TesterResponse request(TesterRequest action) throws Exception {
    WebhookRequest webhookRequest = new WebhookRequest();
    webhookRequest.setSession(sessionId);

    QueryResult queryResult = new QueryResult();
    webhookRequest.setQueryResult(queryResult);

    queryResult.setLanguageCode("en");
    queryResult.setAction(action.getAction());
    queryResult.setQueryText(action.getInput());

    if (!action.getParameters().isEmpty()) {
      Map<String, Object> parameters = new HashMap<>();
      for (Entry<String, String> entry : action.getParameters().entrySet()) {
        parameters.put(entry.getKey(), new JsonPrimitive(entry.getValue()));
      }
      queryResult.setParameters(parameters);
    }
    if (!action.getContexts().isEmpty()) {
      List<Context> contexts = new ArrayList<>();
      for (String context : action.getContexts()) {
        contexts.add(new Context().setName(context));
      }
      queryResult.setOutputContexts(contexts);
    }
    queryResult.setFulfillmentText(action.getSpeech());

    ResultActions mvcResult = mvc.perform(
        post("/webhook").contentType(APPLICATION_JSON_UTF8).content(gson.toJson(webhookRequest)))
        .andExpect(status().isOk());

    TesterResponse tr = new TesterResponse(mvcResult);
    if (tr.hasSsml()) {
      Logger.getLogger(this.getClass().getName())
          .info(String.format("%s => %s", action.getAction(), WordUtils.wrap(tr.getSsml(), 150)));
    } else {
      Logger.getLogger(this.getClass().getName())
          .info((String.format("%s => empty response", action.getAction())));
    }
    return tr;
  }

  public TesterResponse navigate(String location) throws Exception {
    return request(TesterRequest.action("navigate").param("location", location));
  }

  public TesterResponse startCase() throws Exception {
    return request(TesterRequest.action("start-case"));
  }

  public String getSessionId() {
    return sessionId;
  }

  public TesterResponse lookup(String query) throws Exception {
    return request(TesterRequest.action("lookup").param("query", query));
  }

  public TesterResponse action(String string) throws Exception {
    return request(TesterRequest.action(string));
  }
}
