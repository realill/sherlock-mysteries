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

/** */
package com.google.mystery.actions.model;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.api.client.util.Lists;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.mystery.actions.model.SherlockScreenData.Type;
import com.google.mystery.data.model.Clue;
import com.google.mystery.data.model.Hint;
import com.google.mystery.data.model.Story;

/** @author Ilya Platonov */
public class SherlockResponseBuilder {
  /** used as story audio peace title shown in UI. */
  private String title = null;
  /** output text. */
  private String storyText = null;
  /** link to story audio file. */
  private URL storyAudioURL;

  public URL getStoryAudioURL() {
    return storyAudioURL;
  }

  /** image that will be show with response (on surfaces that support this). */
  private URL imageURL = null;
  /** alternative text to the image. */
  private String imageAlt = null;
  /** link that goes with response (on surfaces that support this) */
  private URL linkOutURL;

  /** title of response link. */
  private String linkOutTitle;
  /** text that goes before main story happens. */
  private String prestoryText = "";
  /** text that goes after main story happens. */
  private String afterstoryText = "";
  /** context spanned by this response. */
  private List<SherlockContext> outContexts = new ArrayList<>();
  /** string parameters for out context. */
  private Map<String, String> contextParameters = new HashMap<>();
  /** tells google home to finish conversation. */
  private boolean endConversation = false;

  private Hint hint = null;
  private List<Clue> clues = Lists.newArrayList();
  /** response suggestions. */
  private List<String> suggestions = Lists.newArrayList();
  /** Message used to send message to other device. */
  private String otherSurfaceMessage = null;
  /** Title used to send message to other device. */
  private String otherSurfaceTtitle = null;
  /** Cards to show. */
  private List<SherlockResponseCard> cards = new ArrayList<>();
  /** Screen data. */
  private SherlockScreenData screenData = null;

  public SherlockResponseBuilder() {}

  public SherlockResponseBuilder(String text) {
    this.storyText = text;
  }

  public SherlockResponseBuilder storyText(String text) {
    this.storyText = text;
    return this;
  }

  public SherlockResponseBuilder cardsTitle(String title) {
    this.title = title;
    return this;
  }

  private SherlockResponseBuilder storyAudio(String title, URL url) {
    this.storyAudioURL = url;
    this.title = title;
    return this;
  }

  public URL getLinkOutURL() {
    return linkOutURL;
  }

  public boolean isEndConversation() {
    return endConversation;
  }

  public List<String> getSuggestions() {
    return suggestions;
  }

  public SherlockResponseBuilder confirmSuggestions() {
    return suggestions("Yes");
  }

  public SherlockResponseBuilder confirmRejectSuggestions() {
    return suggestions("Yes", "No");
  }

  public SherlockResponseBuilder continueSuggestions() {
    return suggestions("Go on");
  }

  public SherlockResponseBuilder suggestions(String... suggestions) {
    this.suggestions = Lists.newArrayList();
    for (String s : suggestions) {
      this.suggestions.add(s);
    }
    return this;
  }

  public SherlockResponseBuilder suggestions(List<String> suggestions) {
    this.suggestions = Lists.newArrayList(suggestions);
    return this;
  }

  public SherlockResponseBuilder linkOut(String title, URL linkOutURL) {
    this.linkOutTitle = title;
    this.linkOutURL = linkOutURL;
    return this;
  }

  public SherlockResponseBuilder afterstoryText(String afterstoryText) {
    this.afterstoryText = afterstoryText;
    return this;
  }

  public SherlockResponseBuilder prestoryText(String prestoryText) {
    this.prestoryText = prestoryText;
    return this;
  }

  public SherlockResponseBuilder addOutContext(String outContext) {
    SherlockContext context = new SherlockContext(outContext);
    this.outContexts.add(context);
    return this;
  }

  public SherlockResponseBuilder addParameter(String name, String value) {
    contextParameters.put(name, value);
    return this;
  }

  public SherlockResponseBuilder endConversation(boolean endConversation) {
    this.endConversation = endConversation;
    return this;
  }

  public SherlockResponseBuilder hint(Hint hint) {
    this.hint = hint;
    return this;
  }

  public SherlockResponseBuilder addClue(Clue clue) {
    clues.add(clue);
    return this;
  }

  /** Sets image url for this response if one is not already exist in current response */
  public SherlockResponseBuilder imageUrlIfNotPresent(URL imageURL, String alt) {
    if (this.imageURL == null) {
      this.imageURL = imageURL;
      this.imageAlt = alt;
    }
    return this;
  }

  /**
   * Setting new screen type for screen data.
   */
  public SherlockResponseBuilder screenDataType(SherlockScreenData.Type type) {
    screenData = new SherlockScreenData(type, true);
    return this;
  }

  /**
   * Setting new screen type and if text should be shown.
   */
  public SherlockResponseBuilder screenDataType(SherlockScreenData.Type type, boolean showText) {
    screenData = new SherlockScreenData(type, showText);
    return this;
  }


  public SherlockResponse build() {
    if (storyText == null) {
      throw new IllegalArgumentException("Response builder without text is forbidden");
    }
    String text = SherlockResponse.normalize(storyText);
    String pretext = SherlockResponse.normalize(prestoryText);
    String aftertext = SherlockResponse.normalize(afterstoryText);
    return new SherlockResponse(title, text, pretext, aftertext, storyAudioURL, outContexts,
        ImmutableMap.copyOf(contextParameters), endConversation, hint, ImmutableList.copyOf(clues),
        linkOutURL, linkOutTitle, imageURL, imageAlt, suggestions, otherSurfaceTtitle,
        otherSurfaceMessage, cards, screenData);
  }

  /** Creates html version of this response */
  public String buildHtml() {
    String text = SherlockResponse.normalize(storyText);
    return SherlockResponse.textToHtml(text);
  }

  public SherlockResponseBuilder story(Story story) {
    storyText(story.getText());
    storyAudio(story.getTitle(), story.getAudioUrl());
    if (story.getImageUrl() != null) {
      imageUrlIfNotPresent(story.getImageUrl(), story.getTitle());
    }
    screenDataType(Type.STORY);
    return this;
  }

  public SherlockResponseBuilder addOutContext(String name, int lifespan) {
    SherlockContext context = new SherlockContext(name, lifespan);
    this.outContexts.add(context);
    return this;
  }

  public Hint getHint() {
    return hint;
  }

  public List<Clue> getClues() {
    return clues;
  }

  public SherlockResponseBuilder otherSurface(String title, String message) {
    otherSurfaceTtitle = title;
    otherSurfaceMessage = message;
    return this;
  }

  public SherlockResponseBuilder addSuggestion(String suggestion) {
    suggestions.add(suggestion);
    return this;
  }

  public SherlockResponseBuilder addCard(String title, String description, URL imageUrl,
      String imageAlt, String key, List<String> synonyms) {
    cards.add(new SherlockResponseCard(title, description, imageUrl, imageAlt, key, synonyms));
    return this;
  }
}
