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

package com.google.mystery.actions.model;

import java.net.URL;
import java.util.List;
import java.util.Map;
import com.google.common.collect.ImmutableList;
import com.google.mystery.data.model.Clue;
import com.google.mystery.data.model.Hint;
import com.google.mystery.data.model.Story;

/**
 * Game action response.
 *
 * @author ilyaplatonov
 */
public class SherlockResponse {
  private final String title;
  /** output text. */
  private final String storyText;
  /** text that goes before main story happens. */
  private final String prestoryText;
  /** text that goes after main story happens. */
  private final String afterstoryText;
  /** link to story audio file. */
  private final URL storyAudioURL;
  /** context spanned by this response. */
  private final List<SherlockContext> outContexts;
  /** string parameters for out context. */
  private final Map<String, String> contextParameters;
  /** if google home should finish session. */
  private final boolean endConversation;
  /** hint provided with this response. */
  private final Hint hint;
  /** external link showed to user. */
  private final URL linkOut;
  /** title show on response link. */
  private final String linkOutTitle;
  /** image url. */
  private final URL image;
  /** image alt text. */
  private final String imageAlt;
  /** clues provided with this response. */
  private final List<Clue> clues;
  /** response suggestions. */
  private final List<String> suggestions;
  /** Message used to send message to other device. */
  private final String otherSurfaceMessage;
  /** Title used to send message to other device. */
  private final String otherSurfaceTitle;
  /** cards to show. */
  private final List<SherlockResponseCard> cards;
  /** output screen data. */
  private final SherlockScreenData screenData;

  SherlockResponse(String storyTitle, String storyText, String prestoryText, String afterstoryText,
      URL storyAudioURL, List<SherlockContext> outContexts, Map<String, String> contextParameters,
      boolean endConversation, Hint hint, List<Clue> clues, URL linkOut, String linkOutTitle,
      URL image, String imageAlt, List<String> suggestions, String otherSurfaceTitle,
      String otherSurfaceMessage, List<SherlockResponseCard> cards, SherlockScreenData screenData) {
    this.title = storyTitle;
    this.storyText = storyText;
    this.prestoryText = prestoryText;
    this.afterstoryText = afterstoryText;
    this.storyAudioURL = storyAudioURL;
    this.outContexts = outContexts;
    this.contextParameters = contextParameters;
    this.endConversation = endConversation;
    this.hint = hint;
    this.clues = clues;
    this.linkOut = linkOut;
    this.linkOutTitle = linkOutTitle;
    this.image = image;
    this.imageAlt = imageAlt;
    this.suggestions = suggestions == null ? ImmutableList.of() : ImmutableList.copyOf(suggestions);
    this.otherSurfaceTitle = otherSurfaceTitle;
    this.otherSurfaceMessage = otherSurfaceMessage;
    this.cards = cards == null ? ImmutableList.of() : ImmutableList.copyOf(cards);
    this.screenData = screenData;
  }

  public String getLinkOutTitle() {
    return linkOutTitle;
  }

  public String getTitle() {
    return title;
  }

  public String getStoryText() {
    return storyText;
  }

  public String getPrestoryText() {
    return prestoryText;
  }

  public String getAfterstoryText() {
    return afterstoryText;
  }

  public URL getStoryAudioURL() {
    return storyAudioURL;
  }

  public URL getImage() {
    return image;
  }

  public static SherlockResponseBuilder builder(String text) {
    return new SherlockResponseBuilder(text);
  }

  public String getCombinedText() {
    String text = SherlockResponse.normalize(storyText);
    String pretext = SherlockResponse.normalize(prestoryText);
    String aftertext = SherlockResponse.normalize(afterstoryText);
    return (pretext + "\n" + text + "\n" + aftertext).trim();
  }

  /** Combines pretext and text. */
  public String getPreAndText() {
    String pretext = SherlockResponse.normalize(prestoryText);
    String text = SherlockResponse.normalize(storyText);
    return (pretext + "\n" + text).trim();
  }

  public static String normalize(String text) {
    return text.replaceAll("“", "\"").replaceAll("”", "\"");
  }

  public static String textToText(String text) {
    text = text.replace("]", "");
    text = text.replace("[", "");
    text = text.replaceAll("\\.\\.\\.", "");
    return text;
  }

  public static String textToSsml(String text) {
    text = text.replace("]", "");
    text = text.replace("[", "");
    text = text.replaceAll("\\.\\.\\.", "<break time=\"150ms\"/>");
    // pause after character direct speech "\n
    text = text.replaceAll("\"\n", "\"<break time=\"220ms\"/>\n");
    text = text.replace("\n", " ");

    text = text.replaceAll("&", "&amp;");

    // AoG TTS tend to say "dot" if it got >. in SSML.
    text = text.replaceAll(">\\.", ">");

    return text;
  }

  public List<SherlockContext> getOutContexts() {
    return outContexts;
  }

  public Map<String, String> getContextParameters() {
    return contextParameters;
  }

  public boolean getEndConversation() {
    return endConversation;
  }

  public Hint getHint() {
    return hint;
  }

  public List<Clue> getClues() {
    return clues;
  }

  public URL getLinkOut() {
    return linkOut;
  }

  public static SherlockResponseBuilder builder(Story story) {
    return builder(story.getText()).story(story);
  }

  public static String textToHtml(String text) {
    text = text.replaceAll("\n", "<br>");

    text = text.replace("[", "<b>");
    text = text.replace("]", "</b>");
    text = text.replaceAll("\\.\\.\\.", "");
    text = text.replaceAll("&", "&amp;");

    return text;
  }

  public String getImageAlt() {
    return imageAlt;
  }

  public List<String> getSuggestions() {
    return suggestions;
  }

  public String getOtherSurfaceMessage() {
    return otherSurfaceMessage;
  }

  public String getOtherSurfaceTitle() {
    return otherSurfaceTitle;
  }

  public List<SherlockResponseCard> getCards() {
    return cards;
  }

  public SherlockScreenData getScreenData() {
    return screenData;
  }
}
