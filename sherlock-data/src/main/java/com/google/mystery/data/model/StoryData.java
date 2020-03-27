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

package com.google.mystery.data.model;

/**
 * External story implementation used for import data.
 * 
 * @author ilyaplatonov
 */
public class StoryData {
  final private String id;
  final private String title;
  final private String clues;
  final private String latlong;
  final private String text;

  public StoryData(String id, String title, String clues, String latlong, String text) {
    this.id = id;
    this.title = title;
    this.clues = clues;
    this.latlong = latlong;
    this.text = text;
  }

  public String getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public String getClues() {
    return clues;
  }

  public String getText() {
    return text;
  }

  public String getLatlong() {
    return latlong;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((clues == null) ? 0 : clues.hashCode());
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    result = prime * result + ((latlong == null) ? 0 : latlong.hashCode());
    result = prime * result + ((text == null) ? 0 : text.hashCode());
    result = prime * result + ((title == null) ? 0 : title.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    StoryData other = (StoryData) obj;
    if (clues == null) {
      if (other.clues != null)
        return false;
    } else if (!clues.equals(other.clues))
      return false;
    if (id == null) {
      if (other.id != null)
        return false;
    } else if (!id.equals(other.id))
      return false;
    if (latlong == null) {
      if (other.latlong != null)
        return false;
    } else if (!latlong.equals(other.latlong))
      return false;
    if (text == null) {
      if (other.text != null)
        return false;
    } else if (!text.equals(other.text))
      return false;
    if (title == null) {
      if (other.title != null)
        return false;
    } else if (!title.equals(other.title))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "StoryData [id=" + id + ", title=" + title + ", clues=" + clues + ", latlong=" + latlong
        + ", text=" + text + "]";
  }
}
