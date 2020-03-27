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

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.test.web.servlet.ResultActions;
import com.google.api.services.dialogflow_fulfillment.v2.model.Context;
import com.google.common.truth.Truth;
import com.google.gson.JsonElement;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.InvalidJsonException;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.jayway.jsonpath.spi.json.GsonJsonProvider;

public class TesterResponse {
  private DocumentContext response;
  private static Logger logger = Logger.getLogger(TesterResponse.class.getName());
  Configuration configuration =
      Configuration.defaultConfiguration().jsonProvider(new GsonJsonProvider());

  public TesterResponse(ResultActions mvcResult) {
    try {
      response = JsonPath.using(configuration)
          .parse(mvcResult.andReturn().getResponse().getContentAsString());
    } catch (InvalidJsonException | UnsupportedEncodingException e) {
      logger.log(Level.SEVERE, "getting json unexpeted error", e);
    }
  }

  public String getSsml() {
    return response
        .read(
            "$.payload.google.richResponse.items[?(@.simpleResponse != null)].simpleResponse.ssml")
        .toString();
  }

  public List<Context> getContexts() {
    List<Context> contexts = new ArrayList<>();
    JsonElement element = response.read("$.outputContexts");
    if (element.isJsonArray()) {
      for (JsonElement e : element.getAsJsonArray()) {
        Context context = new Context();
        context.setName(e.getAsJsonObject().get("name").getAsString());
        contexts.add(context);
      }
    }
    return contexts;
  }

  public TesterResponse assertSsmlContains(String string) {
    Truth.assertThat(getSsml().toLowerCase()).contains(string.toLowerCase());
    return this;
  }

  public TesterResponse assertSsmlDoesNotContain(String string) {
    Truth.assertThat(getSsml().toLowerCase()).doesNotContain(string.toLowerCase());
    return this;
  }

  public TesterResponse assertOutContext(String contextName) {
    for (Context context : getContexts()) {
      if (context.getName().toLowerCase().endsWith(contextName)) {
        return this;
      }
    }
    Truth.assert_().fail("There are not context with %s", contextName);
    return null;
  }

  public void assertLinkOutContains(String string) {
    response.read("$.payload.google.richResponse.linkOutSuggestion.openUrlAction.url").toString();
  }

  public void assertEndConversation() {
    response.read("$.payload.google.expectUserResponse").equals(false);
  }

  public void assertNotEndConversation() {
    try {
      response.read("$.payload.google.expectUserResponse").equals(false);
    } catch (PathNotFoundException e) {
      return;
    }
  }

  public boolean hasSsml() {
    try {
      response.read(
          "$.payload.google.richResponse.items[?(@.simpleResponse != null)].simpleResponse.ssml")
          .toString();
      return true;
    } catch (PathNotFoundException e) {
      return false;
    }
  }
}
