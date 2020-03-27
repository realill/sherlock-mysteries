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
  /** id. */
  private final String caseid;
  /** currently used data for this id. */
  private final String caseDataId;
  /** name of the case. */
  private final String name;
  /** alternative names that can be used to find thid case. */
  private final List<String> alternativeNames;
  /** Category to characterize this case. */
  private final String category;
  /** indicates if this case is currently shown to users. */
  private final boolean enabled;
  /** thumbnail image. */
  private final URL imageUrl;

  public Case(String caseid, String caseDataId, String name, List<String> alternativeNames,
      String category, boolean enabled, URL imageUrl) {
    super();
    this.caseid = caseid;
    this.caseDataId = caseDataId;
    this.name = name;
    this.alternativeNames = alternativeNames;
    this.category = category;
    this.enabled = enabled;
    this.imageUrl = imageUrl;
  }

  public static Entity toEntity(Case mcase) {
    Entity entity = new Entity(DataManager.CASE_KIND, mcase.getCaseId());
    entity.setUnindexedProperty(CASE_DATA_ID_PROPERTY, mcase.getCaseDataId());
    entity.setUnindexedProperty(NAME_PROPERTY, mcase.getName());
    DataUtil.setStringListProperty(entity, ALTERNATIVE_NAMES, mcase.getAlternativeNames());
    entity.setUnindexedProperty(CATEGORY_PROPERTY, mcase.getCategory());
    entity.setUnindexedProperty(ENABLED_PROPERTY, mcase.isEnabled());
    if (mcase.getImageUrl() != null) {
      entity.setUnindexedProperty("imageUrl", mcase.getImageUrl().toString());
    }
    return entity;
  }

  public static Case fromEntity(Entity entity) {
    URL url = null;
    if (entity.getProperty("imageUrl") != null) {
      try {
        url = new URL(entity.getProperty("imageUrl").toString());
      } catch (MalformedURLException e) {
        Logger.getLogger(Case.class.getName()).log(Level.WARNING, "Error parsing image for case",
            e);
      }
    }
    return new Case(entity.getKey().getName(), entity.getProperty(CASE_DATA_ID_PROPERTY).toString(),
        entity.getProperty(NAME_PROPERTY).toString(),
        DataUtil.getStringList(entity, ALTERNATIVE_NAMES),
        entity.getProperty(CATEGORY_PROPERTY).toString(),
        (Boolean) entity.getProperty(ENABLED_PROPERTY), url);
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

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((alternativeNames == null) ? 0 : alternativeNames.hashCode());
    result = prime * result + ((caseDataId == null) ? 0 : caseDataId.hashCode());
    result = prime * result + ((caseid == null) ? 0 : caseid.hashCode());
    result = prime * result + ((category == null) ? 0 : category.hashCode());
    result = prime * result + (enabled ? 1231 : 1237);
    result = prime * result + ((name == null) ? 0 : name.hashCode());
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
    Case other = (Case) obj;
    if (alternativeNames == null) {
      if (other.alternativeNames != null)
        return false;
    } else if (!alternativeNames.equals(other.alternativeNames))
      return false;
    if (caseDataId == null) {
      if (other.caseDataId != null)
        return false;
    } else if (!caseDataId.equals(other.caseDataId))
      return false;
    if (caseid == null) {
      if (other.caseid != null)
        return false;
    } else if (!caseid.equals(other.caseid))
      return false;
    if (category == null) {
      if (other.category != null)
        return false;
    } else if (!category.equals(other.category))
      return false;
    if (enabled != other.enabled)
      return false;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "Case [caseid=" + caseid + ", caseDataId=" + caseDataId + ", name=" + name
        + ", alternativeNames=" + alternativeNames + ", category=" + category + ", enabled="
        + enabled + "]";
  }

  public URL getImageUrl() {
    return imageUrl;
  }
}
