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

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Text;
import com.google.common.collect.ImmutableList;
import com.google.mystery.data.DataManager;
import com.google.mystery.data.DataUtil;

/**
 * Story represents single text event in the game.
 *
 * @author ilyaplatonov
 */
public class Story implements Serializable {
  private static final long serialVersionUID = -126266652161890309L;
  /** id based story assets. */
  public static final String SIMPLE = "simple";
  /** location based story which moves investigation forward. */
  public static final String LOCATION = "location";
  /** game The Times article. */
  public static final String TIMES_ARTICLE = "timesArticle";
  /** id of case introduction. */
  public static final String CASE_INTRODUCTION_ID = "caseIntroduction";
  public static final String FINAL_SOLUTION_ID = "finalSolution";

  private final String caseDataId;
  private final String id;
  private final String title;
  /** location,timesArticle or simple. */
  private final String type;
  /** story text. */
  private final String text;
  /** audio link. */
  private final URL audioUrl;
  /** story image URL. */
  private final URL imageUrl;
  /** clues that story gives you at this location. */
  private final List<String> clues;
  /** comma separated latlong used to make pins on map. */
  private final String latlong;

  /** when this entry was last updated. */
  private final Date updateTime;

  /** Creates location based story */
  public static Story location(String caseDataId, String id, String title, String text, URL link,
      List<String> clues, String latlong, URL imageUrl) {
    return new Story(caseDataId, id, title, LOCATION, text, link, imageUrl, clues, latlong,
        new Date());
  }

  /** Creates simple story */
  public static Story simple(String caseDataId, String id, String title, String text, URL link,
      List<String> clues, String latlong, URL imageUrl) {
    return new Story(caseDataId, id, title, SIMPLE, text, link, imageUrl, clues, latlong,
        new Date());
  }

  /** Creates simple story */
  public static Story timesArticle(String caseDataId, String id, String title, String text,
      URL link, URL imageUrl) {
    return new Story(caseDataId, id, title, TIMES_ARTICLE, text, link, imageUrl, ImmutableList.of(),
        null, new Date());
  }

  public Story(String caseDataId, String id, String title, String type, String text, URL link,
      URL imageUrl, List<String> clues, String latlong, Date updateTime) {
    this.caseDataId = caseDataId;
    this.id = id;
    this.title = title;
    this.type = type;
    this.text = text;
    this.audioUrl = link;
    this.imageUrl = imageUrl;
    this.clues = clues;
    this.latlong = latlong;
    this.updateTime = updateTime;
  }

  public Story(String caseDataId, String id, String title, String type, String text, URL link,
      URL imageUrl) {
    this(caseDataId, id, title, type, text, link, imageUrl, ImmutableList.of(), null, new Date());
  }

  public String getCaseDataId() {
    return caseDataId;
  }

  public String getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public String getType() {
    return type;
  }

  public String getText() {
    return text;
  }

  public boolean isLocation() {
    return LOCATION.equals(type);
  }

  public URL getAudioUrl() {
    return audioUrl;
  }

  public URL getImageUrl() {
    return imageUrl;
  }

  public String getLatlong() {
    return latlong;
  }

  public Date getUpdateTime() {
    return updateTime;
  }

  public static Story fromEntity(Entity entity) {
    URL audioLink = null;
    URL imageUrl = null;

    try {
      if (entity.getProperty("audioUrl") != null) {
        audioLink = new URL(entity.getProperty("audioUrl").toString());
      }
      if (entity.getProperty("imageUrl") != null) {
        imageUrl = new URL(entity.getProperty("imageUrl").toString());
      }
    } catch (MalformedURLException e) {
      Logger.getLogger(Story.class.getName()).log(Level.SEVERE, "Unparsable story url from db", e);
    }

    String latlong = null;
    if (entity.getProperty("latlong") != null) {
      latlong = entity.getProperty("latlong").toString();
    }

    return new Story(entity.getProperty(DataManager.CASE_DATA_ID).toString(),
        entity.getProperty("id").toString(), entity.getProperty("title").toString(),
        entity.getProperty("type").toString(), ((Text) entity.getProperty("text")).getValue(),
        audioLink, imageUrl, DataUtil.getStringList(entity, "clue"), latlong,
        (Date) entity.getProperty("updateTime"));
  }

  public static Entity toEntity(Story story) {
    Entity storyEntity = new Entity(DataManager.STORY_KIND);
    storyEntity.setIndexedProperty(DataManager.CASE_DATA_ID, story.getCaseDataId());
    storyEntity.setIndexedProperty("id", story.getId());
    storyEntity.setUnindexedProperty("title", story.getTitle());
    storyEntity.setIndexedProperty("type", story.getType());
    storyEntity.setUnindexedProperty("text", new Text(story.getText()));
    if (story.getAudioUrl() != null) {
      storyEntity.setUnindexedProperty("audioUrl", story.getAudioUrl().toString());
    }
    if (story.getImageUrl() != null) {
      storyEntity.setUnindexedProperty("imageUrl", story.getImageUrl().toString());
    }
    DataUtil.setStringListProperty(storyEntity, "clue", story.getClues());
    if (story.getLatlong() != null) {
      storyEntity.setUnindexedProperty("latlong", story.getLatlong());
    }
    storyEntity.setUnindexedProperty("updateTime", new Date());
    return storyEntity;
  }

  public List<String> getClues() {
    return clues;
  }

  @Override
  public String toString() {
    return "Story [id=" + id + ", title=" + title + ", type=" + type + ", text=" + text
        + ", audioUrl=" + audioUrl + ", imageUrl=" + imageUrl + ", clues=" + clues + ", latlong="
        + latlong + ", updateTime=" + updateTime + "]";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((audioUrl == null) ? 0 : audioUrl.hashCode());
    result = prime * result + ((caseDataId == null) ? 0 : caseDataId.hashCode());
    result = prime * result + ((clues == null) ? 0 : clues.hashCode());
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    result = prime * result + ((imageUrl == null) ? 0 : imageUrl.hashCode());
    result = prime * result + ((latlong == null) ? 0 : latlong.hashCode());
    result = prime * result + ((text == null) ? 0 : text.hashCode());
    result = prime * result + ((title == null) ? 0 : title.hashCode());
    result = prime * result + ((type == null) ? 0 : type.hashCode());
    result = prime * result + ((updateTime == null) ? 0 : updateTime.hashCode());
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
    Story other = (Story) obj;
    if (audioUrl == null) {
      if (other.audioUrl != null)
        return false;
    } else if (!audioUrl.equals(other.audioUrl))
      return false;
    if (caseDataId == null) {
      if (other.caseDataId != null)
        return false;
    } else if (!caseDataId.equals(other.caseDataId))
      return false;
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
    if (imageUrl == null) {
      if (other.imageUrl != null)
        return false;
    } else if (!imageUrl.equals(other.imageUrl))
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
    if (type == null) {
      if (other.type != null)
        return false;
    } else if (!type.equals(other.type))
      return false;
    if (updateTime == null) {
      if (other.updateTime != null)
        return false;
    } else if (!updateTime.equals(other.updateTime))
      return false;
    return true;
  }
}
