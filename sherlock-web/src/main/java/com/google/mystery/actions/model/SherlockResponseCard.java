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

public class SherlockResponseCard {
  private final String title;
  private final String description;
  private final URL imageUrl;
  private final String imageAlt;
  private final String key;
  private final List<String> synonyms;

  public SherlockResponseCard(String title, String description, URL imageUrl, String imageAlt,
      String key, List<String> synonyms) {
    this.title = title;
    this.description = description;
    this.imageUrl = imageUrl;
    this.imageAlt = imageAlt;
    this.key = key;
    this.synonyms = synonyms;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public URL getImageUrl() {
    return imageUrl;
  }

  public String getImageAlt() {
    return imageAlt;
  }

  public String getKey() {
    return key;
  }

  public List<String> getSynonyms() {
    return synonyms;
  }
}
