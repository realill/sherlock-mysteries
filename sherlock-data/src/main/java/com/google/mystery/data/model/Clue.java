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
package com.google.mystery.data.model;

import java.net.MalformedURLException;
import java.net.URL;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Text;
import com.google.mystery.data.DataManager;

/**
 * Clue found in game
 *
 * @author ilyaplatonov
 */
public class Clue {
  private final String caseDataId;
  private final String id;
  private final String name;
  private final String description;
  private final String keywords;

  private final URL imageUrl;

  public Clue(
      String caseDataId,
      String id,
      String name,
      String description,
      String keywords,
      URL imageUrl) {
    this.id = id;
    this.caseDataId = caseDataId;
    this.name = name;
    this.description = description;
    this.keywords = keywords;
    this.imageUrl = imageUrl;
  }

  public String getName() {
    return name;
  }

  public String getId() {
    return id;
  }

  public String getDescription() {
    return description;
  }

  public String getKeywords() {
    return keywords;
  }

  public URL getImageUrl() {
    return imageUrl;
  }

  public String getCaseDataId() {
    return caseDataId;
  }

  public static Clue fromEntity(Entity entity) {
    try {
      URL url = null;
      if (entity.getProperty("imageUrl") != null) {
        url = new URL(entity.getProperty("imageUrl").toString());
      }
      String description;
      if (entity.getProperty("description") instanceof Text) {
        description = ((Text) entity.getProperty("description")).getValue();
      } else {
        // temporary code, remove after transition of current data.
        description = entity.getProperty("description").toString();
      }
      return new Clue(
          entity.getProperty(DataManager.CASE_DATA_ID).toString(),
          entity.getProperty("clueId").toString(),
          entity.getProperty("name").toString(),
          description,
          entity.getProperty("keywords") == null ? null : entity.getProperty("keywords").toString(),
          url);
    } catch (MalformedURLException e) {
      // should never happen
      throw new IllegalArgumentException(
          "Error parsing url " + entity.getProperty("imageUrl").toString(), e);
    }
  }

  public static Entity toEntity(Clue clue) {
    Entity entity = new Entity(DataManager.CLUE_KIND);
    entity.setIndexedProperty(DataManager.CASE_DATA_ID, clue.getCaseDataId());
    entity.setIndexedProperty("clueId", clue.getId());
    entity.setUnindexedProperty("name", clue.getName());
    entity.setUnindexedProperty("description", new Text(clue.getDescription()));
    if (clue.getKeywords() != null) {
      entity.setUnindexedProperty("keywords", clue.getKeywords());
    }
    if (clue.getImageUrl() != null) {
      entity.setUnindexedProperty("imageUrl", clue.getImageUrl().toString());
    }
    return entity;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((caseDataId == null) ? 0 : caseDataId.hashCode());
    result = prime * result + ((description == null) ? 0 : description.hashCode());
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    result = prime * result + ((imageUrl == null) ? 0 : imageUrl.hashCode());
    result = prime * result + ((keywords == null) ? 0 : keywords.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    Clue other = (Clue) obj;
    if (caseDataId == null) {
      if (other.caseDataId != null) return false;
    } else if (!caseDataId.equals(other.caseDataId)) return false;
    if (description == null) {
      if (other.description != null) return false;
    } else if (!description.equals(other.description)) return false;
    if (id == null) {
      if (other.id != null) return false;
    } else if (!id.equals(other.id)) return false;
    if (imageUrl == null) {
      if (other.imageUrl != null) return false;
    } else if (!imageUrl.equals(other.imageUrl)) return false;
    if (keywords == null) {
      if (other.keywords != null) return false;
    } else if (!keywords.equals(other.keywords)) return false;
    if (name == null) {
      if (other.name != null) return false;
    } else if (!name.equals(other.name)) return false;
    return true;
  }

  @Override
  public String toString() {
    return "Clue [id="
        + id
        + ", caseDataId="
        + caseDataId
        + ", name="
        + name
        + ", description="
        + description
        + ", keywords="
        + keywords
        + ", imageUrl="
        + imageUrl
        + "]";
  }
}
