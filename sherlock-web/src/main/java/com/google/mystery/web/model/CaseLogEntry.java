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

package com.google.mystery.web.model;

import java.net.URL;
import java.util.List;
import com.google.mystery.data.model.Clue;

public class CaseLogEntry {
  private final String id;
  private final String title;
  private final String subtitle;
  private final String html;
  private final URL imageUrl;
  private final String hintMessage;
  private final List<Clue> clues;

  public CaseLogEntry(
      String id,
      String title,
      String subtitle,
      String html,
      URL imageUrl,
      String hintMessage,
      List<Clue> clues) {
    this.id = id;
    this.title = title;
    this.subtitle = subtitle;
    this.html = html;
    this.imageUrl = imageUrl;
    this.hintMessage = hintMessage;
    this.clues = clues;
  }

  public String getTitle() {
    return title;
  }

  public String getSubtitle() {
    return subtitle;
  }

  public URL getImageUrl() {
    return imageUrl;
  }

  public String getHintMessage() {
    return hintMessage;
  }

  public List<Clue> getClues() {
    return clues;
  }

  public String getHtml() {
    return html;
  }

  public String getId() {
    return id;
  }
}
