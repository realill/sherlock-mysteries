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

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.appengine.api.datastore.Entity;
import com.google.mystery.data.DataManager;
import com.google.mystery.data.DataUtil;
import com.sherlockmysteries.pdata.Case.MapType;

/**
 * Represents active case.
 *
 * @author ilyaplatonov
 */
public class Case implements Serializable {
  private static final long serialVersionUID = 8890307337926288359L;

  private static final String ENABLED_PROPERTY = "enabled";
  private static final String CATEGORY_PROPERTY = "category";
  private static final String ALTERNATIVE_NAMES = "alternativeNames";
  private static final String NAME_PROPERTY = "name";
  private static final String CASE_DATA_ID_PROPERTY = "caseDataId";
  private static final String IMAGE_URL = "imageUrl";
  private static final String CASE_SOURCE_URL = "caseSourceUrl";
  private static final String AUTHOR = "author";
  private static final String VOICE_ACTOR = "voiceActor";
  private static final String ILLUSTRATION_ARTIST = "illustrationArtist";
  private static final String MAP_TYPE = "mapType";

  /** id. */
  private final String caseid;
  /** currently used data for this id. */
  private final String caseDataId;
  /** name of the case. */
  private final String name;
  /** alternative names that can be used to find this case. */
  private final List<String> alternativeNames;
  /** Category to characterize this case. */
  private final String category;
  /** indicates if this case is currently shown to users. */
  private final boolean enabled;
  /** thumbnail image. */
  private final URL imageUrl;
  /** points to spreadsheet that is source of data for the case. */
  private final URL caseSourceUrl;
  /** author of the case. */
  private final String author;

  private final String voiceActor;
  private final String illustrationArtist;
  private final MapType mapType;

  public Case(
      String caseid,
      String caseDataId,
      String name,
      List<String> alternativeNames,
      String category,
      boolean enabled,
      URL imageUrl,
      URL caseSourceUrl,
      String author,
      String voiceActor,
      String illustrationArtist,
      MapType mapType) {
    super();
    this.caseid = caseid;
    this.caseDataId = caseDataId;
    this.name = name;
    this.alternativeNames = alternativeNames;
    this.category = category;
    this.enabled = enabled;
    this.imageUrl = imageUrl;
    this.caseSourceUrl = caseSourceUrl;
    this.author = author;
    this.voiceActor = voiceActor;
    this.illustrationArtist = illustrationArtist;
    this.mapType = mapType;
  }

  public static Entity toEntity(Case mcase) {
    Entity entity = new Entity(DataManager.CASE_KIND, mcase.getCaseId());
    entity.setUnindexedProperty(CASE_DATA_ID_PROPERTY, mcase.getCaseDataId());
    entity.setUnindexedProperty(NAME_PROPERTY, mcase.getName());
    DataUtil.setStringListProperty(entity, ALTERNATIVE_NAMES, mcase.getAlternativeNames());
    entity.setUnindexedProperty(CATEGORY_PROPERTY, mcase.getCategory());
    entity.setUnindexedProperty(ENABLED_PROPERTY, mcase.isEnabled());
    if (mcase.getImageUrl() != null) {
      entity.setUnindexedProperty(IMAGE_URL, mcase.getImageUrl().toString());
    }
    if (mcase.getCaseSourceUrl() != null) {
      entity.setUnindexedProperty(CASE_SOURCE_URL, mcase.getCaseSourceUrl().toString());
    }
    entity.setUnindexedProperty(AUTHOR, mcase.getAuthor());
    entity.setUnindexedProperty(VOICE_ACTOR, mcase.getVoiceActor());
    entity.setUnindexedProperty(ILLUSTRATION_ARTIST, mcase.getIllustrationArtist());
    entity.setUnindexedProperty(MAP_TYPE, Long.valueOf(mcase.getMapType().ordinal()));
    return entity;
  }

  public static Case fromEntity(Entity entity) {
    URL imageUrl = null;
    URL caseSourceUrl = null;
    try {
      if (entity.getProperty(IMAGE_URL) != null) {
        imageUrl = new URL(entity.getProperty(IMAGE_URL).toString());
      }
      if (entity.getProperty(CASE_SOURCE_URL) != null) {
        caseSourceUrl = new URL(entity.getProperty(CASE_SOURCE_URL).toString());
      }
    } catch (MalformedURLException e) {
      Logger.getLogger(Case.class.getName()).log(Level.WARNING, "Error parsing url for case", e);
    }
    return new Case(
        entity.getKey().getName(),
        entity.getProperty(CASE_DATA_ID_PROPERTY).toString(),
        entity.getProperty(NAME_PROPERTY).toString(),
        DataUtil.getStringList(entity, ALTERNATIVE_NAMES),
        entity.getProperty(CATEGORY_PROPERTY).toString(),
        (Boolean) entity.getProperty(ENABLED_PROPERTY),
        imageUrl,
        caseSourceUrl,
        entity.getProperty(AUTHOR) == null ? "" : entity.getProperty(AUTHOR).toString(),
        entity.getProperty(VOICE_ACTOR) == null ? "" : entity.getProperty(VOICE_ACTOR).toString(),
        entity.getProperty(ILLUSTRATION_ARTIST) == null
            ? ""
            : entity.getProperty(ILLUSTRATION_ARTIST).toString(),
        entity.getProperty(MAP_TYPE) == null
            ? MapType.BIRDEYE
            : MapType.forNumber(((Long) entity.getProperty(MAP_TYPE)).intValue()));
  }

  public MapType getMapType() {
    return mapType;
  }

  public String getCaseId() {
    return caseid;
  }

  public String getCaseDataId() {
    return caseDataId;
  }

  public String getName() {
    return name;
  }

  public List<String> getAlternativeNames() {
    return alternativeNames;
  }

  public String getCategory() {
    return category;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public String getAuthor() {
    return author;
  }

  public URL getCaseSourceUrl() {
    return caseSourceUrl;
  }

  public String getVoiceActor() {
    return voiceActor;
  }

  public String getIllustrationArtist() {
    return illustrationArtist;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((alternativeNames == null) ? 0 : alternativeNames.hashCode());
    result = prime * result + ((author == null) ? 0 : author.hashCode());
    result = prime * result + ((caseDataId == null) ? 0 : caseDataId.hashCode());
    result = prime * result + ((caseSourceUrl == null) ? 0 : caseSourceUrl.hashCode());
    result = prime * result + ((caseid == null) ? 0 : caseid.hashCode());
    result = prime * result + ((category == null) ? 0 : category.hashCode());
    result = prime * result + (enabled ? 1231 : 1237);
    result = prime * result + ((illustrationArtist == null) ? 0 : illustrationArtist.hashCode());
    result = prime * result + ((imageUrl == null) ? 0 : imageUrl.hashCode());
    result = prime * result + ((mapType == null) ? 0 : mapType.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((voiceActor == null) ? 0 : voiceActor.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    Case other = (Case) obj;
    if (alternativeNames == null) {
      if (other.alternativeNames != null) return false;
    } else if (!alternativeNames.equals(other.alternativeNames)) return false;
    if (author == null) {
      if (other.author != null) return false;
    } else if (!author.equals(other.author)) return false;
    if (caseDataId == null) {
      if (other.caseDataId != null) return false;
    } else if (!caseDataId.equals(other.caseDataId)) return false;
    if (caseSourceUrl == null) {
      if (other.caseSourceUrl != null) return false;
    } else if (!caseSourceUrl.equals(other.caseSourceUrl)) return false;
    if (caseid == null) {
      if (other.caseid != null) return false;
    } else if (!caseid.equals(other.caseid)) return false;
    if (category == null) {
      if (other.category != null) return false;
    } else if (!category.equals(other.category)) return false;
    if (enabled != other.enabled) return false;
    if (illustrationArtist == null) {
      if (other.illustrationArtist != null) return false;
    } else if (!illustrationArtist.equals(other.illustrationArtist)) return false;
    if (imageUrl == null) {
      if (other.imageUrl != null) return false;
    } else if (!imageUrl.equals(other.imageUrl)) return false;
    if (mapType != other.mapType) return false;
    if (name == null) {
      if (other.name != null) return false;
    } else if (!name.equals(other.name)) return false;
    if (voiceActor == null) {
      if (other.voiceActor != null) return false;
    } else if (!voiceActor.equals(other.voiceActor)) return false;
    return true;
  }

  @Override
  public String toString() {
    return "Case [caseid="
        + caseid
        + ", caseDataId="
        + caseDataId
        + ", name="
        + name
        + ", alternativeNames="
        + alternativeNames
        + ", category="
        + category
        + ", enabled="
        + enabled
        + "]";
  }

  public URL getImageUrl() {
    return imageUrl;
  }
}
