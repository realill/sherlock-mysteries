// Copyright 2020 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.mystery.web;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import com.google.actions.api.ActionResponse;
import com.google.actions.api.response.ResponseBuilder;
import com.google.api.services.actions_fulfillment.v2.model.BasicCard;
import com.google.api.services.actions_fulfillment.v2.model.HtmlResponse;
import com.google.api.services.actions_fulfillment.v2.model.LinkOutSuggestion;
import com.google.api.services.actions_fulfillment.v2.model.MediaObject;
import com.google.api.services.actions_fulfillment.v2.model.MediaResponse;
import com.google.api.services.actions_fulfillment.v2.model.RichResponseItem;
import com.google.api.services.actions_fulfillment.v2.model.SimpleResponse;
import com.google.api.services.actions_fulfillment.v2.model.Suggestion;
import com.google.api.services.dialogflow_fulfillment.v2.model.Context;
import com.google.common.collect.ImmutableList;
import com.google.common.truth.Truth;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.mystery.actions.SessionManager;
import com.google.mystery.actions.TestUtil;
import com.google.mystery.actions.messages.MessagesManager;
import com.google.mystery.actions.model.SherlockRequest;
import com.google.mystery.actions.model.SherlockRequest.Flag;
import com.google.mystery.actions.model.SherlockRequestBuilder;
import com.google.mystery.actions.model.SherlockResponse;
import com.google.mystery.actions.model.SherlockResponseBuilder;
import com.google.mystery.actions.model.SherlockScreenData.Type;
import com.google.mystery.assets.AssetsManager;
import com.google.mystery.config.SherlockConfig;
import com.google.mystery.data.model.Story;

@RunWith(MockitoJUnitRunner.class)
public class DFV2ResponseGeneratorTest {
  @Spy
  private SherlockConfig config;
  @InjectMocks
  private DFV2ResponseGenerator generator;
  @Mock
  private SessionManager sessionManager;
  @Mock
  private AssetsManager assetsManager;
  @Mock
  private MessagesManager messangesManager;

  private static String DIALOGFLOW_SESSION = "projects/my-project/agent/sessions/my-session";


  @Test
  public void simpleCanvas() throws Exception {
    TestUtil.mockCaseStartedSession(sessionManager);

    SherlockRequest request = new SherlockRequestBuilder("input", TestUtil.CASE_SESSIONID, "action")
        .addFlag(Flag.CANVAS).build();


    ActionResponse response = generate(request,
        SherlockResponse.builder("text").addSuggestion("sug1").addSuggestion("sug2").build());

    HtmlResponse html = getHtmlResponse(response);
    Truth.assertThat(html).isNotNull();
    Truth.assertThat(html.getUrl()).endsWith("base.html");
    Truth.assertThat(html.getSuppressMic()).isFalse();
    Truth.assertThat(html.getUpdatedState().get("suggestions")).isNotNull();
    List<String> suggestions = (List<String>) html.getUpdatedState().get("suggestions");
    Truth.assertThat(suggestions).containsExactly("sug1", "sug2");

    Truth.assertThat(html.getUpdatedState().get("html")).isEqualTo("text");

    SimpleResponse simple = getSimpleResponse(response);
    Truth.assertThat(simple.getDisplayText()).isEqualTo("text");
  }

  @Test
  public void newspaperCanvas() throws Exception {
    TestUtil.mockCaseStartedSession(sessionManager);
    Story story1 = TestUtil.story("article1");
    Story story2 = TestUtil.story("article2");

    SherlockRequest request = new SherlockRequestBuilder("input", TestUtil.CASE_SESSIONID, "action")
        .addFlag(Flag.CANVAS).build();

    Mockito.when(assetsManager.getAllAricles(eq(request.getCaseDataId())))
        .thenReturn(ImmutableList.of(story1, story2));
    Mockito.when(messangesManager.message(eq("readAllArticlesHTML"), any()))
        .thenReturn("readAllArticlesHTML messages");

    ActionResponse response = generate(request, SherlockResponse.builder("text")
        .addSuggestion("sug1").addSuggestion("sug2").screenDataType(Type.NEWSPAPER).build());

    HtmlResponse html = getHtmlResponse(response);
    Truth.assertThat(html).isNotNull();
    Truth.assertThat(html.getUrl()).endsWith("story.html");
    Truth.assertThat(html.getSuppressMic()).isFalse();
    Truth.assertThat(html.getUpdatedState().get("suggestions")).isNotNull();
    List<String> suggestions = (List<String>) html.getUpdatedState().get("suggestions");
    Truth.assertThat(suggestions).containsExactly("Continue", "sug1", "sug2");

    Truth.assertThat(html.getUpdatedState().get("html")).isEqualTo("readAllArticlesHTML messages");

    SimpleResponse simple = getSimpleResponse(response);
    Truth.assertThat(simple.getDisplayText()).isEqualTo("text");
  }

  @Test
  public void mapCanvas() throws MalformedURLException {
    TestUtil.mockCaseStartedSession(sessionManager);
    Story story = Story.location(TestUtil.CASE_DATA_ID, "storyid", "Story title", "Story text",
        new URL("http://example.com/story.ogg"), ImmutableList.of(), "10,20",
        new URL("http://example.com/image"));

    SherlockRequest request = new SherlockRequestBuilder("input", TestUtil.CASE_SESSIONID, "action")
        .addFlag(Flag.CANVAS).build();

    Mockito
        .when(assetsManager.getStory(eq(request.getCaseDataId()), eq(Story.CASE_INTRODUCTION_ID)))
        .thenReturn(story);
    ActionResponse response =
        generate(request, SherlockResponse.builder("text").screenDataType(Type.MAP).build());

    HtmlResponse html = getHtmlResponse(response);
    Truth.assertThat(html).isNotNull();
    Truth.assertThat(html.getUrl()).endsWith("map.html");
    Truth.assertThat(html.getSuppressMic()).isTrue();
    Truth.assertThat(html.getUpdatedState().get("locations")).isNotNull();
    JsonArray json = (JsonArray) html.getUpdatedState().get("locations");
    JsonObject first = json.get(0).getAsJsonObject();
    Truth.assertThat(first.get("title").getAsString()).isEqualTo("Story title");

    SimpleResponse simple = getSimpleResponse(response);
    Truth.assertThat(simple.getDisplayText()).isEqualTo("text");
  }

  protected HtmlResponse getHtmlResponse(ActionResponse response) {
    for (RichResponseItem item : response.getRichResponse().getItems()) {
      if (item.getHtmlResponse() != null) {
        return item.getHtmlResponse();
      }
    }
    return null;
  }

  @Test
  public void media_onImage() throws MalformedURLException {
    Story story = new Story("caseDataId", "storyid", "Story title", Story.LOCATION, "Story text",
        new URL("http://example.com/story.ogg"), null);

    ActionResponse response = generate(SherlockResponse.builder(story).prestoryText("my prestory")
        .afterstoryText("my afterstory").build());
    SimpleResponse simple = getSimpleResponse(response);
    MediaResponse media = getMediaContent(response);

    Truth.assertThat(simple.getDisplayText()).isEqualTo("my prestory");
    Truth.assertThat(media.getMediaObjects()).hasSize(1);
    MediaObject mo = media.getMediaObjects().get(0);
    Truth.assertThat(mo.getContentUrl()).isEqualTo("http://example.com/story.ogg");
    Truth.assertThat(mo.getName()).isEqualTo("Story title");
    Truth.assertThat(mo.getLargeImage().getUrl()).endsWith("logo.jpg");
    Mockito.verify(sessionManager, Mockito.atLeastOnce())
        .setFollowupText(Matchers.eq("my afterstory"));
  }

  @Test
  public void media() throws MalformedURLException {
    Story story = new Story("case-data-id", "id", "title", "type", "text",
        new URL("http://example.org/media.mp3"), new URL("http://example.org/image.jpg"),
        ImmutableList.of(), "latlong", new Date());
    ActionResponse response = generate(
        SherlockResponse.builder("hello there").prestoryText("hello").story(story).build());
    SimpleResponse simple = getSimpleResponse(response);
    Truth.assertThat(simple.getDisplayText()).isEqualTo("hello");

    MediaResponse media = getMediaContent(response);
    Truth.assertThat(media).isNotNull();
    Truth.assertThat(media.getMediaObjects()).hasSize(1);
    Truth.assertThat(media.getMediaObjects().get(0).getName()).isEqualTo("title");
    Truth.assertThat(media.getMediaObjects().get(0).getContentUrl())
        .isEqualTo("http://example.org/media.mp3");
    Truth.assertThat(media.getMediaObjects().get(0).getLargeImage().getUrl())
        .isEqualTo("http://example.org/image.jpg");
  }

  @Test
  public void linkOut() throws MalformedURLException {
    ActionResponse response = generate(SherlockResponse.builder("hello there")
        .linkOut("Link Title", new URL("http://example.com")).build());
    SimpleResponse simple = getSimpleResponse(response);
    LinkOutSuggestion linkOut = getLinkOutResponse(response);
    Truth.assertThat(simple.getDisplayText()).isEqualTo("hello there");
    Truth.assertThat(linkOut.getOpenUrlAction().getUrl()).isEqualTo("http://example.com");
    Truth.assertThat(linkOut.getDestinationName()).isEqualTo("Link Title");
  }

  @Test
  public void contexts() {
    ActionResponse response =
        generate(SherlockResponse.builder("hello there").addOutContext("context1")
            .addOutContext("context2", 5).addParameter("pname", "pvalue").build());

    JsonObject json = new JsonParser().parse(response.toJson()).getAsJsonObject();

    Truth.assertThat(response.getWebhookResponse().getOutputContexts().size()).isGreaterThan(2);
    int count = 0;
    for (Context context : response.getWebhookResponse().getOutputContexts()) {
      if (context.getName().endsWith("context1")) {
        count++;
      } else if (context.getName().endsWith("context2")) {
        Truth.assertThat(context.getLifespanCount()).isEqualTo(5);
        Truth.assertThat(context.getParameters().get("pname").toString()).isEqualTo("pvalue");
        count++;
      }
    }
    Truth.assertThat(count).isEqualTo(2);
  }

  @Test
  public void carousel() throws Exception {
    SherlockRequest request = new SherlockRequestBuilder("hello", "mysessionid", "action").build();
    SherlockResponse response = new SherlockResponseBuilder("speech")
        .addCard("title", "description", new URL("http://example.org"), "imageAlt", "key",
            ImmutableList.of())
        .addCard("title2", "description2", new URL("http://example.org"), "imageAlt2", "key2",
            ImmutableList.of())
        .build();
    ActionResponse webhookResponse = generate(request, response);
    Truth.assertThat(webhookResponse.getHelperIntent().getIntent())
        .isEqualTo("actions.intent.OPTION");
  }

  @Test
  public void oneCarousel() throws Exception {
    SherlockRequest request = new SherlockRequestBuilder("hello", "mysessionid", "action").build();
    SherlockResponse response =
        new SherlockResponseBuilder("speech").addCard("title", "description",
            new URL("http://example.org"), "imageAlt", "key", ImmutableList.of()).build();
    ActionResponse webhookResponse = generate(request, response);
    Truth.assertThat(webhookResponse.getHelperIntent()).isNull();
  }

  @Test
  public void simpleResponse() {
    ActionResponse response = generate(SherlockResponse.builder("hello there").build());
    SimpleResponse simple = getSimpleResponse(response);
    Truth.assertThat(simple.getDisplayText()).isEqualTo("hello there");
    Truth.assertThat(simple.getSsml()).isEqualTo("<speak>hello there</speak>");
    Truth.assertThat(getExpectedUserResponse(response)).isTrue();
  }

  @Test
  public void expectUserResponse_falseOnCanvas() {
    TestUtil.mockCaseStartedSession(sessionManager);
    ActionResponse response = generate(
        new SherlockRequestBuilder("exit", "sessionid", "action").addFlag(Flag.CANVAS).build(),
        SherlockResponse.builder("exiting").endConversation(true).build());
    Truth.assertThat(getExpectedUserResponse(response)).isFalse();
  }

  @Test
  public void endConversation() throws MalformedURLException {
    ActionResponse response =
        generate(SherlockResponse.builder("hello there").endConversation(true).build());
    Truth.assertThat(getExpectedUserResponse(response)).isFalse();
  }

  @Test
  public void noAudio() {
    ActionResponse whResponse = generate(SherlockResponse.builder("hello there")
        .prestoryText("my prestory").afterstoryText("my afterstory").build());

    SimpleResponse response = getSimpleResponse(whResponse);
    Truth.assertThat(response.getDisplayText()).contains("hello there");
    Truth.assertThat(response.getDisplayText()).contains("my prestory");
    Truth.assertThat(response.getDisplayText()).contains("my afterstory");
    Truth.assertThat(response.getSsml()).contains("hello there");
    Truth.assertThat(response.getSsml()).contains("my prestory");
    Truth.assertThat(response.getSsml()).contains("my afterstory");
  }

  @Test
  public void noAudio_long() {
    StringBuilder text = new StringBuilder();
    text.append("hello there");
    for (int i = 0; i < 65; i++) {
      text.append("1234567890");
    }
    ActionResponse webhookResponse = generate(SherlockResponse.builder(text.toString())
        .prestoryText("my prestory").afterstoryText("my afterstory").build());
    SimpleResponse response = getSimpleResponse(webhookResponse);
    Truth.assertThat(response.getDisplayText()).contains("hello there");
    Truth.assertThat(response.getDisplayText()).contains("my prestory");
    Truth.assertThat(response.getSsml()).contains("hello there");
    Truth.assertThat(response.getSsml()).contains("my prestory");

    SimpleResponse secondResponse = getSimpleResponse(webhookResponse, 1);
    Truth.assertThat(secondResponse.getDisplayText()).contains("my afterstory");
    Truth.assertThat(secondResponse.getSsml()).contains("my afterstory");
  }

  @Test
  public void noAudio_card() throws Exception {
    ActionResponse webhookResponse = generate(SherlockResponse.builder("hello there")
        .prestoryText("my prestory").afterstoryText("my afterstory")
        .imageUrlIfNotPresent(new URL("http://example.org/image"), "alt")
        .linkOut("Title", new URL("http://example.org/")).build());
    SimpleResponse response = getSimpleResponse(webhookResponse);
    Truth.assertThat(response.getDisplayText()).contains("hello there");
    Truth.assertThat(response.getDisplayText()).contains("my prestory");

    BasicCard card = getBasicCard(webhookResponse);
    Truth.assertThat(card.getImage().getUrl()).isEqualTo("http://example.org/image");
    Truth.assertThat(card.getImage().getAccessibilityText()).isEqualTo("alt");
    Truth.assertThat(card.getButtons().get(0).getOpenUrlAction().getUrl())
        .isEqualTo("http://example.org/");

    SimpleResponse lastResponse = getLastSimpleResponse(webhookResponse);
    Truth.assertThat(lastResponse.getDisplayText()).isEqualTo("my afterstory");
  }

  @Test
  public void useStorage() throws MalformedURLException {
    ActionResponse response =
        generate(new SherlockRequestBuilder("hello", "mysessionid", "action").build(),
            SherlockResponse.builder("hello there").endConversation(true).build());
    Truth.assertThat(response.toJson().toString()).containsMatch("sessionid");
    Truth.assertThat(response.toJson().toString()).containsMatch("mysessionid");
  }

  @Test
  public void suggestions_limit() throws MalformedURLException {
    List<String> generatedSuggestions = new ArrayList<String>();
    for (int i = 0; i < 30; i++) {
      generatedSuggestions.add("suggestion " + i);
    }
    ActionResponse response =
        generate(SherlockResponse.builder("hello there").suggestions(generatedSuggestions).build());
    List<Suggestion> list = response.getRichResponse().getSuggestions();
    List<String> suggestions = list.stream().map(s -> s.getTitle()).collect(Collectors.toList());

    generatedSuggestions = generatedSuggestions.subList(0, 7);
    Truth.assertThat(suggestions).containsExactlyElementsIn(generatedSuggestions);
  }

  @Test
  public void suggestions() throws MalformedURLException {
    ActionResponse response = generate(SherlockResponse.builder("hello there")
        .suggestions("suggestion 1", "suggestion 2").build());
    List<Suggestion> list = response.getRichResponse().getSuggestions();
    List<String> suggestions = list.stream().map(s -> s.getTitle()).collect(Collectors.toList());

    Truth.assertThat(suggestions).containsExactly("suggestion 1", "suggestion 2");
  }

  @Test
  public void otherSurfaces() throws Exception {
    SherlockResponse sresponse = new SherlockResponseBuilder().storyText("hello")
        .otherSurface("my title", "my \"message\"").build();

    ActionResponse response = generate(sresponse);

    Truth.assertThat(response.getHelperIntent().getIntent())
        .isEqualTo("actions.intent.NEW_SURFACE");
  }


  @Test
  public void splitIntoTwo() {
    Truth.assertThat(DFV2ResponseGenerator.splitTextIntoTwo("1234\n\n12345", 5))
        .isEqualTo(new String[] {"1234", "12345"});
    Truth.assertThat(DFV2ResponseGenerator.splitTextIntoTwo("12\n\n12345", 5))
        .isEqualTo(new String[] {"12", "12345"});
    Truth.assertThat(DFV2ResponseGenerator.splitTextIntoTwo("1\n\n2\n\n12345", 6))
        .isEqualTo(new String[] {"1\n\n2", "12345"});
    Truth.assertThat(DFV2ResponseGenerator.splitTextIntoTwo("1\n\n2\n\n34\n\n567", 8))
        .isEqualTo(new String[] {"1\n\n2\n\n34", "567"});
    Truth.assertThat(DFV2ResponseGenerator.splitTextIntoTwo("12345\n\n123", 4))
        .isEqualTo(new String[] {"12345", "123"});
    Truth.assertThat(DFV2ResponseGenerator.splitTextIntoTwo("12345\n\n67891", 4))
        .isEqualTo(new String[] {"12345", "67891"});
  }

  private ActionResponse generate(SherlockRequest request, SherlockResponse response) {
    ResponseBuilder responseBuilder =
        new ResponseBuilder(true, "sessionid", new HashMap<>(), new HashMap<>());
    generator.generate(request, response, DIALOGFLOW_SESSION, responseBuilder);
    return responseBuilder.build();
  }

  private ActionResponse generate(SherlockResponse response) {
    return generate(new SherlockRequestBuilder("hello", "sessionid", "action").build(), response);
  }


  private LinkOutSuggestion getLinkOutResponse(ActionResponse response) {
    return response.getRichResponse().getLinkOutSuggestion();
  }



  private SimpleResponse getSimpleResponse(ActionResponse response, int index) {
    int current = 0;
    for (RichResponseItem item : response.getRichResponse().getItems()) {
      if (item.getSimpleResponse() != null) {
        if (index == current) {
          return item.getSimpleResponse();
        }
        current++;
      }
    }
    return null;
  }

  private SimpleResponse getSimpleResponse(ActionResponse response) {
    return getSimpleResponse(response, 0);
  }

  private boolean getExpectedUserResponse(ActionResponse response) {
    return response.getExpectUserResponse();
  }

  private MediaResponse getMediaContent(ActionResponse response) {
    for (RichResponseItem message : response.getRichResponse().getItems()) {
      if (message.getMediaResponse() != null) {
        return message.getMediaResponse();
      }
    }
    return null;
  }

  private BasicCard getBasicCard(ActionResponse webhookResponse) {
    for (RichResponseItem message : webhookResponse.getRichResponse().getItems()) {
      if (message.getBasicCard() != null) {
        return message.getBasicCard();
      }
    }
    return null;
  }

  private SimpleResponse getLastSimpleResponse(ActionResponse webhookResponse) {
    SimpleResponse response = null;
    for (RichResponseItem item : webhookResponse.getRichResponse().getItems()) {
      if (item.getSimpleResponse() != null) {
        response = item.getSimpleResponse();
      }
    }
    return response;
  }

}
