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

/**
 * 
 */
package com.google.mystery.web;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import org.apache.commons.text.WordUtils;
import com.google.actions.api.ActionContext;
import com.google.actions.api.response.ResponseBuilder;
import com.google.actions.api.response.helperintent.NewSurface;
import com.google.actions.api.response.helperintent.SelectionCarousel;
import com.google.api.services.actions_fulfillment.v2.model.BasicCard;
import com.google.api.services.actions_fulfillment.v2.model.Button;
import com.google.api.services.actions_fulfillment.v2.model.CarouselSelectCarouselItem;
import com.google.api.services.actions_fulfillment.v2.model.HtmlResponse;
import com.google.api.services.actions_fulfillment.v2.model.Image;
import com.google.api.services.actions_fulfillment.v2.model.LinkOutSuggestion;
import com.google.api.services.actions_fulfillment.v2.model.MediaObject;
import com.google.api.services.actions_fulfillment.v2.model.MediaResponse;
import com.google.api.services.actions_fulfillment.v2.model.OpenUrlAction;
import com.google.api.services.actions_fulfillment.v2.model.OptionInfo;
import com.google.api.services.actions_fulfillment.v2.model.SimpleResponse;
import com.google.api.services.actions_fulfillment.v2.model.Suggestion;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.mystery.actions.SessionManager;
import com.google.mystery.actions.messages.MessageException;
import com.google.mystery.actions.messages.MessagesManager;
import com.google.mystery.actions.model.SherlockContext;
import com.google.mystery.actions.model.SherlockRequest;
import com.google.mystery.actions.model.SherlockResponse;
import com.google.mystery.actions.model.SherlockResponseCard;
import com.google.mystery.actions.model.SherlockScreenData.Type;
import com.google.mystery.assets.AssetsManager;
import com.google.mystery.config.SherlockConfig;
import com.google.mystery.data.model.Clue;
import com.google.mystery.data.model.Session;
import com.google.mystery.data.model.Session.State;
import com.google.mystery.data.model.Story;

/**
 * @author ilyaplatonov
 *
 */
public class DFV2ResponseGenerator {
  @Inject
  private SherlockConfig config;
  @Inject
  private SessionManager sessionManager;
  @Inject
  private AssetsManager assetsManager;
  @Inject
  private MessagesManager messagesManager;

  /** logger. */
  private Logger logger = Logger.getLogger(this.getClass().getName());

  public void generate(SherlockRequest request, SherlockResponse response, String dialogflowSession,
      ResponseBuilder webhookResponse) {

    if (!handleCanvas(request, response, webhookResponse)) {
      if (response.getStoryAudioURL() == null) {
        handleNoAudioStory(response, webhookResponse);
      } else {
        handleAudioStory(response, webhookResponse);
      }
      handleSuggestions(request, response, webhookResponse);
    }
    handleContexts(response, dialogflowSession, webhookResponse);
    handleUserStorage(request.getSessionid(), webhookResponse);
    handleOtherSurface(response, webhookResponse);
    handleEndConversation(response, webhookResponse);
  }

  private boolean handleCanvas(SherlockRequest request, SherlockResponse response,
      ResponseBuilder webhookResponse) {
    if (!request.isCanvas()) {
      return false;
    }
    if (response.getStoryAudioURL() != null) {
      addText(webhookResponse, response.getPrestoryText());
      handleStoryCanvas(request, response, webhookResponse);
      return true;
    } else if (response.getScreenData() != null) {
      if (response.getScreenData().getType() == Type.MAP) {
        addText(webhookResponse, response.getCombinedText());
        handleMapCanvas(request, response, webhookResponse);
        return true;
      } else if (response.getScreenData().getType() == Type.NEWSPAPER) {
        addText(webhookResponse, response.getCombinedText());
        handleCanvasNewspaper(request, response, webhookResponse);
        return true;
      } else if (response.getScreenData().getType() == Type.MENU) {
        handleMenu(response, webhookResponse);
        return true;
      } else if (response.getScreenData().getType() == Type.BASE) {
        handleBaseCanvas(response, webhookResponse);
        return true;
      }
    } else if (!request.hasBrowser() || response.getImage() == null
        || response.getLinkOut() == null) {
      handleBaseCanvas(response, webhookResponse);
      return true;
    }
    return false;
  }

  /**
   * Rendering initial welcome menu.
   */
  protected void handleMenu(SherlockResponse response, ResponseBuilder webhookResponse) {
    addText(webhookResponse, response.getCombinedText());
    Map<String, Object> data = new HashMap<>();
    data.put("title", "Sherlock Mysteries");
    data.put("backgroundImage",
        "https://storage.googleapis.com/" + config.getBucketName() + "/images/background.png");
    setSuggestions(data, response);
    webhookResponse.add(new HtmlResponse().setSuppressMic(false)
        .setUrl(config.getUrl("/static/canvas/menu.html").toString()).setUpdatedState(data));
  }

  /**
   * Base canvas support.
   * 
   * @param response
   * @param webhookResponse
   */
  protected void handleBaseCanvas(SherlockResponse response, ResponseBuilder webhookResponse) {
    addText(webhookResponse, response.getCombinedText());
    Map<String, Object> data = new HashMap<>();
    if (response.getScreenData() == null || response.getScreenData().isShowText()) {
      data.put("html", SherlockResponse.textToHtml(response.getCombinedText()));
    }

    if (response.getImage() != null) {
      data.put("image", response.getImage().toString());
    }

    if (response.getCards() != null && !response.getCards().isEmpty()) {
      JsonArray cardsArray = new JsonArray();
      if (response.getTitle() != null) {
        data.put("cardsTitle", response.getTitle());
      }
      data.put("cards", cardsArray);
      for (SherlockResponseCard card : response.getCards()) {
        JsonObject cardElement = new JsonObject();
        cardElement.addProperty("title", card.getTitle());
        if (card.getImageUrl() != null) {
          cardElement.addProperty("image", card.getImageUrl().toString());
        } else {
          cardElement.addProperty("image", config.getPlaceholderUrl().toString());
        }
        cardElement.addProperty("description", card.getDescription());
        cardElement.addProperty("query", card.getKey());
        cardsArray.add(cardElement);
      }
    }

    setSuggestions(data, response);
    setShowQuickMenu(data);

    webhookResponse.add(new HtmlResponse().setSuppressMic(false)
        .setUrl(config.getUrl("/static/canvas/base.html").toString()).setUpdatedState(data));
  }

  protected void handleCanvasNewspaper(SherlockRequest request, SherlockResponse response,
      ResponseBuilder webhookResponse) {
    Map<String, Object> data = new HashMap<>();
    data.put("title", "Newspaper");
    List<Story> articles = assetsManager.getAllAricles(request.getCaseDataId());
    String html = "";
    try {
      html = messagesManager.message("readAllArticlesHTML", ImmutableMap.of("articles", articles));
    } catch (MessageException | IOException e) {
      logger.log(Level.SEVERE, "Error rendering html", e);
    }
    data.put("html", html);
    if (response.getImage() != null) {
      data.put("image", response.getImage().toString());
    }
    setSuggestions(data, response, "Continue");
    setShowQuickMenu(data);
    webhookResponse.add(new HtmlResponse().setSuppressMic(false)
        .setUrl(config.getUrl("/static/canvas/story.html").toString()).setUpdatedState(data));
  }


  protected void handleMapCanvas(SherlockRequest request, SherlockResponse response,
      ResponseBuilder webhookResponse) {
    Session session = sessionManager.getSession();
    List<Story> stories = new ArrayList<>();
    stories.add(assetsManager.getStory(request.getCaseDataId(), Story.CASE_INTRODUCTION_ID));
    for (String location : session.getLocationsBacklog()) {
      stories.add(assetsManager.getStory(request.getCaseDataId(), location));
    }

    JsonArray markers = new JsonArray();
    for (Story story : stories) {
      try {
        if (story.getLatlong() != null) {
          String[] split = story.getLatlong().split(",");
          if (split.length == 2) {
            JsonObject marker = new JsonObject();
            marker.addProperty("title", story.getTitle());
            marker.addProperty("lat", Double.parseDouble(split[0].trim()));
            marker.addProperty("long", Double.parseDouble(split[1].trim()));
            if (Story.LOCATION.equals(story.getType())) {
              marker.addProperty("address", WordUtils.capitalize(story.getId()));
            }
            marker.addProperty("id", story.getId());
            markers.add(marker);
          }
        }
      } catch (NumberFormatException e) {
        logger.log(Level.WARNING, "Error lat/long parsing", e);
      }
    }

    Map<String, Object> data = new HashMap<>();
    data.put("locations", markers);
    setSuggestions(data, response, "Continue");
    setShowQuickMenu(data);
    webhookResponse
        .add(new HtmlResponse().setUrl(config.getUrl("/static/canvas/map.html").toString())
            .setSuppressMic(true).setUpdatedState(data));
  }



  private void setShowQuickMenu(Map<String, Object> data) {
    if (sessionManager.getSession().getState() != State.NEW) {
      data.put("showQuickMenu", Boolean.TRUE);
    }
  }

  private void setSuggestions(Map<String, Object> data, SherlockResponse response,
      String... extra) {
    List<String> suggestions = new ArrayList<>();
    for (String s : extra) {
      suggestions.add(s);
    }
    if (response.getSuggestions() != null) {
      for (String s : response.getSuggestions()) {
        suggestions.add(s);
      }
    }

    data.put("suggestions", suggestions);
  }

  /**
   * Handling canvas API response elements
   */
  private void handleStoryCanvas(SherlockRequest request, SherlockResponse response,
      ResponseBuilder webhookResponse) {
    Map<String, Object> data = new HashMap<>();
    data.put("title", response.getTitle());
    if (response.getStoryAudioURL() != null) {
      data.put("audio", response.getStoryAudioURL().toString());
    }
    data.put("html", SherlockResponse.textToHtml(response.getStoryText()));
    if (response.getStoryAudioURL() != null) {
      data.put("audio", response.getStoryAudioURL().toString());
    }

    if (response.getImage() != null) {
      data.put("image", response.getImage().toString());
    }

    if (response.getHint() != null) {
      data.put("hint", response.getHint().getHint());
    }

    setSuggestions(data, response, "Continue");
    setShowQuickMenu(data);

    if (!response.getClues().isEmpty()) {
      if (response.getTitle() != null) {
        data.put("cardsTitle", "New Clues");
      }
      JsonArray cardsArray = new JsonArray();

      for (Clue clue : response.getClues()) {
        JsonObject cardElement = new JsonObject();
        cardElement.addProperty("title", clue.getName());
        if (clue.getImageUrl() != null) {
          cardElement.addProperty("image", clue.getImageUrl().toString());
        } else {
          cardElement.addProperty("image", config.getPlaceholderUrl().toString());
        }
        cardElement.addProperty("description", clue.getDescription());
        cardElement.addProperty("query", String.format("Show %s", clue.getName()));
        cardsArray.add(cardElement);
      }
      data.put("cards", cardsArray);
    }

    webhookResponse
        .add(new HtmlResponse().setUrl(config.getUrl("/static/canvas/story.html").toString())
            .setSuppressMic(true).setUpdatedState(data));

    if (!Strings.isNullOrEmpty(response.getAfterstoryText())) {
      // setting text to show up on next request
      sessionManager.setFollowupText(response.getAfterstoryText());
    }
  }


  private void handleAudioStory(SherlockResponse response, ResponseBuilder webhookResponse) {

    if (response.getPrestoryText().trim().length() > 0) {
      String text = SherlockResponse.textToText(response.getPrestoryText());
      addText(webhookResponse, text);
    }

    MediaObject mediaObject = new MediaObject();
    mediaObject.setContentUrl(response.getStoryAudioURL().toString()).setName(response.getTitle());

    if (response.getImage() == null) {
      mediaObject.setLargeImage(new Image().setUrl(config.getPlaceholderUrl().toString())
          .setAccessibilityText(Optional.of(response.getTitle()).orElse("media")));
    } else {
      mediaObject.setLargeImage(new Image().setUrl(response.getImage().toString())
          .setAccessibilityText(response.getImageAlt()));
    }

    webhookResponse.add(
        new MediaResponse().setMediaObjects(ImmutableList.of(mediaObject)).setMediaType("AUDIO"));

    if (!Strings.isNullOrEmpty(response.getAfterstoryText())) {
      // setting text to show up on next request
      sessionManager.setFollowupText(response.getAfterstoryText());
    }
  }

  private void handleNoAudioStory(SherlockResponse response, ResponseBuilder webhookResponse) {
    String[] splittedText;
    if (Strings.isNullOrEmpty(response.getAfterstoryText())) {
      splittedText = splitTextIntoTwo(response.getCombinedText(), 640);
    } else {
      if (response.getCombinedText().length() > 640 || response.getImage() != null) {
        splittedText = new String[] {response.getPreAndText(), response.getAfterstoryText()};
      } else {
        splittedText = new String[] {response.getCombinedText()};
      }
    }
    if (splittedText.length > 0) {
      addText(webhookResponse, splittedText[0]);
    }
    if (response.getImage() != null) {
      addImage(response, webhookResponse);
    }

    for (int i = 1; i < splittedText.length; i++) {
      addText(webhookResponse, splittedText[i]);
    }

    if (response.getImage() == null && !response.getCards().isEmpty()) {
      if (response.getCards().size() > 1) {
        List<SherlockResponseCard> responseCards = response.getCards();
        if (responseCards.size() > 10) {
          responseCards = responseCards.subList(0, 10);
          logger.warning("Got more that 10 carusel cards");
        }

        List<CarouselSelectCarouselItem> items = new ArrayList<>();
        for (SherlockResponseCard sherlockCard : response.getCards()) {
          CarouselSelectCarouselItem item = new CarouselSelectCarouselItem();
          item.setTitle(sherlockCard.getTitle());
          item.setDescription(limitLines(sherlockCard.getDescription(), 4));

          URL imageUrl = sherlockCard.getImageUrl();
          if (imageUrl == null) {
            imageUrl = config.getPlaceholderUrl();
          }
          String imageAlt = sherlockCard.getImageAlt();
          if (imageAlt == null) {
            imageAlt = sherlockCard.getTitle();
          }
          item.setImage(new Image().setUrl(imageUrl.toString()).setAccessibilityText(imageAlt));
          item.setOptionInfo(new OptionInfo().setKey(sherlockCard.getKey())
              .setSynonyms(sherlockCard.getSynonyms()));

          items.add(item);

        }
        webhookResponse.add(new SelectionCarousel().setItems(items));
      }
    }
  }

  private void handleContexts(SherlockResponse response, String dialogflowSession,
      ResponseBuilder webhookResponse) {
    for (SherlockContext responseContext : response.getOutContexts()) {
      ActionContext actionContext = new ActionContext(responseContext.getName(),
          responseContext.getLifespan() == null ? 3 : responseContext.getLifespan());
      if (response.getContextParameters() != null) {
        actionContext.setParameters(response.getContextParameters());
      }

      webhookResponse.add(actionContext);
    }
  }

  private void handleSuggestions(SherlockRequest request, SherlockResponse response,
      ResponseBuilder webhookResponse) {
    if (!response.getEndConversation()) {
      List<String> suggestionsList = Lists.newArrayList(response.getSuggestions());
      // if we have link, but do not support browser we suggest to open Investigation Log.
      if (response.getLinkOut() != null && !request.hasBrowser()) {
        suggestionsList.add("Investigation Log");
      }

      if (suggestionsList.size() > 8) {
        suggestionsList = suggestionsList.subList(0, 7);
      }

      for (String s : suggestionsList) {
        if (s.length() > 20) {
          Logger.getLogger(this.getClass().getName())
              .warning("Suggestion is too long to use: " + s);
        } else {
          webhookResponse.add(new Suggestion().setTitle(s));
        }
      }
    }

    if (response.getLinkOut() != null) {
      webhookResponse.add(new LinkOutSuggestion().setDestinationName(response.getLinkOutTitle())
          .setOpenUrlAction(new OpenUrlAction().setUrl(response.getLinkOut().toString())));
    }

  }

  private void addImage(SherlockResponse response, ResponseBuilder webhookResponse) {
    Image image = new Image().setUrl(response.getImage().toString())
        .setAccessibilityText(response.getImageAlt());
    BasicCard basicCard = new BasicCard().setImage(image);


    if (response.getLinkOut() != null) {
      basicCard.setButtons(ImmutableList.of(new Button().setTitle(response.getLinkOutTitle())
          .setOpenUrlAction(new OpenUrlAction().setUrl(response.getLinkOut().toString()))));
    }
    webhookResponse.add(basicCard);
  }

  private void addText(ResponseBuilder webhookResponse, String string) {
    String text = SherlockResponse.textToText(string);
    if (text.length() > 640) {
      text = text.substring(0, 640);
    }
    String ssml = "<speak>" + SherlockResponse.textToSsml(string) + "</speak>";;
    webhookResponse.add(new SimpleResponse().setDisplayText(text).setSsml(ssml));
  }

  private void handleEndConversation(SherlockResponse response, ResponseBuilder webhookResponse) {
    if (response.getEndConversation()) {
      webhookResponse.endConversation();
    }
  }

  /** Building transfer to their device response. */
  private void handleOtherSurface(SherlockResponse response, ResponseBuilder webhookResponse) {
    if (response.getOtherSurfaceTitle() != null && response.getOtherSurfaceMessage() != null) {
      webhookResponse.add(new NewSurface().setNotificationTitle(response.getOtherSurfaceTitle())
          .setContext(response.getOtherSurfaceMessage())
          .setCapability("actions.intent.NEW_SURFACE"));
    }
  }


  /** Setting userStorage object */
  private void handleUserStorage(String sessionid, ResponseBuilder webhookResponse) {
    webhookResponse.getUserStorage().put("sessionid", sessionid);
  }

  public static String[] splitTextIntoTwo(String text, int limit) {
    List<String> splitted = Splitter.on("\n\n").trimResults().splitToList(text.trim());
    int firstNum = 0;
    int size = 0;
    for (; firstNum < splitted.size(); firstNum++) {
      size = size + splitted.get(firstNum).length() + 2;
      if (size - 2 > limit) {
        break;
      }
    }
    if (firstNum == 0) {
      firstNum++;
    }
    if (firstNum == splitted.size()) {
      return new String[] {text.trim()};
    }

    String first = Joiner.on("\n\n").join(splitted.subList(0, firstNum));
    String second = Joiner.on("\n\n").join(splitted.subList(firstNum, splitted.size()));

    return new String[] {first, second};
  }

  /**
   * Limiting number of lines for given string.
   */
  public static String limitLines(String string, int limit) {
    String[] split = string.split("\n");
    if (split.length > limit) {
      return split[0];
    }
    return string;
  }
}
